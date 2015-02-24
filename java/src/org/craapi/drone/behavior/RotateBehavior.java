package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.Bid;
import org.craapi.drone.ControlCommand;
import org.craapi.drone.ControlToken;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneCallable;
import org.craapi.drone.DroneController;

public class RotateBehavior extends BehaviorBase {
	
	// -1 to 1 as per API
	protected float speed;
	protected long lastTs;
	
	public RotateBehavior() {
		super();
	}

	public RotateBehavior(ExecutorService exec) {
		super(exec);
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	@Override
	public Bid getBidForControl(Drone drone, Bid highestBid) {
		// do not complete
		if (highestBid != null && highestBid.getValue() != null) {
			return null;
		}

		return new Bid(this, Integer.MAX_VALUE, true);
	}
	
	@Override
	protected ControlCommand produceRequest(Drone drone, ControlToken token,
			long timeoutMs) {
		long now = System.currentTimeMillis();
		if (now - lastTs > 100) {
			lastTs = now;
			return new ControlCommand(this, token, Integer.MAX_VALUE,
						drone, new Rotate());
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}

		return null;
	}
	
	protected class Rotate implements DroneCallable<Boolean> {

		@Override
		public Boolean call(DroneController drone) throws Exception {
			
			return drone.spin(token, 0.2f);
		}
	}
}
