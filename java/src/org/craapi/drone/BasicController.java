package org.craapi.drone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.craapi.drone.ar2.Util;

public class BasicController implements ControlAuthority {

	private final Drone drone;
	private final ConcurrentMap<Object, ControlToken> activeTokens = new ConcurrentHashMap<Object, ControlToken>();
	private final ReadWriteLock tokenLock = new ReentrantReadWriteLock();
	private final PriorityBlockingQueue<ControlCommand> commandQueue = new PriorityBlockingQueue<ControlCommand>();
	private ControlToken master;
	private ControlToken activeToken;
	private DroneControlObserver tempObserver;
	private ExecutorService exec;
	private CommandSink sink;

	public BasicController(Drone drone) {
		this.drone = drone;
	}

	@Override
	public synchronized void start() {
		if (exec != null) {
			throw new DroneException("Cannot start because already running!");
		}
		exec = Executors.newCachedThreadPool();
		sink = new CommandSink();
		exec.submit(sink);
	}

	@Override
	public void stop() {
		if (exec == null) {
			throw new DroneException("Cannot stop because already stopped!");
		}
		sink.stop();
		sink = null;
		exec.shutdownNow();
		exec = null;
	}

	@Override
	public E_DroneState getState() {
		return drone.getState();
	}

	@Override
	public void submitRequest(ControlCommand rq) {
		commandQueue.offer(rq);
	}

	@Override
	public void requestControl(Bid... bidsForControl) {

		final List<Bid> bids = new ArrayList<Bid>(Arrays.asList(bidsForControl));
		Collections.sort(bids);

		detachAllBut(bidsForControl);

		tokenLock.writeLock().lock();
		try {
			if (!bids.isEmpty()) {
				boolean first = true;
				for (Bid b : bids) {
					if (activeTokens.get(b.getBidder()) == null) {
						ControlToken t = generateToken(b);
						if (first) {
							if (t.isAbsolute()) {
								master = t;
							}
							first = false;
						}
						activeTokens.put(b.getBidder(), t);
					} else {
						// special case: active token during state transition
						activeToken.absolute = !b.isCooperative();
						activeToken.maxPriority = b.getValue();
						if (first) {
							if (activeToken.isAbsolute()) {
								master = activeToken;
							}
							first = false;
						}
					}
				}
			}

		} finally {
			tokenLock.writeLock().unlock();
		}

		for (Entry<Object, ControlToken> dcoe : activeTokens.entrySet()) {
			try {
				if (dcoe.getKey() instanceof DroneControlObserver) {
					final DroneControlObserver dco = (DroneControlObserver) dcoe
							.getKey();
					final ControlToken token = dcoe.getValue();
					dco.attach(drone, token);
				}
			} catch (Exception e) {
				Util.printError(
						"Encountered exception while attaching observer", e,
						true);
			}
		}
	}

	protected void detachAllBut(Bid... bidsForControl) {

		final List<DroneControlObserver> toDetach = new LinkedList<DroneControlObserver>();
		final List<Object> bidders = new ArrayList<Object>(
				bidsForControl.length);
		for (Bid b : bidsForControl) {
			bidders.add(b.getBidder());
		}

		tokenLock.writeLock().lock();
		try {
			master = null;
			Object activeBidder = null;
			for (Entry<Object, ControlToken> e : activeTokens.entrySet()) {
				boolean continuedBidder = bidders.contains(e.getKey());
				boolean stillActive = e.getValue().equals(activeToken);
				if (continuedBidder && stillActive) {
					// currently active command is still interested in state
					// TODO generate new token with new bid parameters
					activeBidder = e.getKey();
				} else if (continuedBidder) {
					// bidder from last state is interested, but does not have
					// control
					// (token will be replaced with new token)
					e.getValue().cancel();
					if (e.getKey() instanceof DroneControlObserver) {
						tempObserver = (DroneControlObserver) e.getKey();
					}
				} else if (stillActive) {
					// active command is not interested in new state, but must
					// complete task
					// (detach will occur when token is unlocked)
					e.getValue().cancel();
				} else {
					// bidder no longer interested
					e.getValue().cancel();
					if (e.getKey() instanceof DroneControlObserver) {
						toDetach.add((DroneControlObserver) e.getKey());
					}
				}
			}

			activeTokens.clear();
			if (activeBidder != null) {
				activeTokens.put(activeBidder, activeToken);
			}

		} finally {
			tokenLock.writeLock().unlock();
		}

		for (DroneControlObserver dco : toDetach) {
			try {
				dco.detach(drone);
			} catch (Exception e) {
				Util.printError(
						"Encountered exception while detaching observer", e,
						true);
			}
		}
	}

	protected ControlToken generateToken(Bid bid) {
		ControlToken token = new ControlToken(this, bid.getValue(),
				!bid.isCooperative());
		return token;
	}

	protected final boolean lockAuthorization(ControlToken token) {

		if (tokenLock.readLock().tryLock()) {
			try {
				if (activeToken != null) {
					return activeToken.equals(token);
				}

				if (master != null) {
					if (master.equals(token)) {
						activeToken = token;
						return true;
					}
				} else {
					if (activeTokens.containsValue(token)) {
						activeToken = token;
						return true;
					}
				}
			} finally {
				tokenLock.readLock().unlock();
			}
		}

		return false;
	}

	protected final boolean unlockAuthorization(ControlToken token) {
		DroneControlObserver dco = null;
		boolean result = false;
		tokenLock.readLock().lock();
		try {
			if (activeToken != null && activeToken.equals(token)) {
				if (activeToken.isCancelled() && tempObserver != null) {
					dco = tempObserver;
				}
				activeToken = null;
				result = true;
			}
		} finally {
			tokenLock.readLock().unlock();
		}

		if (dco != null) {
			dco.detach(drone);
		}

		return result;
	}

	@Override
	public boolean connect(ControlToken token) {
		if (lockAuthorization(token)) {
			try {
				return drone.connect();
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean disconnect(ControlToken token, boolean force) {
		if (lockAuthorization(token)) {
			try {
				return drone.disconnect(force);
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean takeoff(ControlToken token) {
		if (lockAuthorization(token)) {
			try {
				return drone.takeoff();
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean land(ControlToken token) {
		if (lockAuthorization(token)) {
			try {
				return drone.land();
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean flatTrim(ControlToken token) {
		if (lockAuthorization(token)) {
			try {
				return drone.flatTrim();
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean spin(ControlToken token, float yawSpeed) {
		if (lockAuthorization(token)) {
			try {
				return drone.spin(yawSpeed);
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public boolean hover(ControlToken token) {
		if (lockAuthorization(token)) {
			try {
				return drone.hover();
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}
	
	@Override
	public boolean move(ControlToken token, boolean combineYaw, float yaw, float pitch, float roll,
			float vertical) {
		if (lockAuthorization(token)) {
			try {
				return drone.move(combineYaw, yaw, pitch, roll, vertical);
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return false;
		}
	}

	@Override
	public E_DroneState transition(ControlToken token, E_DroneState expected,
			E_DroneState desired) {
		if (lockAuthorization(token)) {
			try {
				return drone.transition(expected, desired);
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return drone.getState();
		}
	}

	@Override
	public E_DroneState transition(ControlToken token, E_DroneState expected,
			E_DroneState transition, E_DroneState success,
			E_DroneState failure, Callable<Boolean> task) {

		if (lockAuthorization(token)) {
			try {
				return drone.transition(expected, transition, success, failure,
						task);
			} finally {
				unlockAuthorization(token);
			}
		} else {
			token.cancel();
			return drone.getState();
		}
	}

	protected class CommandSink implements Runnable {

		protected boolean running = false;

		public void stop() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			while (running) {
				try {

					ControlCommand cmd = commandQueue.poll(100,
							TimeUnit.MILLISECONDS);
					if (cmd != null) {
						if (Util.DEBUG_ENABLED) {
							Util.printDebug("Executing command: " + cmd);
						}
						cmd.executeAsync(BasicController.this, exec);
					}
				} catch (InterruptedException e) {
					// ignore
				} catch (Exception e) {
					Util.printError(
							"Exception encountered while processing command",
							e, true);
				}
			}
		}
	}
}
