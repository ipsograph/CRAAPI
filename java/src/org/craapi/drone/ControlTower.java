package org.craapi.drone;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ControlTower implements DroneStateController, DroneRegistry {

	private final ConcurrentMap<DroneDescriptor, Drone> droneLookup = new ConcurrentHashMap<DroneDescriptor, Drone>();
	private final ConcurrentMap<DroneDescriptor, DroneDriver> registry = new ConcurrentHashMap<DroneDescriptor, DroneDriver>();
	private final ControlFactory factory;

	public ControlTower(ControlFactory factory) {
		this.factory = factory;
	}

	@Override
	public void addStateObserver(Drone drone, DroneObserver requester,
			E_DroneState... states) {
		final DroneDriver entry = registry.get(drone);
		if (entry != null) {

			if (states == null || states.length == 0) {
				states = E_DroneState.values();
			}

			entry.addStateObserver(drone, requester, states);
		}
	}
	
	@Override
	public void removeStateObserver(Drone drone, DroneObserver requester) {
		final DroneDriver entry = registry.get(drone);
		if (entry != null) {
			entry.removeStateObserver(drone, requester);
		}
	}
	
	@Override
	public void removeAllObservers(Drone drone) {
		final DroneDriver entry = registry.get(drone);
		if (entry != null) {
			entry.removeAllObservers(drone);
		}
	}

	@Override
	public void register(Drone drone, DroneDescriptor descriptor) {
		DroneDriver entry = new DroneDriver(descriptor,
				factory.getControlAuthority(drone));
		registry.putIfAbsent(descriptor, entry);
		droneLookup.put(descriptor, drone);
	}

	@Override
	public void unregister(Drone drone, DroneDescriptor descriptor) {
		final DroneDriver entry = registry.get(descriptor);
		final Drone registered = droneLookup.get(descriptor);
		if ((registered == null && entry != null) || !registered.equals(drone)) {
			throw new DroneException("Cannot unregister drone '"
					+ descriptor.getId() + "' due to instance mismatch");
		}
		if (entry != null) {
			entry.stop();
			registry.remove(descriptor);
			droneLookup.remove(descriptor);
		}
	}

	@Override
	public synchronized void start() {
		for (DroneDriver entry : registry.values()) {
			entry.start();
		}
	}

	@Override
	public synchronized void stop() {
		for (DroneDriver entry : registry.values()) {
			entry.stop();
		}
	}
}
