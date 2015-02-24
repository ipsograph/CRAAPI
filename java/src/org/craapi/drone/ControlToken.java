package org.craapi.drone;

import java.util.UUID;

public class ControlToken {

	protected boolean absolute;
	protected Integer maxPriority;
	protected final UUID id;
	protected final ControlAuthority ca;
	protected Boolean complete = false;
	protected Boolean cancelled = false;

	public ControlToken(ControlAuthority ca, Integer maxPriority) {
		this(ca, maxPriority, false);
	}

	public ControlToken(ControlAuthority ca, Integer maxPriority,
			boolean absolute) {
		this.id = UUID.randomUUID();
		this.ca = ca;
		this.maxPriority = maxPriority;
		this.absolute = absolute;
	}

	public synchronized void complete() {
		if (!cancelled) {
			complete = true;
		}
	}

	public synchronized void cancel() {
		cancelled = true;
	}

	public boolean isComplete() {
		return complete;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isExpired() {
		return isCancelled() || isComplete();
	}

	public boolean isAbsolute() {
		return absolute;
	}

	public Integer getMaxPriority() {
		return maxPriority;
	}
	
	public void submitRequest(ControlCommand rq) {
		if (!this.equals(rq.getToken())) {
			throw new DroneException("Cannot handle request for another token!");
		}
		ca.submitRequest(rq);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ControlToken) {
			return ((ControlToken) obj).id.equals(id);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (maxPriority != null) {
			sb.append("priority=").append(maxPriority).append(",");
		}
		sb.append("absolute=").append(absolute).append(",");
		sb.append("complete=").append(complete).append(",");
		sb.append("cancelled=").append(cancelled);
		
		return sb.toString();
	}
}
