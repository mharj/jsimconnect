package flightsim.simconnect.samples;

import java.io.IOException;

import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;

public class JoystickInput implements EventHandler {
	static enum GROUP_ID {
	    GROUP_0
	};

	static enum INPUT_ID {
	    INPUT_Z,
	    INPUT_SLIDER,
	    INPUT_XAXIS,
	    INPUT_YAXIS,
	    INPUT_RZAXIS,
	    INPUT_HAT,
	};

	static enum EVENT_ID {
	    EVENT_Z,
	    EVENT_SLIDER,
	    EVENT_XAXIS,
	    EVENT_YAXIS,
	    EVENT_RZAXIS,
	    EVENT_HAT,
	 };

	
	private JoystickInput() throws IOException, ConfigurationNotFoundException {

		SimConnect sc = new SimConnect("Joystick input", 0);
		
        // Set up some private events
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_Z);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_SLIDER);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_XAXIS);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_YAXIS);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_RZAXIS);
		sc.mapClientEventToSimEvent(EVENT_ID.EVENT_HAT);
	      

        // Add all the private events to a notifcation group
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_Z);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_SLIDER);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_XAXIS);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_YAXIS);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_RZAXIS);
		sc.addClientEventToNotificationGroup(GROUP_ID.GROUP_0, EVENT_ID.EVENT_HAT);

        // Set a high priority for the group
		sc.setNotificationGroupPriority(GROUP_ID.GROUP_0, NotificationPriority.HIGHEST);

        // Map input events to the private client events
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_Z, "z", EVENT_ID.EVENT_Z);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_SLIDER, "joystick:0:slider", EVENT_ID.EVENT_SLIDER);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_XAXIS, "joystick:0:XAxis", EVENT_ID.EVENT_XAXIS);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_YAXIS, "joystick:0:YAxis", EVENT_ID.EVENT_YAXIS);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_RZAXIS, "joystick:0:Rzaxis", EVENT_ID.EVENT_RZAXIS);
		sc.mapInputEventToClientEvent(INPUT_ID.INPUT_HAT, "joystick:0:POV", EVENT_ID.EVENT_HAT);
		
        // Turn on the Z key
		sc.setInputGroupState(INPUT_ID.INPUT_Z, true);
		sc.setInputGroupPriority(INPUT_ID.INPUT_Z, NotificationPriority.HIGHEST);

        // Turn all the joystick events off
		sc.setInputGroupState(INPUT_ID.INPUT_SLIDER, false);
		sc.setInputGroupState(INPUT_ID.INPUT_XAXIS, false);
		sc.setInputGroupState(INPUT_ID.INPUT_YAXIS, false);
		sc.setInputGroupState(INPUT_ID.INPUT_RZAXIS, false);
		sc.setInputGroupState(INPUT_ID.INPUT_HAT, false);

		// dispatcher
		DispatcherTask dt = new DispatcherTask(sc);
		dt.addEventHandler(this);
		dt.addExceptionHandler(new ExceptionHandler() {
			public void handleException(SimConnect sender, RecvException e) {
				System.err
						.println("Exception: " + e.getException() + " packet "
								+ e.getSendID() + " index " + e.getIndex());
			}
		});
		dt.createThread().start();
	}
	
	private int current = 0;

	public void handleEvent(SimConnect sender, RecvEvent e) {
		EVENT_ID evid = EVENT_ID.values()[e.getEventID()];
        switch(evid)
        {
            case EVENT_SLIDER:
                System.out.println("Slider value: " + e.getData());
                break;
            case EVENT_XAXIS:
            	System.out.println("X Axis value:" + e.getData());
                break;
            case EVENT_YAXIS:
            	System.out.println("Y Axis value:" + e.getData());
                break;
            case EVENT_RZAXIS:
            	System.out.println("Rotate Z axis value:" + e.getData());
                break;
            case EVENT_HAT:
            	System.out.println("Hat value:" + e.getData());
                break;

            case EVENT_Z:
                current++;
                if (current == 6)
                    current = 1;
                switch( current )
                {
                case 1:
                	System.out.println("Slider is active");
                	try {
                		sender.setInputGroupState(INPUT_ID.INPUT_SLIDER, true);
                		sender.setInputGroupState(INPUT_ID.INPUT_XAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_YAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_RZAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_HAT, false);
                	} catch (IOException ioe){}
                    break;

                case 2:
                	System.out.println("X AXIS is active");
                	try {
                		sender.setInputGroupState(INPUT_ID.INPUT_SLIDER, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_XAXIS, true);
                		sender.setInputGroupState(INPUT_ID.INPUT_YAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_RZAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_HAT, false);
                	} catch (IOException ioe){}
                    break;

                case 3:
                	System.out.println("Y AXIS is active");
                	try {
                		sender.setInputGroupState(INPUT_ID.INPUT_SLIDER, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_XAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_YAXIS, true);
                		sender.setInputGroupState(INPUT_ID.INPUT_RZAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_HAT, false);
                	} catch (IOException ioe){}
                    break;
                case 4:
                	System.out.println("Z ROTATION is active");
                	try {
                		sender.setInputGroupState(INPUT_ID.INPUT_SLIDER, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_XAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_YAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_RZAXIS, true);
                		sender.setInputGroupState(INPUT_ID.INPUT_HAT, false);
                	} catch (IOException ioe){}
                    break;

                case 5:
                	System.out.println("HAT is active");
                	try {
                		sender.setInputGroupState(INPUT_ID.INPUT_SLIDER, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_XAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_YAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_RZAXIS, false);
                		sender.setInputGroupState(INPUT_ID.INPUT_HAT, true);
                	} catch (IOException ioe){}
                    break;
                }
                break;
        }
	}
	
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			JoystickInput ji = new JoystickInput();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ConfigurationNotFoundException e) {
			e.printStackTrace();
		}
	}
}
