package org.craapi.drone.test;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.craapi.drone.BasicControlFactory;
import org.craapi.drone.ControlTower;
import org.craapi.drone.Drone;
import org.craapi.drone.DroneDescriptor;
import org.craapi.drone.E_DroneState;
import org.craapi.drone.ar2.ARDrone2;
import org.craapi.drone.behavior.ConnectBehavior;
import org.craapi.drone.behavior.DisconnectBehavior;
import org.craapi.drone.behavior.LandBehavior;
import org.craapi.drone.behavior.RotateBehavior;
import org.craapi.drone.behavior.TakeoffBehavior;
import org.craapi.drone.sim.OfflineSimDrone;

public class TestConnectAndScan {

	public static void main(String[] args) {
		
		final Drone drone = createSimDrone();
		final ControlTower c2 = createTower();
		final DroneDescriptor descriptor = new DroneDescriptor("TEST", drone);
		c2.register(drone, descriptor);
		c2.addStateObserver(descriptor, new ConnectBehavior(), E_DroneState.OFFLINE);
		c2.addStateObserver(descriptor, new TakeoffBehavior(0), E_DroneState.GROUNDED);
		c2.addStateObserver(descriptor, new RotateBehavior(), E_DroneState.HOVER, E_DroneState.INFLIGHT);
		
//		HoverBehavior hover = new HoverBehavior();
//		hover.setHoverTimeMs(10000);
//		c2.addStateObserver(descriptor, hover, E_DroneState.HOVER, E_DroneState.INFLIGHT);
//		c2.addStateObserver(descriptor, new ScanBehavior(), E_DroneState.HOVER);
//		LandBehavior timedLand = new LandBehavior();
//		timedLand.setDelayMs(10000);
//		c2.addStateObserver(descriptor, timedLand, E_DroneState.HOVER, E_DroneState.INFLIGHT);
		c2.addStateObserver(descriptor, new DisconnectBehavior(1), E_DroneState.GROUNDED);
		c2.addStateObserver(descriptor, new LandBehavior(-1), E_DroneState.UNKNOWN, E_DroneState.ERROR);
		c2.start();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JOptionPane.showConfirmDialog(null, "Shutdown drone?");
				System.out.println("SHTUDOWN");
				E_DroneState state = drone.getState();
				if (state != E_DroneState.GROUNDED
						&& state != E_DroneState.DISCONNECTING
						&& state != E_DroneState.OFFLINE) {
					drone.transition(null, E_DroneState.ERROR);
				}
			}
		});
		
//		JFrame controlWindow = new JFrame("Drone Control Frame");
//		JButton kill = new JButton("KILL");
//		kill.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				
//			}
//		});
//		controlWindow.add(kill);
		
		
//		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				System.out.println("SHUTDOWN");
//				drone.transition(null, E_DroneState.ERROR);
//				try {
//					Thread.sleep(10000);
//				} catch (InterruptedException e) {
//					// ignore
//				}
//			}
//		}));
	}

	@SuppressWarnings("unused")
	private static Drone createARDrone() {
		return new ARDrone2();
	}
	
	private static Drone createSimDrone() {
		return new OfflineSimDrone();
	}
	
	private static ControlTower createTower() {
		return new ControlTower(new BasicControlFactory());
	}
}
