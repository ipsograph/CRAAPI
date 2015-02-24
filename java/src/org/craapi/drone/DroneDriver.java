package org.craapi.drone;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.craapi.drone.behavior.DroneBehavior;

public class DroneDriver implements DroneStateController {

	private final ControlAuthority control;
	private final DroneDescriptor drone;
	private final ConcurrentMap<E_DroneState, List<DroneObserver>> observers = new ConcurrentHashMap<E_DroneState, List<DroneObserver>>();
	private final StateMonitor monitor = new StateMonitor();

	public DroneDriver(DroneDescriptor drone, ControlAuthority control) {
		this.drone = drone;
		this.control = control;
		for (E_DroneState state : E_DroneState.values()) {
			observers.put(state,
					new LinkedList<DroneObserver>());
		}
	}

	@Override
	public void addStateObserver(Drone drone, DroneObserver observer,
			E_DroneState... states) {
		
		if (!this.drone.equals(drone)) {
			throw new DroneException("Cannot process request to "
					+ "observe drone that is "
					+ "not managed by this driver");
		}
		
		for (E_DroneState s : states) {
			final List<DroneObserver> obs = observers.get(s);
			obs.add(observer);
		}
	}

	@Override
	public void removeStateObserver(Drone drone, DroneObserver observer) {
		if (!this.drone.equals(drone)) {
			throw new DroneException("Cannot process request to "
					+ "remove observer from drone that is "
					+ "not managed by this driver");
		}
		
		for (E_DroneState s : E_DroneState.values()) {
			final List<DroneObserver> obs = observers.get(s);
			obs.remove(observer);
		}
	}
	
	@Override
	public void removeAllObservers(Drone drone) {
		if (!this.drone.equals(drone)) {
			throw new DroneException("Cannot process request to "
					+ "remove all observers from drone that is "
					+ "not managed by this driver");
		}
		
		for (E_DroneState s : E_DroneState.values()) {
			final List<DroneObserver> obs = observers.get(s);
			obs.clear();
		}
	}
	
	@Override
	public synchronized void start() {
		drone.addObserver(monitor);
		control.start();
	}

	@Override
	public synchronized void stop() {
		control.stop();
		drone.removeObserver(monitor);
	}

	protected class StateMonitor implements DroneStateObserver {
		
		private E_DroneState lastState;

		@Override
		public void stateChanged(Drone drone, E_DroneState oldState,
				E_DroneState newState) {
			
			if (lastState == newState) {
				return;
			} else {
				lastState = newState;
			}
			
			final List<DroneObserver> all = observers.get(newState);
			final Map<DroneBehavior, Bid> bids = new HashMap<DroneBehavior, Bid>();
			Bid best = null;
			
			// initialize
			for (DroneObserver o : all) {
				if (o instanceof DroneBehavior) {
					final Bid temp = ((DroneBehavior) o).getBidForControl(drone, null);
					if (temp != null) {
						bids.put((DroneBehavior)o,temp);
						if (best == null || temp.compareTo(best) < 0) {
							best = temp;
						}
					}
				} else {
					// if just an observer, then what?
				}
			}
			
			// if no one wants control...
			if (best == null) {
				return;
			}
			
			// allow controller to deal with conflicts/priorities
			control.requestControl(bids.values().toArray(new Bid[bids.size()]));
		}
	}
}
