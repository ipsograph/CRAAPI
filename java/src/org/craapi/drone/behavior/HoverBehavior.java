package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.Bid;
import org.craapi.drone.ControlCommand;
import org.craapi.drone.ControlToken;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneCallable;
import org.craapi.drone.DroneController;

public class HoverBehavior extends BehaviorBase {

	private long hoverTimeMs = 1000;
	private long startTs = 0;
	private long lastHoverTs = 0;

	public HoverBehavior() {
		super();
	}

	public HoverBehavior(ExecutorService exec) {
		super(exec);
	}

	public void setHoverTimeMs(long hoverTimeMs) {
		this.hoverTimeMs = hoverTimeMs;
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
		if (now - lastHoverTs > 100) {
			lastHoverTs = now;
			if (startTs == 0) {
				startTs = now;
			}
			if (hoverTimeMs > 0 && (lastHoverTs - startTs) < hoverTimeMs) {
				return new ControlCommand(this, token, Integer.MAX_VALUE,
						drone, new Hover());
			}
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}

		return null;
	}

	protected class Hover implements DroneCallable<Boolean> {

		@Override
		public Boolean call(DroneController drone) throws Exception {
			return true;
		}
	}
}
