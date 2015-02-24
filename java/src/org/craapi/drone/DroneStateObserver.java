package org.craapi.drone;

public interface DroneStateObserver {
	
	public void stateChanged(Drone drone, E_DroneState oldState, E_DroneState newState);
}
