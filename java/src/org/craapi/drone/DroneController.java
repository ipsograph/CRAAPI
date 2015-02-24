package org.craapi.drone;

import java.util.concurrent.Callable;

public interface DroneController {

	public boolean connect(ControlToken token);

	public boolean disconnect(ControlToken token, boolean force);

	public E_DroneState getState();

	public E_DroneState transition(ControlToken token, E_DroneState expected,
			E_DroneState desired);

	public E_DroneState transition(ControlToken token, E_DroneState expected,
			E_DroneState transition, E_DroneState success,
			E_DroneState failure, Callable<Boolean> task);

	public boolean takeoff(ControlToken token);
	
	public boolean land(ControlToken token);
	
	public boolean flatTrim(ControlToken token);
	
	public boolean spin(ControlToken token, float yawSpeed);
	
	public boolean hover(ControlToken token);
	
	public boolean move(ControlToken token, boolean combineYaw, float yaw, float pitch, float roll, float vertical);
}
