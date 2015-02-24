package org.craapi.drone.ar2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 32-bit ctrl_state
 * 32-bit vbat_flying_percent
 * float theta
 * float phi
 * float psi
 * 32-bit altitude
 * float vx
 * float vy
 * float vz
 *
 */
public class NavdemoContent implements NavOptionContent {

	protected E_MajorControlState majorState;
	protected Enum<?> minorState;
	protected int battery; // 0 to 100 percent
	protected float theta;
	protected float phi;
	protected float psi;
	protected int altitude;
	protected float[] velocity;
	
	@Override
	public boolean process(byte[] data) {
		
		if (data.length != 144) {
			Util.printWarn("NAVDEMO content is of unexpected length: "
					+ data.length);
			return false;
		}
		
		try {
			final ByteBuffer bb = ByteBuffer.wrap(data).order(
					ByteOrder.LITTLE_ENDIAN);
			int minor = Util.getUShort(bb);
			int major = Util.getUShort(bb);
			try {
				majorState = E_MajorControlState.values()[major];
				try {
					minorState = majorState.lookupMinorState(minor);
				} catch (Exception e) {
					minorState = null;
				}
			} catch (Exception e) {
				Util.printWarn("Unrecognized major state '" + major
						+ "'");
				majorState = null;
			}
			
			battery = bb.getInt();
			theta = bb.getFloat();
			phi = bb.getFloat();
			psi = bb.getFloat();
			altitude = bb.getInt();
			velocity = Util.readFloatArray(bb, 3);
			return true;
		} catch (Exception e) {
			Util.printError("Failed to parse NAVDEMO content", e, true);
			return false;
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("   major=").append(majorState).append("\n");
		sb.append("   minor=").append(minorState).append("\n");
		sb.append("   battery=").append(battery).append("%\n");
		sb.append("   theta=").append(theta).append("\n");
		sb.append("   phi=").append(phi).append("\n");
		sb.append("   psi=").append(psi).append("\n");
		sb.append("   altitude=").append(altitude).append("\n");
		sb.append("   velocity=").append(Arrays.toString(velocity)).append("\n");
		return sb.toString();
	}
	
	public static enum E_MajorControlState {
		CTRL_DEFAULT,
		CTRL_INIT,
		CTRL_LANDED,
		CTRL_FLYING(E_FlyingState.class),
		CTRL_HOVERING(E_HoverState.class),
		CTRL_TEST,
		CTRL_UNKNOWN,
		CTRL_TRANS_TAKEOFF(E_TakeoffState.class),
		CTRL_TRANS_GOTOFIX(E_GotoState.class),
		CTRL_TRANS_LANDING(E_LandingState.class);
		
		private final Class<? extends Enum<?>> minorStateEnum;
		
		private E_MajorControlState() {
			this(null);
		}
		
		private E_MajorControlState(Class<? extends Enum<?>> minorStateEnum) {
			this.minorStateEnum = minorStateEnum;
		}
		
		public Enum<?> lookupMinorState(int value) {
			if (minorStateEnum == null) {
				return null;
			}

			try {
				return (minorStateEnum.getEnumConstants())[value];
			} catch (Exception e) {
				return null;
			}
		}
	}
	
	public static enum E_FlyingState {
		FLYING_OK,
		FLYING_LOST_ALT,
		FLYING_LOST_ALT_GO_DOWN,
		FLYING_ALT_OUT_ZONE,
		FLYING_COMBINED_YAW, 
		FLYING_BRAKE,
		FLYING_NO_VISION
	}
	
	public static enum E_HoverState {
		HOVERING_OK,
		HOVERING_YAW,
		HOVERING_YAW_LOST_ALT,
		HOVERING_YAW_LOST_ALT_GO_DOWN,
		HOVERING_ALT_OUT_ZONE,
		HOVERING_YAW_ALT_OUT_ZONE,
		HOVERING_LOST_ALT,
		HOVERING_LOST_ALT_GO_DOWN,
		HOVERING_LOST_COM,
		LOST_COM_LOST_ALT,
		LOST_COM_LOST_ALT_TOO_LONG,
		LOST_COM_ALT_OK,
		HOVERING_MAGNETO_CALIB,
		HOVERING_DEMO
	}
	
	public static enum E_TakeoffState {
		TAKEOFF_GROUND,
		TAKEOFF_AUTO
	}
	
	public static enum E_GotoState {
		GOTOFIX_OK,
		GOTOFIX_LOST_ALT,
		GOTOFIX_YAW
	}
	
	public static enum E_LandingState {
		LANDING_CLOSED_LOOP,
		LANDING_OPEN_LOOP,
		LANDING_OPEN_LOOP_FAST
	}
	
	public static enum E_LoopingState {
		LOOPING_IMPULSION,
		LOOPING_OPEN_LOOP_CTRL,
		LOOPING_PLANIF_CTRL
	}
}
