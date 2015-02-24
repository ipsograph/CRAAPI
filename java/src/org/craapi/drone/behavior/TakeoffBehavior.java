package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.ControlToken;
import org.craapi.drone.DroneController;
import org.craapi.drone.ar2.Util;

public class TakeoffBehavior extends OneShotBehavior {

	protected boolean automatic = true;
	protected boolean takeoffCommandReceived = false;
	
	public TakeoffBehavior() {
		super();
	}

	public TakeoffBehavior(ExecutorService exec) {
		super(exec);
	}

	public TakeoffBehavior(int bid, ExecutorService exec) {
		super(bid, exec);
	}

	public TakeoffBehavior(int bid) {
		super(bid);
	}
	
	public void setAutomatic(boolean automatic) {
		this.automatic = automatic;
	}
	
	public void approveTakeoff() {
		this.takeoffCommandReceived = true;
	}
	
	public void denyTakeoff() {
		this.takeoffCommandReceived = false;
	}

	@Override
	protected Boolean oneShot(DroneController drone, ControlToken token) {
		if (automatic || takeoffCommandReceived) {
			Util.printDebug("Takeoff start...");
			final Boolean result = drone.takeoff(token);
			Util.printDebug("Takeoff complete: " + result);
			return Boolean.TRUE; // no matter what, do not do this again
		}
		return Boolean.FALSE;
	}
}
