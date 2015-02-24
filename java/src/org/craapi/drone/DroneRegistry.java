package org.craapi.drone;

public interface DroneRegistry {

	public void register(Drone drone, DroneDescriptor descriptor);
	
	public void unregister(Drone drone, DroneDescriptor descriptor);
}
