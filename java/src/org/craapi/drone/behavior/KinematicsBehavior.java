package org.craapi.drone.behavior;

import java.util.concurrent.ExecutorService;

import org.craapi.drone.Bid;
import org.craapi.drone.ControlCommand;
import org.craapi.drone.ControlToken;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneCallable;
import org.craapi.drone.DroneController;

public class KinematicsBehavior extends BehaviorBase {

	protected Kinematics max;
	protected Kinematics min;
	protected Kinematics kinematics;
	protected long lastTs;

	public KinematicsBehavior() {
		super();
		this.resetKinematics();
	}

	public KinematicsBehavior(ExecutorService exec) {
		super(exec);
		this.resetKinematics();
	}

	public void resetKinematics() {
		Kinematics temp = new Kinematics();
		temp.setCombinedYaw(false);
		temp.setHover(true);
		temp.setPitch(0.f);
		temp.setRoll(0.f);
		temp.setYaw(0.f);
		temp.setVertical(0.f);
		this.kinematics = temp;
	}

	public void setMax(Kinematics max) {
		this.max = max;
	}

	public void setMin(Kinematics min) {
		this.min = min;
	}

	public void adjustKinematics(Kinematics newKinematics) {
		this.kinematics = Kinematics.merge(kinematics, newKinematics);
	}

	@Override
	public Bid getBidForControl(Drone drone, Bid highestBid) {
		// do not complete
		if (highestBid != null && highestBid.getValue() != null) {
			return null;
		}

		return new Bid(this, 1, true);
	}

	@Override
	protected ControlCommand produceRequest(Drone drone, ControlToken token,
			long timeoutMs) {
		long now = System.currentTimeMillis();
		if (now - lastTs > 100) {
			lastTs = now;
			return new ControlCommand(this, token, Integer.MAX_VALUE, drone,
					new Move(kinematics));
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}

		return null;
	}

	protected class Move implements DroneCallable<Boolean> {

		protected final Kinematics kref;

		public Move(Kinematics kref) {
			this.kref = kref;
		}

		@Override
		public Boolean call(DroneController drone) throws Exception {

			boolean hover = kref.getHover() == null ? false : kref.getHover();
			if (hover) {
				return drone.hover(token);
			}

			float yaw = kref.getYaw() == null ? 0.f : kref.getYaw();
			float pitch = kref.getPitch() == null ? 0.f : kref.getPitch();
			float roll = kref.getRoll() == null ? 0.f : kref.getRoll();
			float vertical = kref.getVertical() == null ? 0.f : kref
					.getVertical();
			boolean combine = kref.getCombinedYaw() == null ? false : kref
					.getCombinedYaw();

			if (max != null) {
				if (max.getYaw() != null) {
					yaw = Math.min(max.getYaw(), yaw);
				}
				if (max.getPitch() != null) {
					pitch = Math.min(max.getPitch(), pitch);
				}
				if (max.getRoll() != null) {
					roll = Math.min(max.getRoll(), roll);
				}
				if (max.getVertical() != null) {
					vertical = Math.min(max.getVertical(), vertical);
				}
			}
			if (min != null) {
				if (min.getYaw() != null) {
					yaw = Math.max(min.getYaw(), yaw);
				}
				if (min.getPitch() != null) {
					pitch = Math.max(min.getPitch(), pitch);
				}
				if (min.getRoll() != null) {
					roll = Math.max(min.getRoll(), roll);
				}
				if (min.getVertical() != null) {
					vertical = Math.max(min.getVertical(), vertical);
				}
			}

			return drone.move(token, combine, yaw, pitch, roll, vertical);
		}
	}
}
