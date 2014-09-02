package flightsim.simconnect.samples;

import flightsim.simconnect.FacilityListType;
import flightsim.simconnect.NotificationPriority;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.TextResult;
import flightsim.simconnect.TextType;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.FacilitiesListHandler;
import flightsim.simconnect.recv.FacilityAirport;
import flightsim.simconnect.recv.FacilityNDB;
import flightsim.simconnect.recv.FacilityVOR;
import flightsim.simconnect.recv.FacilityWaypoint;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.QuitHandler;
import flightsim.simconnect.recv.RecvAirportList;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvNDBList;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvQuit;
import flightsim.simconnect.recv.RecvVORList;
import flightsim.simconnect.recv.RecvWaypointList;

/**
 * SimConnect FaciliitesData sample
 * 
 *      Description:
 *                              Ctrl F1 displays the Get Facilities menu on the screen
 *                              Ctrl F2 displays the Subscribe to Faciliites menu on the screen
 *
 */
public class FacilitiesData implements OpenHandler, EventHandler, FacilitiesListHandler, QuitHandler {
	static enum GROUP_ID 
	{
	    GROUP0,
	};

	static enum EVENT_ID 
	{
	    EVENT0,
	    EVENT1,
	    
	    EVENT_MENU_1,
	    EVENT_MENU_2,
	};

	static enum INPUT_ID 
	{
	    INPUT0,
	};

	static enum REQUEST_ID 
	{
	    REQUEST_0,
	    REQUEST_1
	};

	static boolean quit = false;
	
	private static final String[] GET_FACILITIES_MENU_OPTIONS = {
        "Get airport facilities",
        "Get waypoints",
        "Get NDB",
        "Get VOR",
        "Close menu",
	};
	private static final String[] SUBSCRIBE_FACILITIES_MENU_OPTIONS = {
        "Subscribe to airports",
        "Subscribe to waypoints",
        "Subscribe to NDB",
        "Subscribe to VOR",
        "Unsubscribe to airports",
        "Unsubscribe to waypoints",
        "Unsubscribe to NDB",
        "Unsubscribe to VOR",
        "Close menu",
	};

	
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
		
		System.out.println("Event id=" + eventId + " data=" +e.getData());
		try {
			if (eventId == EVENT_ID.EVENT0.ordinal()) {
				// Display menu
				sender.menu(0.0f, EVENT_ID.EVENT_MENU_1,
						"SimConnect Facilities Test", 
						"Choose which item:",
						GET_FACILITIES_MENU_OPTIONS);
			} else if (eventId == EVENT_ID.EVENT1.ordinal()) {
				// Display menu
				sender.menu(0.0f, EVENT_ID.EVENT_MENU_1,
						"SimConnect Facilities Test", 
						"Choose which item:",
						SUBSCRIBE_FACILITIES_MENU_OPTIONS);
			} else if (eventId == EVENT_ID.EVENT_MENU_1.ordinal()) {
				TextResult result = TextResult.type(e.getData());
				System.out.println("\tMenu result=" + result);
				int item = result.value() - TextResult.MENU_SELECT_1.value();
				if (item < FacilityListType.COUNT.ordinal()) {
				    // Get the current cached list of airports, waypoints, etc, as the item indicates
					sender.requestFacilitiesList(FacilityListType.values()[item], REQUEST_ID.REQUEST_0);
				}
			} else if (eventId == EVENT_ID.EVENT_MENU_2.ordinal()) {
				TextResult result = TextResult.type(e.getData());
				System.out.println("\tMenu result=" + result);
				int item = result.value() - TextResult.MENU_SELECT_1.value();
				if (item < FacilityListType.COUNT.ordinal()) {
					sender.subscribeToFacilities(FacilityListType.values()[item], REQUEST_ID.REQUEST_1);
                }
                else if (FacilityListType.COUNT.ordinal() <= item && item < 2 * FacilityListType.COUNT.ordinal()) {
                	sender.unSubscribeToFacilities(FacilityListType.values()[item - FacilityListType.COUNT.ordinal()]);
				}
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

	public void handleAirportList(SimConnect sender, RecvAirportList list) {
		System.out.println("Facilities list size=" + list.getArraySize() + " " + (list.getEntryNumber()+1) + "/" +
				list.getOutOf());
		System.out.println("Airports");
		for (FacilityAirport f : list.getFacilities()) {
			System.out.println("\t" + f.toString());
		}
		System.out.println();
	}
	
	public void handleNDBList(SimConnect sender, RecvNDBList list) {
		System.out.println("Facilities list size=" + list.getArraySize() + " " + (list.getEntryNumber()+1) + "/" +
				list.getOutOf());
		System.out.println("Airports");
		for (FacilityNDB f : list.getFacilities()) {
			System.out.println("\t" + f.toString());
		}
		System.out.println();
	}
	
	public void handleVORList(SimConnect sender, RecvVORList list) {
		System.out.println("Facilities list size=" + list.getArraySize() + " " + (list.getEntryNumber()+1) + "/" +
				list.getOutOf());
		System.out.println("Airports");
		for (FacilityVOR f : list.getFacilities()) {
			System.out.println("\t" + f.toString());
		}
		System.out.println();
	}
	
	public void handleWaypointList(SimConnect sender, RecvWaypointList list) {
		System.out.println("Facilities list size=" + list.getArraySize() + " " + (list.getEntryNumber()+1) + "/" +
				list.getOutOf());
		System.out.println("Airports");
		for (FacilityWaypoint f : list.getFacilities()) {
			System.out.println("\t" + f.toString());
		}
		System.out.println();
	}

	public static void main(String[] args) {
		try {
			
			SimConnect simconnect = new SimConnect("SimConnect Text", 0);
			System.out.println("Connected to Flight Simulator");
			simconnect.mapClientEventToSimEvent(EVENT_ID.EVENT0);
			simconnect.mapClientEventToSimEvent(EVENT_ID.EVENT1);
			simconnect.addClientEventToNotificationGroup(GROUP_ID.GROUP0, EVENT_ID.EVENT0, true);
			simconnect.addClientEventToNotificationGroup(GROUP_ID.GROUP0, EVENT_ID.EVENT1, true);
			simconnect.setNotificationGroupPriority(GROUP_ID.GROUP0, NotificationPriority.HIGHEST);
			simconnect.mapInputEventToClientEvent(INPUT_ID.INPUT0, "Ctrl+F1", EVENT_ID.EVENT0);
			simconnect.mapInputEventToClientEvent(INPUT_ID.INPUT0, "Ctrl+F2", EVENT_ID.EVENT1);
			simconnect.setInputGroupState(INPUT_ID.INPUT0, true);
			
			simconnect.text(TextType.PRINT_RED, 15, 3, "Facilities Data");
			simconnect.text(TextType.PRINT_RED, 15, 3, "Press Ctrl-F1 for Get Facilities, Ctrl-F2 for Subscribe to Facilities");
			
			FacilitiesData fd = new FacilitiesData();
			
			DispatcherTask dt = new DispatcherTask(simconnect);
			/*
			dt.addEventHandler(tm);
			dt.addOpenHandler(tm);
			dt.addExceptionHandler(tm);
			dt.addQuitHandler(tm);
			*/
			// equivalent
			dt.addHandlers(fd);
			
			while (!quit) {
				simconnect.callDispatch(dt);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	
	


	
}
