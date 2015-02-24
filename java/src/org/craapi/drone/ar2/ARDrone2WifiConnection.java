package org.craapi.drone.ar2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.craapi.drone.ControlConnection;
import org.craapi.drone.ar2.NavOption.E_OptionTag;
import org.craapi.drone.ar2.NavdemoContent.E_MajorControlState;

public class ARDrone2WifiConnection implements ControlConnection {

	private static final byte[] DRONE_ADDRESS = new byte[] { (byte) 192,
			(byte) 168, 1, 1 };
	
	private static final int COMMAND_PORT = 5556;
	private static final int NAVDATA_PORT = 5554;
	
	private static final int MASK_HOVER = (1 << 0);
	private static final int MASK_COMBINED_YAW = (1 << 1);
	private static final int MASK_TAKEOFF = (1 << 9);
	private static final int MASK_EMERGENCY = (1 << 8);
	private static final int REF_BASE = (1 << 28) | (1 << 24) | (1 << 22) | (1 << 20) | (1 << 18);
	
	private static final String DUMMY_SN = "%s";
	private static final String CONFIG_DEMO = "AT*CONFIG=%s,\"general:navdata_demo\",\"FALSE\"\r";
	private static final String ACK = "AT*CTRL=%s\r";
	private static final String COMWDG = "AT*COMWDG=%s\r";
	private static final String REF = "AT*REF=%s,%d\r";
	private static final String FTRIM = "AT*FTRIM=%s\r";
	private static final String PCMD = "AT*PCMD=%s,%d,%s,%s,%s,%s\r"; // flags,
																		// roll,
																		// pitch,
																		// alt,
																		// yaw

	protected InetAddress droneAddress;
	protected DatagramSocket navdataSocket;
	protected DatagramSocket commandSocket;
	protected final AtomicLong sn = new AtomicLong(0);
	protected final byte[] octets;
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	protected final LinkedBlockingQueue<byte[]> navdataQueue = new LinkedBlockingQueue<byte[]>();
	protected final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<String>();
	protected ExecutorService exec;

	protected Navdata lastNavdata;
	protected final ConcurrentMap<E_OptionTag, NavOption> optionsMap = new ConcurrentHashMap<E_OptionTag, NavOption>();

	public ARDrone2WifiConnection() {
		this(DRONE_ADDRESS);
	}

	public ARDrone2WifiConnection(byte[] address) {
		this.octets = address;
	}

	@Override
	public synchronized boolean establish() {

		if (isConnected()) {
			return false;
		}

		Util.printInfo("Opening connection...");

		try {
			droneAddress = InetAddress.getByAddress(octets);
			Util.printDebug("Address: " + droneAddress);
		} catch (Exception e) {
			Util.printError("Failed to resolve drone address", e, true);
			return false;
		}

		try {
			navdataSocket = new DatagramSocket(NAVDATA_PORT);
		} catch (Exception e) {
			Util.printError("Failed to open NAVDATA socket", e, true);
			clean();
			return false;
		}

		try {
			commandSocket = new DatagramSocket(COMMAND_PORT);
		} catch (SocketException e) {
			Util.printError("Failed to open COMMAND socket", e, true);
			clean();
			return false;
		}

		try {
			handshake();
		} catch (IOException e) {
			Util.printError("Failed to complete handshake with drone", e, true);
			clean();
			return false;
		}

		exec = Executors.newCachedThreadPool();
		exec.submit(new NavdataHandler());
		exec.submit(new NavdataMonitor());
		exec.submit(new CommandHandler());
		// exec.submit(new Watchdog(false));

		connected.set(true);

		Util.printInfo("...connection successful");

		return true;
	}

	protected void clean() {
		if (commandSocket != null) {
			commandSocket.close();
			commandSocket = null;
		}
		if (navdataSocket != null) {
			navdataSocket.close();
			navdataSocket = null;
		}
		droneAddress = null;
	}

	@Override
	public synchronized boolean close() {

		Util.printInfo("Closing connection...");

		if (!isConnected()) {
			return false;
		}

		connected.set(false);

		exec.shutdownNow();
		exec = null;

		clean();

		Util.printInfo("...closed connection successfully");

		return true;
	}

	@Override
	public synchronized boolean isConnected() {
		return connected.get();
	}

	@Override
	public boolean sendTakeoffCommand() {
		return sendRefCommand(true, false);
	}

	@Override
	public boolean sendLandCommand() {
		return sendRefCommand(false, false);
	}

	@Override
	public boolean sendFlatTrimCommand() {

		try {
			commandQueue.offer(FTRIM);
			return true;
		} catch (Exception e) {
			Util.printError("Exception occurred while"
					+ " sending flat trim command", e, false);
			return false;
		}
	}

	@Override
	public boolean sendKinematics(boolean hover, boolean combinedYaw, float yawSpeed,
			float pitchSpeed, float rollSpeed, float verticalSpeed) {

		int flags = 0;
		if (!hover) {
			flags = flags | MASK_HOVER;
		}
		if (combinedYaw) {
			flags = flags | MASK_COMBINED_YAW;
		}

		// flags, roll, pitch, alt, yaw
		String pcmd = String.format(PCMD, DUMMY_SN, flags,
				Util.encodeParameter(rollSpeed),
				Util.encodeParameter(pitchSpeed),
				Util.encodeParameter(verticalSpeed),
				Util.encodeParameter(yawSpeed));
		commandQueue.offer(pcmd);

		return true; // non-blocking command
	}

	@Override
	public boolean sendEmergencyCommand(boolean emergency) {
		return sendRefCommand(false, emergency);
	}

	protected boolean sendRefCommand(boolean takeoff, boolean emergency) {

		int ref = REF_BASE;
		if (takeoff) {
			ref = ref | MASK_TAKEOFF;
		}
		if (emergency) {
			ref = ref | MASK_EMERGENCY;
		}

		try {
			final String payload = String.format(REF, DUMMY_SN, ref);
			if (emergency) {
				commandQueue.offer(payload);
				return true;
			}

			final WaitForState task = new WaitForState();
			task.setMaxWaitMs(10000);
			if (takeoff) {
				task.setStateMask(Navdata.MASK_FLY);
				task.setTransitionState(E_MajorControlState.CTRL_TRANS_TAKEOFF);
				task.setEndState(E_MajorControlState.CTRL_HOVERING);
			} else {
				task.setInversionMask(Navdata.MASK_FLY);
				task.setTransitionState(E_MajorControlState.CTRL_TRANS_LANDING);
				task.setEndState(E_MajorControlState.CTRL_LANDED);
			}
			final Future<Boolean> f = exec.submit(task);
			long start = System.currentTimeMillis();
			int nSent = 0;
			while (!f.isDone()) {
				if (nSent < 3) {
					commandQueue.offer(payload);
					nSent++;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			if (Util.DEBUG_ENABLED) {
				long end = System.currentTimeMillis();
				long dt = end - start;
				Util.printDebug("Takeoff/landing completed with result "
						+ f.get() + " after " + dt + "ms");
			}
			return Boolean.TRUE.equals(f.get());
		} catch (Exception e) {
			Util.printError("Exception occurred while sending REF (" + takeoff
					+ ") command : ", e, false);
			return false;
		}
	}

	protected void handshake() throws IOException {

		// 1. send message to navdata port
		final DatagramPacket hello = new DatagramPacket(
				new byte[] { 1, 0, 0, 0 }, 4, droneAddress, NAVDATA_PORT);
		navdataSocket.send(hello);

		// 2. receive response (with status)
		final DatagramPacket rec = new DatagramPacket(new byte[4096], 4096);
		navdataSocket.receive(rec);

		// 3. Verify status
		final Navdata ndstat = new Navdata();
		if (ndstat.parse(rec.getData(), rec.getOffset(), rec.getLength())) {
			Util.printDebug("Received status:\n" + ndstat.toString());
		} else {
			Util.printWarn("Received status, but parsing failed");
		}

		// 4. Send bootstrap config to command port
		byte[] bsbuff = String.format(CONFIG_DEMO, getSeqNumParam()).getBytes(
				"ASCII");
		DatagramPacket bootstrap = new DatagramPacket(bsbuff, bsbuff.length,
				droneAddress, COMMAND_PORT);
		commandSocket.send(bootstrap);

		// 5. Receive AR_DRONE_COMMAND_MASK
		navdataSocket.receive(rec);

		// 6. Verify status
		final Navdata ndack = new Navdata();
		if (ndack.parse(rec.getData(), rec.getOffset(), rec.getLength())) {
			Util.printDebug("Received control ACK:\n" + ndack.toString());
		} else {
			Util.printWarn("Received control ACK, but parsing failed");
		}

		// 7. Acknowledge
		byte[] ackbuff = String.format(ACK, getSeqNumParam()).getBytes("ASCII");
		DatagramPacket ack = new DatagramPacket(ackbuff, ackbuff.length,
				droneAddress, COMMAND_PORT);
		commandSocket.send(ack);
	}

	protected String getSeqNumParam() {
		return Long.toString(sn.incrementAndGet());
	}

	protected class NavdataMonitor implements Runnable {

		@Override
		public void run() {

			final byte[] dataBuffer = new byte[2048];
			final DatagramPacket packet = new DatagramPacket(dataBuffer,
					dataBuffer.length);

			while (isConnected()) {

				try {

					navdataSocket.receive(packet);
					int length = packet.getLength();
					int offset = packet.getOffset();
					if (length > 0) {
						byte[] copy = new byte[length];
						System.arraycopy(dataBuffer, offset, copy, 0, length);
						navdataQueue.offer(copy);
						if (navdataQueue.size() > 10) {
							Util.printWarn("NAVDATA Queue is backing up ("
									+ navdataQueue.size() + ")");
						}
					}
				} catch (Exception e) {
					if (isConnected()) {
						Util.printError("Exception while receiving NAVDATA", e,
								true);
					}
				}
			}
		}
	}

	protected class NavdataHandler implements Runnable {

		@Override
		public void run() {

			final List<byte[]> dump = new ArrayList<byte[]>(100);
			while (isConnected()) {
				try {
					dump.clear();
					int nRead = navdataQueue.drainTo(dump, 100);
					if (nRead > 0) {
						if (nRead > 1) {
							Util.printWarn("Discarding " + (nRead - 1)
									+ " NAVADATA objects");
						}
						byte[] packet = dump.get(dump.size() - 1);
						final Navdata nav = new Navdata();
						if (nav.parse(packet, 0, packet.length)) {
							if (nav.getOptions() != null) {
								for (NavOption o : nav.getOptions()) {
									if (o.getTag() != null) {
										optionsMap.put(o.getTag(), o);

										// debug logging to track control state
										if (Util.DEBUG_ENABLED) {
											if (o.getTag() == E_OptionTag.NAVDATA_DEMO) {
												if (o.getParsedContent() instanceof NavdemoContent) {
													NavdemoContent nc = (NavdemoContent) o
															.getParsedContent();
													Util.printDebug("CTRL_STATE: "
															+ nc.majorState
															+ ":"
															+ nc.minorState);
												}
											}
										}
									}
								}
							}
							lastNavdata = nav;
							if (Util.DEBUG_ENABLED) {
								Util.printDebug("Received NAVDATA:\n" + nav.toString());
							}
						}
					}
				} catch (Exception e) {
					Util.printError("Exception while monitoring NAVDATA queue",
							e, true);
				}
			}
		}
	}

	protected class CommandHandler implements Runnable {

		@Override
		public void run() {

			List<String> dump = new ArrayList<String>(100);
			long lastMsgTs = 0;
			while (isConnected()) {
				try {
					dump.clear();
					int nRead = commandQueue.drainTo(dump, 100);
					if (nRead > 0) {
						if (nRead != 1) {
							Util.printWarn("Sending multiple commands ("
									+ nRead + ")");
						}

						for (String msg : dump) {
							final String fmsg = String.format(msg,
									getSeqNumParam());
							final byte[] data = fmsg.getBytes("ASCII");
							if (data.length > 1024) {
								Util.printWarn("Command discarded -"
										+ " Buffer is not long enough (command length is "
										+ data.length + ")");
								continue;
							}

							// TODO this is silly/inefficient
							final byte[] buff = new byte[1024];
							final DatagramPacket packet = new DatagramPacket(
									buff, buff.length, droneAddress,
									COMMAND_PORT);
							System.arraycopy(data, 0, buff, 0, data.length);
							packet.setLength(data.length);
							commandSocket.send(packet);
							Util.printDebug("SENT COMMAND: " + fmsg);
							lastMsgTs = System.currentTimeMillis();
						}
					} else {
						// watchdog/keepalive
						long ts = System.currentTimeMillis();
						if (ts - lastMsgTs > 40) {
							final byte[] data = String.format(COMWDG,
									getSeqNumParam()).getBytes("ASCII");
							final byte[] buff = new byte[1024];
							final DatagramPacket packet = new DatagramPacket(
									buff, buff.length, droneAddress,
									COMMAND_PORT);
							System.arraycopy(data, 0, buff, 0, data.length);
							packet.setLength(data.length);
							commandSocket.send(packet);
							lastMsgTs = System.currentTimeMillis();
						}
					}
				} catch (Exception e) {
					Util.printError("Exception while draining command queue",
							e, true);
				}
			}
		}
	}

	@Deprecated
	protected class Watchdog implements Runnable {

		protected final boolean sendAlways;

		public Watchdog(boolean sendAlways) {
			this.sendAlways = sendAlways;
		}

		@Override
		public void run() {

			while (isConnected()) {

				try {
					if (sendAlways
							|| (lastNavdata != null && lastNavdata
									.isComWatchdog())) {
						commandQueue.offer(COMWDG);
					}
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// ignore
				} catch (Exception e) {
					Util.printError("Exception in watchdog thread", e, true);
				}
			}
		}
	}

	protected class WaitForState implements Callable<Boolean> {

		protected int stateMask = 0;
		protected int inversionMask = 0;
		protected boolean matchAll = false;
		protected long maxWaitMs = -1;
		protected E_MajorControlState startState;
		protected E_MajorControlState transitionState;
		protected E_MajorControlState endState;
		protected Enum<?> endMinorState;

		public int getStateMask() {
			return stateMask;
		}

		public void setStateMask(int stateMask) {
			this.stateMask = stateMask;
		}

		public int getInversionMask() {
			return inversionMask;
		}

		public void setInversionMask(int inversionMask) {
			this.inversionMask = inversionMask;
		}

		public boolean isMatchAll() {
			return matchAll;
		}

		public void setMatchAll(boolean matchAll) {
			this.matchAll = matchAll;
		}

		public long getMaxWaitMs() {
			return maxWaitMs;
		}

		public void setMaxWaitMs(long maxWaitMs) {
			this.maxWaitMs = maxWaitMs;
		}

		public E_MajorControlState getStartState() {
			return startState;
		}

		public void setStartState(E_MajorControlState startState) {
			this.startState = startState;
		}

		public E_MajorControlState getTransitionState() {
			return transitionState;
		}

		public void setTransitionState(E_MajorControlState transitionState) {
			this.transitionState = transitionState;
		}

		public E_MajorControlState getEndState() {
			return endState;
		}

		public void setEndState(E_MajorControlState endState) {
			setEndState(endState, null);
		}

		public void setEndState(E_MajorControlState endState,
				Enum<?> endMinorState) {
			this.endState = endState;
			this.endMinorState = endMinorState;
		}

		@Override
		public Boolean call() throws Exception {

			final long start = System.currentTimeMillis();
			boolean startStatePass = (startState == null);
			boolean transitionStatePass = (transitionState == null);
			boolean endStatePass = (endState == null);
			while (maxWaitMs < 0
					|| (System.currentTimeMillis() - start) < maxWaitMs) {

				try {
					if (lastNavdata != null) {
						final int state = lastNavdata.state;
						final boolean onStatePass;
						if (stateMask != 0) {
							final int onFlags = state & stateMask;
							onStatePass = ((!matchAll && (onFlags > 0)) || (matchAll && (onFlags == stateMask)));
						} else {
							onStatePass = true;
						}

						final boolean offStatePass;
						if (onStatePass && inversionMask != 0) {
							final int mask = state | inversionMask;
							final int offFlags = state ^ mask;
							offStatePass = ((!matchAll && (offFlags > 0)) || (matchAll && (offFlags == inversionMask)));
						} else {
							offStatePass = true;
						}

						final NavOption demo = optionsMap
								.get(E_OptionTag.NAVDATA_DEMO);
						if (demo != null
								&& demo.getParsedContent() instanceof NavdemoContent) {
							final NavdemoContent c = (NavdemoContent) demo
									.getParsedContent();
							if (!startStatePass) {
								if (startState.equals(c.majorState)) {
									startStatePass = true;
								} else {
									// start state MUST be met on first pass
									Util.printWarn("Start state is not as expected");
									return false;
								}
							} else if (!transitionStatePass) {
								transitionStatePass = transitionState
										.equals(c.majorState);
							} else if (!endStatePass) {
								if (endMinorState == null) {
									endStatePass = endState
											.equals(c.majorState);
								} else {
									endStatePass = endState
											.equals(c.majorState)
											&& endMinorState
													.equals(c.minorState);
								}
							}
						}

						if (onStatePass && offStatePass && startStatePass
								&& transitionStatePass && endStatePass) {
							Util.printDebug("WaitForState PASSED");
							return true;
						}
					}

					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// ignore
					}
				} catch (Exception e) {
					Util.printError(
							"Exception occurred while waiting for state", e,
							true);
					return false;
				}
			}

			Util.printDebug("WaitForState timed out");
			return false;
		}
	}
}
