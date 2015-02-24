package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.ControlToken;
import org.craapi.drone.DroneController;
import org.craapi.drone.ar2.Util;

public class ConnectBehavior extends OneShotBehavior {

	public ConnectBehavior() {
		super();
	}

	public ConnectBehavior(ExecutorService exec) {
		super(exec);
	}

	@Override
	protected Boolean oneShot(DroneController drone, ControlToken token) {

		Util.printInfo("Attempting to connect to the drone...");

		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		} while (isTokenValid(token) && !drone.connect(token));

		token.complete();

		return Boolean.TRUE; // no matter what, do not do this again
	}
}
