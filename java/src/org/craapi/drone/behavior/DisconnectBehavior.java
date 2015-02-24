package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.ControlToken;
import org.craapi.drone.DroneController;

public class DisconnectBehavior extends OneShotBehavior {

	public DisconnectBehavior() {
		super();
	}

	public DisconnectBehavior(ExecutorService exec) {
		super(exec);
	}

	public DisconnectBehavior(int bid, ExecutorService exec) {
		super(bid, exec);
	}

	public DisconnectBehavior(int bid) {
		super(bid);
	}

	@Override
	protected Boolean oneShot(DroneController drone, ControlToken token) {
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		} while (isTokenValid(token) && !drone.disconnect(token, false));
		
		token.complete();

		return !token.isCancelled();
	}
}
