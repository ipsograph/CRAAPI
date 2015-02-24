package org.craapi.drone;

import edu.brown.cs.common.resource.Connection;

public interface ControlConnection extends Connection {
	
	public boolean sendTakeoffCommand();
	
	public boolean sendLandCommand();
	
	public boolean sendFlatTrimCommand();
	
	public boolean sendEmergencyCommand(boolean emergency);
	
	public boolean sendKinematics(boolean hover, boolean combinedYaw, float yawSpeed,
			float pitchSpeed, float rollSpeed, float verticalSpeed);
}
