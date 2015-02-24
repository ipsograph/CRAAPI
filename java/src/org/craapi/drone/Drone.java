package org.craapi.drone;

import java.util.concurrent.Callable;

public interface Drone {

	public boolean connect();

	public boolean disconnect(boolean force);

	public boolean takeoff();

	public boolean land();
	
	public boolean flatTrim();
	
	public boolean hover();
	
	public boolean spin(float speed);
	
	public boolean move(boolean combineYaw, float yaw, float pitch, float roll, float vertical);
	
	public E_DroneState getState();
	
	public E_DroneState transition(E_DroneState expected, E_DroneState desired);
	
	public E_DroneState transition(E_DroneState expected,
			E_DroneState transition, E_DroneState success,
			E_DroneState failure, Callable<Boolean> task);

	public void addObserver(DroneStateObserver observer);

	public void removeObserver(DroneStateObserver observer);
}
