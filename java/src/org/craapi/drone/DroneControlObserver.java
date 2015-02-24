package org.craapi.drone;

public interface DroneControlObserver {
	
	public boolean attach(Drone drone, ControlToken token);
	
	public boolean detach(Drone drone);
}
