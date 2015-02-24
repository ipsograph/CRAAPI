package org.craapi.drone.sim;

import java.util.concurrent.atomic.AtomicBoolean;

import org.craapi.drone.ControlConnection;

public class OfflineSimConnection implements ControlConnection {

	protected final AtomicBoolean state = new AtomicBoolean(false);
	
	@Override
	public boolean establish() {
		System.out.println("[SIM] Establish");
		return state.compareAndSet(false, true);
	}

	@Override
	public boolean close() {
		System.out.println("[SIM] Close");
		return state.compareAndSet(true, false);
	}

	@Override
	public boolean isConnected() {
		return state.get();
	}
	
	@Override
	public boolean sendEmergencyCommand(boolean emergency) {
		
		System.out.println("[SIM] Toggle emergency...");
		
		return true;
	}
	
	@Override
	public boolean sendKinematics(boolean hover, boolean combinedYaw,
			float yawSpeed, float pitchSpeed, float rollSpeed,
			float verticalSpeed) {

		System.out.println("[SIM] Kinematics: hover=" + hover + ", yaw="
				+ yawSpeed + ", pitch=" + pitchSpeed + ", roll=" + rollSpeed
				+ ", vspeed=" + verticalSpeed);

		return true;
	}
	
	@Override
	public boolean sendLandCommand() {
		System.out.println("[SIM] Landing...");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		System.out.println("[SIM] ...landed");
		
		return true;
	}
	
	@Override
	public boolean sendTakeoffCommand() {
		System.out.println("[SIM] Takeoff...");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		System.out.println("[SIM] ...took off");
		
		return true;
	}
	
	@Override
	public boolean sendFlatTrimCommand() {
		System.out.println("[SIM] Flat trim");
		return true;
	}
}
