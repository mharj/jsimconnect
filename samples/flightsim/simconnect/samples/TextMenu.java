package flightsim.simconnect.samples;

import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.TextResult;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.ExceptionHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.QuitHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvQuit;

/**
 * SimConnect Text Menu sample
 * Description:
 * 				Ctrl F1 displays a menu on the screen
 * 				Ctrl F2 removes the menu from the screen
 * 				Selecting any menu option sends an event and removes the menu
 * 
 * @since 0.5
 * @author lc0277
 *
 */
public class TextMenu implements OpenHandler,EventHandler, QuitHandler, ExceptionHandler {

	static enum GROUP_ID {
	    GROUP0,
	};

	static enum EVENT_ID {
	    EVENT1,
		EVENT2,
	    EVENT_MENU_1,
	};

	static enum INPUT_ID {
	    INPUT0,
	};
	
	boolean quit = false;

	private static final String MENU_TEXT = "SimConnect Text Menu";
	private static final String MENU_PROMPT = "Choose which item:";
	private static final String[] MENU_ITEMS = {
		"Item #0",
		"Item #1",
		"Item #2",
		"Item #3",
		"Item #4",
		"Item #5",
	};

	String menuText(TextResult result) {
		switch(result)
	    {
	    case MENU_SELECT_1:
	        return "Item #1 Selected";
	    case MENU_SELECT_2:
	        return "Item #2 Selected";
	    case MENU_SELECT_3:
	        return "Item #3 Selected";
	    case MENU_SELECT_4:
	        return "Item #4 Selected";
	    case MENU_SELECT_5:
	        return "Item #5 Selected";
	    case MENU_SELECT_6:
	        return "Item #6 Selected";
	    case MENU_SELECT_7:
	        return "Item #7 Selected";
	    case MENU_SELECT_8:
	        return "Item #8 Selected";
	    case MENU_SELECT_9:
	        return "Item #9 Selected";
	    case MENU_SELECT_10:
	        return "Item #10 Selected";
	    case DISPLAYED:
	        return "Displayed";
	    case QUEUED:
	        return "Queued";
	    case REMOVED:
	        return "Removed from Queue";
	    case REPLACED:
	        return "Replaced in Queue";
	    case TIMEOUT:
	        return "Timeout";
	    default:
	        return "<unknown>";
	    }
	}
	
	public void handleOpen(SimConnect sender, RecvOpen e) {
		System.out.println("Open AppName=\"" +
				e.getApplicationName() + "\" AppVersion=" +
				e.getApplicationVersionMajor() + "." +
				e.getApplicationVersionMinor() + "." + 
				e.getApplicationBuildMajor() + "." +
				e.getApplicationBuildMinor() +  " SimConnectVersion=" +
				e.getSimConnectVersionMajor() + "." +
				e.getSimConnectVersionMinor() + "." +
				e.getSimConnectBuildMajor() + "." +
				e.getSimConnectBuildMinor());
	}
	
	public void handleEvent(SimConnect sender, RecvEvent e) {
		int eventId = e.getEventID();
		
		try {
			if (eventId == EVENT_ID.EVENT1.ordinal()) {
				// Display menu
				sender.menu(0.0f, EVENT_ID.EVENT_MENU_1, 
						MENU_TEXT, MENU_PROMPT, MENU_ITEMS);
			} else if (eventId == EVENT_ID.EVENT2.ordinal()) {
				// Stop displaying menu
				sender.menu(0.0f, EVENT_ID.EVENT_MENU_1, 
						null, null, (String[]) null);
			} else if (eventId == EVENT_ID.EVENT_MENU_1.ordinal()) {
				System.out.println();
				TextResult result = TextResult.type(e.getData());
				System.out.println(menuText(result));
			} else {
				System.out.println("SIMCONNECT_RECV_EVENT: " +
						"0x" + Integer.toHexString(e.getEventID()) + " " +
						"0x" + Integer.toHexString(e.getData()));
			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void handleException(SimConnect sender, RecvException e) {
		System.out.println("\nEXCEPTION=" + 
				e.getException() + " SendID=" + e.getSendID() + 
				" Offset=" + e.getIndex());
	}
	
	public void handleQuit(SimConnect sender, RecvQuit e) {
		System.out.println("\n***** SIMCONNECT_RECV_ID_QUIT *****");
		quit = true;
	}
	
	public static void main(String[] args) {
		try {
			
			SimConnect simconnect = new SimConnect("SimConnect Text", 0);
			System.out.println("Connected to Flight Simulator");
			simconnect.mapClientEventToSimEvent(EVENT_ID.EVENT1);
			simconnect.mapClientEventToSimEvent(EVENT_ID.EVENT2);
			simconnect.addClientEventToNotificationGroup(GROUP_ID.GROUP0, EVENT_ID.EVENT1, true);
			simconnect.addClientEventToNotificationGroup(GROUP_ID.GROUP0, EVENT_ID.EVENT2, true);
			simconnect.setNotificationGroupPriority(GROUP_ID.GROUP0, NotificationPriority.HIGHEST);
			simconnect.mapInputEventToClientEvent(INPUT_ID.INPUT0, "Ctrl+F1", EVENT_ID.EVENT1);
			simconnect.mapInputEventToClientEvent(INPUT_ID.INPUT0, "Ctrl+F2", EVENT_ID.EVENT2);
			simconnect.setInputGroupState(INPUT_ID.INPUT0, true);
			
			TextMenu tm = new TextMenu();
			
			DispatcherTask dt = new DispatcherTask(simconnect);
			/*
			dt.addEventHandler(tm);
			dt.addOpenHandler(tm);
			dt.addExceptionHandler(tm);
			dt.addQuitHandler(tm);
			*/
			dt.addHandlers(tm);
			
			while (!tm.quit) {
				simconnect.callDispatch(dt);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

}
