package org.craapi.drone;

public interface ControlAuthority extends DroneController {
	
	public void submitRequest(ControlCommand rq);
	
	public void requestControl(Bid... bidsForControl);
	
	public void start();
	
	public void stop();
}
