package org.craapi.drone.ar2;

import org.craapi.drone.DroneBase;

public class ARDrone2 extends DroneBase {
	
	public ARDrone2() {
		super(new ARDrone2WifiConnection());
	}
}
