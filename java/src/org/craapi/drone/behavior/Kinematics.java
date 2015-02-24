package org.craapi.drone.behavior;

public class Kinematics {

	protected final long timestamp;
	protected Float yaw;
	protected Float pitch;
	protected Float roll;
	protected Float vertical;
	protected Boolean hover;
	protected Boolean combinedYaw;
	
	public Kinematics() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public Kinematics(Float yaw, Float pitch, Float roll, Float vertical,
			Boolean hover, Boolean combinedYaw) {
		this.timestamp = System.currentTimeMillis();
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		this.vertical = vertical;
		this.hover = hover;
		this.combinedYaw = combinedYaw;
	}

	public Float getYaw() {
		return yaw;
	}

	public void setYaw(Float yaw) {
		this.yaw = yaw;
	}

	public Float getPitch() {
		return pitch;
	}

	public void setPitch(Float pitch) {
		this.pitch = pitch;
	}

	public Float getRoll() {
		return roll;
	}

	public void setRoll(Float roll) {
		this.roll = roll;
	}

	public Float getVertical() {
		return vertical;
	}

	public void setVertical(Float vertical) {
		this.vertical = vertical;
	}

	public Boolean getHover() {
		return hover;
	}

	public void setHover(Boolean hover) {
		this.hover = hover;
	}

	public Boolean getCombinedYaw() {
		return combinedYaw;
	}

	public void setCombinedYaw(Boolean combinedYaw) {
		this.combinedYaw = combinedYaw;
	}
	
	public static Kinematics merge(Kinematics a, Kinematics b) {
		Kinematics merged = new Kinematics();
		if (a.timestamp > b.timestamp) {
			merged.setHover(a.getHover() == null ? b.getHover() : a.getHover());
			merged.setCombinedYaw(a.getCombinedYaw() == null ? b.getCombinedYaw() : a.getCombinedYaw());
			merged.setYaw(a.getYaw() == null ? b.getYaw() : a.getYaw());
			merged.setPitch(a.getPitch() == null ? b.getPitch() : a.getPitch());
			merged.setRoll(a.getRoll() == null ? b.getRoll() : a.getRoll());
			merged.setVertical(a.getVertical() == null ? b.getVertical() : a.getVertical());
		} else {
			merged.setHover(b.getHover() == null ? a.getHover() : b.getHover());
			merged.setCombinedYaw(b.getCombinedYaw() == null ? a.getCombinedYaw() : b.getCombinedYaw());
			merged.setYaw(b.getYaw() == null ? a.getYaw() : b.getYaw());
			merged.setPitch(b.getPitch() == null ? a.getPitch() : b.getPitch());
			merged.setRoll(b.getRoll() == null ? a.getRoll() : b.getRoll());
			merged.setVertical(b.getVertical() == null ? a.getVertical() : b.getVertical());
		}
		
		return merged;
	}
}
