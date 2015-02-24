package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.ControlToken;
import org.craapi.drone.DroneController;
import org.craapi.drone.ar2.Util;

public class LandBehavior extends OneShotBehavior {

	public LandBehavior() {
		super();
	}

	public LandBehavior(ExecutorService exec) {
		super(exec);
	}

	public LandBehavior(int bid, ExecutorService exec) {
		super(bid, exec);
	}

	public LandBehavior(int bid) {
		super(bid);
	}

	@Override
	protected Boolean oneShot(DroneController drone, ControlToken token) {
		Util.printDebug("Land start...");
		final Boolean result = drone.land(token);
		Util.printDebug("Land complete: " + result);
		return result;
	}
}
