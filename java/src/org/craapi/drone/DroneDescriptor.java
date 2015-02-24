package org.craapi.drone;

import java.util.concurrent.Callable;

public class DroneDescriptor implements Drone {

	private final Drone drone;
	private final String id;

	public DroneDescriptor(String id) {
		this.id = id;
		this.drone = null;
	}

	public DroneDescriptor(String id, Drone drone) {
		this.id = id;
		this.drone = drone;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof DroneDescriptor) {
			return ((DroneDescriptor) o).getId().equals(id);
		} else if (o instanceof Drone) {
			return o.equals(drone);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return drone != null ? drone.hashCode() : id.hashCode();
	}

	@Override
	public String toString() {
		return "Drone Descriptor '" + id + "' [" + drone + "]";
	}

	@Override
	public boolean connect() {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public boolean disconnect(boolean force) {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public boolean land() {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public boolean takeoff() {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}
	
	@Override
	public boolean flatTrim() {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}
	
	@Override
	public boolean spin(float yawSpeed) {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}
	
	@Override
	public boolean hover() {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}
	
	@Override
	public boolean move(boolean combineYaw, float yaw, float pitch, float roll, float vertical) {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public E_DroneState getState() {
		if (drone == null) {
			throw new DroneException("Drone reference is not available");
		}
		return drone.getState();
	}

	@Override
	public E_DroneState transition(E_DroneState expected, E_DroneState desired) {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public E_DroneState transition(E_DroneState expected,
			E_DroneState transition, E_DroneState success,
			E_DroneState failure, Callable<Boolean> task) {
		throw new DroneException(
				"The drone state cannot be changed through this reference");
	}

	@Override
	public void addObserver(DroneStateObserver observer) {
		if (drone == null) {
			throw new DroneException("Drone reference is not available");
		}
		drone.addObserver(observer);
		observer.stateChanged(drone, null, drone.getState());
	}

	@Override
	public void removeObserver(DroneStateObserver observer) {
		if (drone == null) {
			throw new DroneException("Drone reference is not available");
		}
		drone.removeObserver(observer);
	}
}
