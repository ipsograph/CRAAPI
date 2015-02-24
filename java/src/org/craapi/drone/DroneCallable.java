package org.craapi.drone;


public interface DroneCallable<V> {
	
	public V call(DroneController drone) throws Exception;
}
