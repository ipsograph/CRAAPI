package org.craapi.drone;

public interface ControlFactory {
	
	public ControlAuthority getControlAuthority(Drone drone);
}
