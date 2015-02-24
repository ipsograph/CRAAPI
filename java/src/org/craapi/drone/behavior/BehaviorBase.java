package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.craapi.drone.ControlCommand;
import org.craapi.drone.ControlToken;
import org.craapi.drone.Drone;
import org.craapi.drone.E_DroneState;
import org.craapi.drone.ar2.Util;

public abstract class BehaviorBase implements DroneBehavior {

	protected Drone drone;
	protected ControlToken token;
	private final ExecutorService exec;
	private final boolean localExec;

	public BehaviorBase() {
		this(null);
	}

	public BehaviorBase(ExecutorService exec) {
		if (exec == null) {
			this.exec = Executors.newCachedThreadPool();
			this.localExec = true;
		} else {
			this.exec = exec;
			this.localExec = false;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (localExec) {
				exec.shutdownNow();
			}
		} finally {
			super.finalize();
		}
	}

	@Override
	public void stateChanged(Drone drone, E_DroneState oldState,
			E_DroneState newState) {
		//
	}

	@Override
	public synchronized boolean attach(final Drone drone,
			final ControlToken token) {

		Util.printDebug("Attaching " + this.getClass().getSimpleName());

		if (this.token != null) {
			if (!this.token.equals(token)) {
				Util.printDebug("Replacing token "
						+ this.getClass().getSimpleName());
			} else {
				Util.printWarn("Control granted more than once for"
						+ " same control token (unexpected)");
				return false;
			}
		}

		// if (!checkState(drone.getState())) {
		// return false;
		// }

		this.token = token;
		this.drone = drone;
		exec.submit(new Runnable() {

			@Override
			public void run() {
				Util.printDebug("Start behavior loop... "
						+ BehaviorBase.this.getClass().getSimpleName());
				while (isTokenValid(token)) {
					try {
						final ControlCommand rq = produceRequest(drone, token,
								100);
						if (rq != null) {
							token.submitRequest(rq);
						} else {
							Thread.sleep(50);
						}
					} catch (InterruptedException e) {
						// ignore
					} catch (Exception e) {
						Util.printError(
								"Exception occurred while generating request",
								e, true);
					}
				}
				Util.printDebug("Exit behavior loop... "
						+ BehaviorBase.this.getClass().getSimpleName());
			}
		});

		return true;
	}

	protected abstract ControlCommand produceRequest(Drone drone,
			ControlToken token, long timeoutMs);

	protected boolean checkState(E_DroneState state) {
		return true;
	}

	protected boolean isTokenValid() {
		return isTokenValid(token);
	}

	protected boolean isTokenValid(ControlToken token) {
		return token != null && !token.isExpired() && token.equals(this.token);
	}

	@Override
	public synchronized boolean detach(Drone drone) {

		Util.printDebug("Detaching " + this.getClass().getSimpleName());

		this.token = null;
		this.drone = null;

		return false;
	}
}
