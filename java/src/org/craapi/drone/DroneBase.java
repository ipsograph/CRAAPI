package org.craapi.drone;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.craapi.drone.ar2.Util;

public abstract class DroneBase implements Drone {

	protected final List<DroneStateObserver> observers = new LinkedList<DroneStateObserver>();
	protected E_DroneState state = E_DroneState.OFFLINE;
	protected ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
	protected ControlConnection connection;
	protected ExecutorService exec;

	public DroneBase(ControlConnection connection) {
		this.connection = connection;
		this.exec = Executors.newCachedThreadPool();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			exec.shutdownNow();
		} finally {
			super.finalize();
		}
	}

	@Override
	public E_DroneState getState() {
		stateLock.readLock().lock();
		try {
			return state;
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public boolean connect() {

		stateLock.writeLock().lock();
		try {
			if (state != E_DroneState.OFFLINE) {
				Util.printWarn("Cannot connect while in state " + state);
				return false;
			}

			if (connection.establish()) {
				transition(E_DroneState.OFFLINE, E_DroneState.GROUNDED);
				return true;
			}

		} finally {
			stateLock.writeLock().unlock();
		}

		return false;
	}

	@Override
	public boolean disconnect(boolean force) {

		stateLock.writeLock().lock();
		try {

			if (state != E_DroneState.GROUNDED && !force) {
				Util.printWarn("Cannot disconnect while in state " + state);
				return false;
			}

			if (connection.close()) {
				transition(null, E_DroneState.OFFLINE);
				return true;
			}

		} finally {
			stateLock.writeLock().unlock();
		}

		return false;
	}

	@Override
	public boolean takeoff() {

		stateLock.writeLock().lock();
		try {

			if (state != E_DroneState.GROUNDED) {
				Util.printWarn("Cannot takeoff while in state " + state);
				return false;
			}

			final E_DroneState result = transition(E_DroneState.GROUNDED,
					E_DroneState.TAKEOFF, E_DroneState.HOVER,
					E_DroneState.UNKNOWN, new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							if (connection.sendFlatTrimCommand()) {
								return connection.sendTakeoffCommand();
							} else {
								return false;
							}
						}
					});

			return E_DroneState.HOVER == result;
		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public boolean land() {

		stateLock.writeLock().lock();
		try {

			if (state == E_DroneState.GROUNDED
					|| state == E_DroneState.DISCONNECTING
					|| state == E_DroneState.OFFLINE) {
				Util.printWarn("Cannot land while in state " + state);
				return false;
			}

			final E_DroneState result = transition(null, E_DroneState.LANDING,
					E_DroneState.GROUNDED, E_DroneState.UNKNOWN,
					new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return connection.sendLandCommand();
						}
					});

			return E_DroneState.GROUNDED == result;
		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public boolean flatTrim() {
		stateLock.readLock().lock();
		try {

			if (state == E_DroneState.DISCONNECTING
					|| state == E_DroneState.OFFLINE) {
				Util.printWarn("Cannot set trim while in state " + state);
				return false;
			}

			return connection.sendFlatTrimCommand();
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public boolean spin(float yawSpeed) {
		stateLock.readLock().lock();
		try {

			if (state != E_DroneState.INFLIGHT && state != E_DroneState.HOVER) {
				Util.printWarn("Cannot spin while in state " + state);
				return false;
			}

			return connection.sendKinematics(false, false, yawSpeed, 0, 0, 0);
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public boolean hover() {
		stateLock.readLock().lock();
		try {

			if (state != E_DroneState.INFLIGHT && state != E_DroneState.HOVER) {
				Util.printWarn("Cannot hover while in state " + state);
				return false;
			}

			return connection.sendKinematics(true, false, 0.f, 0.f, 0.f, 0.f);
		} finally {
			stateLock.readLock().unlock();
		}
	}
	
	@Override
	public boolean move(boolean combineYaw, float yaw, float pitch, float roll, float vertical) {
		stateLock.readLock().lock();
		try {

			if (state != E_DroneState.INFLIGHT && state != E_DroneState.HOVER) {
				Util.printWarn("Cannot move while in state " + state);
				return false;
			}

			return connection.sendKinematics(false, combineYaw, yaw, pitch, roll, vertical);
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public E_DroneState transition(E_DroneState expected, E_DroneState desired) {
		stateLock.writeLock().lock();
		try {

			E_DroneState oldState = state;
			if (expected == null || state == expected) {
				state = desired;
				if (state != oldState) {
					fireStateChangedAsync(oldState, state);
				}
			}

			return state;

		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public E_DroneState transition(E_DroneState expected,
			E_DroneState transition, E_DroneState success,
			E_DroneState failure, Callable<Boolean> task) {

		stateLock.writeLock().lock();
		try {

			E_DroneState initState = state;
			E_DroneState oldState = state;
			if (expected == null || state == expected) {

				state = transition;
				if (state != oldState) {
					fireStateChangedAsync(oldState, state);
				}

				try {
					oldState = state;
					if (Boolean.TRUE.equals(task.call())) {
						state = success == null ? initState : success;
					} else {
						state = failure == null ? initState : failure;
					}
					if (state != oldState) {
						fireStateChangedAsync(oldState, state);
					}
				} catch (Exception e) {
					Util.printError(
							"Exception encountered while transitioning state",
							e, true);
				}
			}
			Util.printDebug("State after action : " + state);
			return state;

		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public void addObserver(DroneStateObserver observer) {
		synchronized (observers) {
			if (!observers.contains(observer)) {
				observers.add(observer);
			}
		}
	}

	@Override
	public void removeObserver(DroneStateObserver observer) {
		synchronized (observers) {
			observers.remove(observer);
		}
	}

	protected void fireStateChangedAsync(final E_DroneState oldState,
			final E_DroneState newState) {
		Util.printInfo("State: " + oldState + " -> " + newState);
		synchronized (observers) {
			for (final DroneStateObserver o : observers) {
				exec.submit(new Runnable() {

					@Override
					public void run() {
						try {
							o.stateChanged(DroneBase.this, oldState, newState);
						} catch (Exception e) {
							Util.printError("Encountered problem while firing"
									+ " state changed event", e, true);
						}
					}
				});
			}
		}
	}

	protected void fireStateChanged(E_DroneState oldState, E_DroneState newState) {
		Util.printInfo("State: " + oldState + " -> " + newState);
		synchronized (observers) {
			for (DroneStateObserver o : observers) {
				try {
					o.stateChanged(this, oldState, newState);
				} catch (Exception e) {
					Util.printError("Encountered problem while firing"
							+ " state changed event", e, true);
				}
			}
		}
	}
}
