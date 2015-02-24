package org.craapi.drone.sim;

import org.craapi.drone.DroneBase;

public class OfflineSimDrone extends DroneBase {

	public OfflineSimDrone() {
		super(new OfflineSimConnection());
	}
}
