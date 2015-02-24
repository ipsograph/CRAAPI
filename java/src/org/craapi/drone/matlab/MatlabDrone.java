package org.craapi.drone.matlab;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.craapi.drone.BasicControlFactory;
import org.craapi.drone.ControlTower;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneDescriptor;
import org.craapi.drone.E_DroneState;
import org.craapi.drone.ar2.ARDrone2;
import org.craapi.drone.ar2.ARDrone2WifiConnection;
import org.craapi.drone.ar2.Util;
import org.craapi.drone.behavior.ConnectBehavior;
import org.craapi.drone.behavior.DisconnectBehavior;
import org.craapi.drone.behavior.Kinematics;
import org.craapi.drone.behavior.KinematicsBehavior;
import org.craapi.drone.behavior.LandBehavior;
import org.craapi.drone.behavior.TakeoffBehavior;
import org.craapi.drone.sim.OfflineSimDrone;

/**
 * Simple ARDrone interface (intended for use in Matlab)
 * <p>
 * The underlying code is pure Java with no dependencies. It is rough, but works
 * well enough (with the limited testing I have been able to do). After nearly
 * completing this interface, I stumbled upon the YADrone open source project.
 * If you are interested in using a Java interface with the drone, I highly
 * recommend the YADrone project over this one. This one was coded very quickly
 * and the design is...unfortunate in some regards.
 * <p>
 * The drone code I threw together is not very well documented. If I have time,
 * I will clean it up and document it. The bulk of the interface to the ARDrone
 * 2.0 is in the {@link ARDrone2WifiConnection} class. It is a bit of a
 * monolithic beast that needs to be cleaned up. Again, I strongly recommend
 * using YADrone over my implementation.
 * <p>
 * Basic usage:<br>
 * TODO
 * 
 * @author nate
 * @see <a href="https://github.com/MahatmaX/YADrone">YADrone at Github</a> (link)
 */
public class MatlabDrone {

	protected AtomicBoolean started = new AtomicBoolean();
	protected AtomicBoolean airborne = new AtomicBoolean();
	protected ControlTower c2;
	protected Drone drone;
	protected KinematicsBehavior control;
	protected TakeoffBehavior takeoff;
	protected ExecutorService exec;
	protected boolean simulated = false;
	
	/**
	 * If the provided argument is 'true', the drone will run in simulation mode
	 * with debug logging enabled. This is intended for testing and debugging.
	 * This must be called prior to {@link #init()}.
	 * <p>
	 * The drone defaults to non-simulation mode.
	 * 
	 * @param simFlag
	 *            if <code>"true"</code>, the drone interface will operate in a
	 *            simulation rather than on a real device.
	 */
	public void setSimulation(int simFlag) {
		this.simulated = simFlag == 1;
	}

	/**
	 * Initialize the drone components. This must be called prior to calling
	 * {@link #startDrone()}.
	 */
	public synchronized void init() {

		if (c2 != null) {
			return;
		}

		if (simulated) {
			Util.DEBUG_ENABLED = true;
			Util.INFO_ENABLED = true;
			Util.WARN_ENABLED = true;
		}

		// create necessary components
		drone = simulated ? new OfflineSimDrone() : new ARDrone2();
		c2 = new ControlTower(new BasicControlFactory());
		final DroneDescriptor descriptor = new DroneDescriptor("MATLAB", drone);
		c2.register(drone, descriptor);
	}

	/**
	 * Connects to the drone if not already started and sets up state behaviors.
	 * 
	 */
	public synchronized void startDrone() {

		if (started.compareAndSet(false, true)) {
			
			exec = Executors.newCachedThreadPool();
			
			DroneDescriptor descriptor = new DroneDescriptor("MATLAB", drone);

			// register default behaviors (launch, hover, wait for termination)
			c2.addStateObserver(descriptor, new ConnectBehavior(exec),
					E_DroneState.OFFLINE);
			c2.addStateObserver(descriptor, new DisconnectBehavior(1, exec),
					E_DroneState.GROUNDED);
			c2.addStateObserver(descriptor, new LandBehavior(-1, exec),
					E_DroneState.UNKNOWN, E_DroneState.ERROR);
			
			// register a delayed takeoff behavior
			TakeoffBehavior takeoff = new TakeoffBehavior(0, exec);
			takeoff.setAutomatic(false); // wait for command
			this.takeoff = takeoff;
			c2.addStateObserver(descriptor, takeoff,
					E_DroneState.GROUNDED);
			
			// register the kinematics behavior
			KinematicsBehavior temp = new KinematicsBehavior(exec);
			temp.setMax(new Kinematics(0.5f, 0.5f, 0.3f, 0.3f, null, null));
			temp.setMin(new Kinematics(-0.5f, -0.5f, -0.3f, -0.3f, null, null));
			this.control = temp;
			c2.addStateObserver(descriptor, temp, E_DroneState.HOVER, E_DroneState.INFLIGHT);
			
			// liftoff...
			c2.start();
		}
	}
	
	/**
	 * Returns <code>1</code> if the drone is started and <code>0</code>
	 * otherwise.
	 * 
	 * @return <code>1</code> if the drone is started and <code>0</code>
	 *         otherwise
	 */
	public int isRunning() {
		return started.get() ? 1 : 0;
	}

	/**
	 * Returns <code>1</code> if the drone is airborne and <code>0</code>
	 * otherwise.
	 * 
	 * @return <code>1</code> if the drone is airborne and <code>0</code>
	 *         otherwise
	 */
	public int isAirborne() {
		return (started.get() && airborne.get()) ? 1 : 0;
	}

	/**
	 * Stops the drone, which includes landing if necessary. After this is
	 * called, {@link #init()} and {@link #startDrone()} must be called in order
	 * to get back to a functional state.
	 * <p>
	 * If you forget to call this prior to exiting Matlab, you may have a
	 * runaway drone on your hands. Call this method directly from the Matlab
	 * command prompt if necessary.
	 */
	public synchronized void stopDrone() {

		if (started.compareAndSet(true, false)) {

			E_DroneState state = drone.getState();
			if (state != E_DroneState.GROUNDED
					&& state != E_DroneState.DISCONNECTING
					&& state != E_DroneState.OFFLINE) {
				drone.transition(null, E_DroneState.ERROR); // hack to cause a
															// landing
			}

			try {
				// give the drone 5 seconds to land
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// ignore
			}

			airborne.set(false);

			c2.stop();
			c2.removeAllObservers(drone);
			control = null;
			takeoff = null;
			exec.shutdownNow();
			exec = null;
		}
	}

	/**
	 * If called after {@link #startDrone()}, this causes the drone to take off
	 * and enter a hovering state. This must be called prior to calling
	 * {@link #setKinematics(String)}.
	 */
	public void takeoff() {
		// update behavior
		if (started.get() && airborne.compareAndSet(false, true)) {
			synchronized (this) {
				if (takeoff != null) {
					takeoff.approveTakeoff();
				}
			}
		}
	}

	/**
	 * Accepts a string that contains the desired kinematics. I left this as a
	 * string for maximum compatibility with Matlab and the possibility of
	 * adding more name/value pairs without changing the API.
	 * <p>
	 * Accepts a comma-delimited string:<br>
	 * e.g., <i>yaw=%f,pitch=%f,roll=%f,alt=%f,combine=true,hover=true<br>
	 * </i>
	 * <p>
	 * Possible values:<br>
	 * <ul>
	 * <li><b>yaw</b> - the horizontal plane rotation speed</li>
	 * <li><b>pitch</b> - the front-back angle/speed (negative is backwards
	 * motion)</li>
	 * <li><b>roll</b> - the side-to-side angle/speed (negative is leftward
	 * motion)</li>
	 * <li><b>alt</b> - the up-down speed (negative is downward motion)</li>
	 * <li><b>combine</b> - enables 'combined yaw mode' (no clue)</li>
	 * <li><b>hover</b> - enables 'hover mode' (other arguments are ignored;
	 * drone is stationary)</li>
	 * </ul>
	 * <p>
	 * <b>NOTE:</b> All speeds are percentages of the maximum (absolute) speed.
	 * For example, a value of 1.0 means "max speed in x direction". A value of
	 * 0.0 means "no speed in x direction". So, all values are expressed in
	 * range [-1.0,1.0].
	 * 
	 * @param kinematics
	 */
	public void setKinematics(String kinematics) {
		
		if (!airborne.get() || !started.get()) {
			return;
		}

		final Kinematics kin = new Kinematics();

		if (kinematics == null) {
			kin.setHover(true);
		} else {
			kinematics = kinematics.trim();
			if (kinematics.isEmpty()) {
				kin.setHover(true);
			} else {
				final String[] tokens = kinematics.split(",");
				final Map<String, String> args = new HashMap<String, String>(
						tokens.length);
				for (String t : tokens) {
					String[] nvPair = t.split("=", 2);
					if (nvPair.length == 2) {
						args.put(nvPair[0].trim().toLowerCase(), nvPair[1]
								.trim().toLowerCase());
					}
				}

				kin.setHover(Boolean.parseBoolean(args.get("hover")));
				if (!kin.getHover()) {
					kin.setCombinedYaw(Boolean.parseBoolean(args.get("combine")));
					try {
						kin.setYaw(Float.parseFloat(args.get("yaw")));
					} catch (Exception e) {
						// ignore
					}
					try {
						kin.setPitch(Float.parseFloat(args.get("pitch")));
					} catch (Exception e) {
						// ignore
					}
					try {
						kin.setRoll(Float.parseFloat(args.get("roll")));
					} catch (Exception e) {
						// ignore
					}
					try {
						kin.setVertical(Float.parseFloat(args.get("alt")));
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}

		// update behavior
		synchronized(this) {
			if (control != null) {
				control.adjustKinematics(kin);
			}
		}
	}
	
	public static void main(String[] args) {
		main(1,true);
	}
	
	public static void mainForMatlab(final int simFlag) {
		final Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				main(simFlag, false);
			}
		});
		t.start();
	}
	
	public static void main(int simFlag, boolean showControlDialog) {
		
		final MatlabDrone drone = new MatlabDrone();
		drone.setSimulation(1);
		drone.init();
		drone.startDrone();
		
		if (showControlDialog) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					JOptionPane.showConfirmDialog(null, "Shutdown drone?");
					drone.stopDrone();
				}
			});
		}
		
		// takeoff
		drone.takeoff();
		
		// set some kinematics...
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("hover=false,yaw=0.2");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("hover=true");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("yaw=0.0,pitch=0.2");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("hover=true");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("pitch=0.0,roll=0.2");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("hover=true");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("alt=-0.1,yaw=-0.2,roll=0.0");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.setKinematics("hover=true");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
		
		drone.stopDrone();
	}
}
