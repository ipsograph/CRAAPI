package org.craapi.drone;

public class DroneException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DroneException() {
		
	}

	public DroneException(String message) {
		super(message);
	}

	public DroneException(Throwable cause) {
		super(cause);
	}

	public DroneException(String message, Throwable cause) {
		super(message, cause);
	}
}
