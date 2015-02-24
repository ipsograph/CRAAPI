package org.craapi.drone;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.craapi.drone.ar2.Util;

public class ControlCommand implements Comparable<ControlCommand> {

	private final Object source;
	private final ControlToken token;
	private final Drone drone;
	private final DroneCallable<Boolean> command;
	private final Integer priority;

	public ControlCommand(Object source, ControlToken token, Integer priority,
			Drone drone, DroneCallable<Boolean> command) {
		this.source = source;
		this.token = token;
		this.priority = priority;
		this.drone = drone;
		this.command = command;
	}

	public Drone getDrone() {
		return drone;
	}

	public ControlToken getToken() {
		return token;
	}

	public Object getSource() {
		return source;
	}

	public Future<Boolean> executeAsync(final DroneController drone,
			ExecutorService exec) {

		return exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				try {
					return command.call(drone);
				} catch (Exception e) {
					Util.printError("Failed to execute command", e, true);
					return false;
				}
			}
		});
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Command: token=").append("[");
		sb.append(token).append("], drone=[").append(drone)
				.append("], command=[").append(command.getClass())
				.append("], source=[");
		sb.append(source).append("]");
		return sb.toString();
	}

	@Override
	public int compareTo(ControlCommand o) {
		int v = 0;
		if (token.isExpired()) {
			if (o.getToken().isExpired()) {
				return 0;
			} else {
				v = -1;
			}
		} else {
			if (o.getToken().isExpired()) {
				v = 1;
			} else {
				v = 0;
			}
		}

		if (v != 0) {
			return v;
		}

		if (priority == null) {
			if (o.priority == null) {
				v = 0;
			} else {
				v = -1;
			}
		} else {
			if (o.priority == null) {
				v = 1;
			} else {
				v = priority.compareTo(o.priority);
			}
		}

		if (v != 0) {
			return v;
		}

		if (!token.isAbsolute()) {
			if (!o.token.isAbsolute()) {
				v = 0;
			} else {
				v = -1;
			}
		} else {
			if (!o.token.isAbsolute()) {
				v = 1;
			} else {
				v = 0;
			}
		}

		return v;
	}
}