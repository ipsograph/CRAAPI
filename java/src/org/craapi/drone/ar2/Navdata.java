package org.craapi.drone.ar2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 32-bit constant header
 * 32-bit state
 * 32-bit sequence number
 * 32-bit vision state
 * 
 * 0 or more options
 * 
 * 64-bit checksum
 */
public class Navdata {

	protected static final int HEADER = 0x55667788;
	
	protected static final int MASK_FLY = 1 << 0;			// 0=landed, 1=flying
	protected static final int MASK_VIDEO = 1 << 1;			// 0=disable, 1=enable
	protected static final int MASK_VISION = 1 << 2;		// 0=disable, 1=enable
	protected static final int MASK_CONTROL = 1 << 3;		// 0=euler angle, 1=angular speed
	protected static final int MASK_ALT_CONTROL = 1 << 4;	// 0=inactive, 1=active
	protected static final int MASK_USER_FEEDBACK = 1 << 5;	// start button
	protected static final int MASK_CTRL_CMD_ACK = 1 << 6;	// 0=none, 1=one
	protected static final int MASK_TRIM_CMD_ACK = 1 << 7;	// 0=none, 1=one
	protected static final int MASK_TRIM_RUNNING = 1 << 8;	// 0=none, 1=running
	protected static final int MASK_TRIM_RESULT = 1 << 9;	// 0=failed, 1=succeeded
	protected static final int MASK_NAVDATA_DEMO = 1 << 10;	// 0=all, 1=demo
	protected static final int MASK_BOOTSTRAP = 1 << 11;	// 0=all or demo, 1=no options sent
	protected static final int MASK_MOTOR_STATUS = 1 << 12;	// 0=ok, 1=down
	protected static final int MASK_UNKNOWN = 1 << 13;		// ?
	protected static final int MASK_GYRO_ERR = 1 << 14;		// 0=ok, 1=issue
	protected static final int MASK_VBAT_LOW = 1 << 15;		// 0=ok, 1=too low
	protected static final int MASK_VBAT_HIGH = 1 << 16;	// 0=ok, 1=too high
	protected static final int MASK_TIMER = 1 << 17;		// 0=no, 1=elapsed
	protected static final int MASK_POWER = 1 << 18;		// 0=ok, 1=too low to fly
	protected static final int MASK_ANGLES = 1 << 19;		// 0=ok, 1=out of range
	protected static final int MASK_WIND = 1 << 20;			// 0=ok, 1=too windy to fly
	protected static final int MASK_ULTRASONIC = 1 << 21;	// 0=ok, 1=deaf
	protected static final int MASK_CUTOUT = 1 << 22;		// 0=ok, 1=detected
	protected static final int MASK_PIC_VERSION = 1 << 23;	// 0=ok, 1=bad
	protected static final int MASK_AT_CODEC_THREAD = 1 << 24;	// 0=off, 1=on
	protected static final int MASK_NAVDATA_THREAD = 1 << 25;	// 0=off, 1=on
	protected static final int MASK_VIDEO_THREAD = 1 << 26;		// 0=off, 1=on
	protected static final int MASK_AQUIRE_THREAD = 1 << 27;	// 0=off, 1=on
	protected static final int MASK_CTRL_WATCHDOG = 1 << 28;	// 0=ok, 1=delay > 5ms
	protected static final int MASK_ADC_WATCHDOG = 1 << 29;		// 0=ok, 1=delay > 5ms
	protected static final int MASK_COM_WATCHDOG = 1 << 30;		// 0=ok, 1=problem (or no client)
	protected static final int MASK_EMERGENCY = 1 << 31;		// 0=ok, 1=emergency
	
	protected int header;
	protected int state;
	protected long seqNum;
	protected boolean visionFlag;
	protected List<NavOption> options;
	
	public int getState() {
		return state;
	}

	public long getSeqNum() {
		return seqNum;
	}

	public boolean isVisionFlag() {
		return visionFlag;
	}

	public List<NavOption> getOptions() {
		return options;
	}

	public boolean parse(byte[] data, int offset, int length) {
		
		final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		if (!parsePayload(bb)) {
			return false;
		}
		
		if (!parseOptions(bb)) {
			return false;
		}
		
		return true;
	}
	
	protected boolean parsePayload(ByteBuffer bb) {
		try {
			header = bb.getInt();
			state = bb.getInt();
			seqNum = Util.getUInteger(bb);
			int temp = bb.getInt();
			visionFlag = temp > 0;
			return true;
		} catch (Exception e) {
			Util.printError("Exception encountered"
					+ " while parsing NAVDATA", e, true);
			return false;
		}
	}
	
	protected boolean parseOptions(ByteBuffer bb) {

		final List<NavOption> opts = new ArrayList<NavOption>();
		while (bb.remaining() > 0) {
			final NavOption o = parseOption(bb);
			if (o != null) {
				opts.add(o);
			}
		}
		this.options = opts;
		return true;
	}
	
	protected NavOption parseOption(ByteBuffer bb) {
		
		try {
			final NavOption opt = new NavOption();
			int tag = Util.getUShort(bb);
			int size = Util.getUShort(bb);
			if (size <= 4) {
				// no data
				return null;
			}
			size = Math.min(bb.remaining(), size - 4);
			byte[] data = new byte[size];
			bb.get(data, 0, data.length);
			opt.size = size;
			opt.setTagAndData(tag, data);
			return opt;
		} catch (Exception e) {
			Util.printError("Exception encountered"
					+ " while parsing NAVDATA option", e, true);
			return null;
		}
	}
	
	public boolean isFlying() {
		return (state & MASK_FLY) > 0;
	}
	
	public boolean isGrounded() {
		return (state & MASK_FLY) == 0;
	}
	
	public boolean isControlCommandAck() {
		return (state & MASK_CTRL_CMD_ACK) > 0;
	}
	
	public boolean isNavdataDemo() {
		return (state & MASK_NAVDATA_DEMO) > 0;
	}
	
	public boolean isBootstrap() {
		return (state & MASK_BOOTSTRAP) > 0;
	}
	
	public boolean isPowered() {
		return (state & MASK_POWER) == 0;
	}
	
	public boolean isAtmosphereCalm() {
		return (state & MASK_WIND) == 0;
	}
	
	public boolean isUltrasonicFunctional() {
		return (state & MASK_ULTRASONIC) == 0;
	}
	
	public boolean isAngleInRange() {
		return (state & MASK_ANGLES) == 0;
	}
	
	public boolean isEmergency() {
		return (state & MASK_EMERGENCY) > 0;
	}
	
	public boolean isComWatchdog() {
		return (state & MASK_COM_WATCHDOG) > 0;
	}
	
	public boolean isAdcWatchdog() {
		return (state & MASK_ADC_WATCHDOG) > 0;
	}
	
	public boolean isCtrlWatchdog() {
		return (state & MASK_CTRL_WATCHDOG) > 0;
	}
	
	public boolean isLandingAdvised() {
		return isFlying()
				&& (!isPowered() || !isAngleInRange() || !isAtmosphereCalm()
						|| !isEmergency() || !isUltrasonicFunctional());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" CORE:").append("\n");
		sb.append("  header=").append(Integer.toHexString(header)).append("\n");
		sb.append("  state=").append(Integer.toHexString(state)).append("\n");
		sb.append("   flying=").append(isFlying()).append("\n");
		sb.append("   bootstrap=").append(isBootstrap()).append("\n");
		sb.append("   ctrl ack=").append(isControlCommandAck()).append("\n");
		sb.append("   nav demo=").append(isNavdataDemo()).append("\n");
		sb.append("   ctrl delay=").append(isCtrlWatchdog()).append("\n");
		sb.append("   com issue=").append(isComWatchdog()).append("\n");
		sb.append("   rq land=").append(isLandingAdvised()).append("\n");
		sb.append("  seqNum=").append(seqNum).append("\n");
		sb.append("  vision=").append(visionFlag).append("\n");
		if (options != null && !options.isEmpty()) {
			sb.append(" OPTIONS:").append("\n");
			int i=0;
			for (NavOption o : options) {
				if (o.getParsedContent() instanceof NavdemoContent) {
					if (o.getTag() != null) {
						sb.append("  tag[").append(i).append("]=").append(o.tag).append("\n");
						sb.append("  size[").append(i).append("]=").append(o.size).append("\n");
						if (o.parsedContent != null) {
							sb.append("  CONTENT[").append(i).append("]:\n");
							sb.append(o.parsedContent);
						}
					}
				}
				i++;
			}
		}
		return sb.toString();
	}
}
