package org.craapi.drone;

public interface DroneStateController {

	public void addStateObserver(Drone drone, DroneObserver observer,
			E_DroneState... states);
	
	public void removeStateObserver(Drone drone, DroneObserver observer);
	
	public void removeAllObservers(Drone drone);
	
	public void start();
	
	public void stop();
}
