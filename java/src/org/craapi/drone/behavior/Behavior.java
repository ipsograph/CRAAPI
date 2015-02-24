package org.craapi.drone.behavior;

import org.craapi.drone.Bid;
import org.craapi.drone.Drone;

public interface Behavior {
	
	/**
	 * null = no interest
	 * bid with no value = observe only
	 * bid with value but cooperative = control and will allow control attempts of others
	 * bid with value and not cooperative = absolute control over state
	 * 
	 * @param drone
	 * @param highestBid
	 * @return
	 */
	public Bid getBidForControl(Drone drone, Bid highestBid);
}
