package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.craapi.drone.Bid;
import org.craapi.drone.ControlCommand;
import org.craapi.drone.ControlToken;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneCallable;
import org.craapi.drone.DroneController;

public abstract class OneShotBehavior extends BehaviorBase {

	protected final AtomicBoolean rqSubmitted = new AtomicBoolean(false);
	protected final AtomicBoolean rqComplete = new AtomicBoolean(false);
	protected final int bid;
	protected long delayMs = 0;
	protected long startMs = 0;

	public OneShotBehavior() {
		this(Integer.MAX_VALUE);
	}

	public OneShotBehavior(int bid) {
		this(bid, null);
	}

	public OneShotBehavior(ExecutorService exec) {
		this(Integer.MAX_VALUE, null);
	}

	public OneShotBehavior(int bid, ExecutorService exec) {
		super(exec);
		this.bid = bid;
	}
	
	public void setDelayMs(long delayMs) {
		this.delayMs = delayMs;
	}

	@Override
	public Bid getBidForControl(Drone drone, Bid highestBid) {
		
		// do not complete
		if (rqComplete.get() || highestBid != null
				&& highestBid.getValue() != null) {
			return null;
		}

		return new Bid(this, bid, false);
	}

	@Override
	protected ControlCommand produceRequest(Drone drone, ControlToken token,
			long timeoutMs) {

		boolean waited = (System.currentTimeMillis() - startMs) >= delayMs;
		if (waited && rqSubmitted.compareAndSet(false, true)) {
			return new ControlCommand(this, token, bid, drone, new OneShotTask(
					token));
		} else {
			if (timeoutMs > 0) {
				try {
					Thread.sleep(timeoutMs);
				} catch (InterruptedException e) {
					// ignore
				}
			}

			return null;
		}
	}
	
	@Override
	public synchronized boolean attach(Drone drone, ControlToken token) {
		
		startMs = System.currentTimeMillis();
		
		return super.attach(drone, token);
	}
	
	@Override
	public synchronized boolean detach(Drone drone) {
		
		rqSubmitted.set(false);
		
		return super.detach(drone);
	}

	protected abstract Boolean oneShot(DroneController drone, ControlToken token);

	protected class OneShotTask implements DroneCallable<Boolean> {

		protected final ControlToken token;

		public OneShotTask(ControlToken token) {
			this.token = token;
		}

		@Override
		public Boolean call(DroneController drone) throws Exception {
			Boolean r = oneShot(drone, token);
			if (Boolean.TRUE.equals(r)) {
				rqComplete.set(true);
			}
			return r;
		}
	}
}
