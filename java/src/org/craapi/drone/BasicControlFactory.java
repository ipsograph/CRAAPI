package org.craapi.drone;

public class BasicControlFactory implements ControlFactory {

	@Override
	public ControlAuthority getControlAuthority(Drone drone) {
		return new BasicController(drone);
	}
}
