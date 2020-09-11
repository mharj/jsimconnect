package flightsim.simconnect.recv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import flightsim.simconnect.Dispatcher;
import flightsim.simconnect.SimConnect;

/**
 * A dispatcher that can run in its own thread and dispatch received messages into 
 * lists of listeners. There is a listener type for each received message type.
 * 
 * <p> It's not mandatory to use to run DispatcherTask in it's own thread,
 * just use the facility providen by the {@link Dispatcher} interface. <p>
 * 
 * <blockquote><pre>
 * class SimpleExample  {
 * 		SimpleExample() {
 *			// construct simconnect instance
 * 			SimConnect sc = new SimConnect(...);
 * 			DispatcherTask dt = new DispatcherTask(sc);
 * 			dt.addOpenHandler(new OpenHandler() {
 * 				public void handleOpen(SimConnect sender, RecvOpen open) {
 * 					// do something
 * 				}
 * 			});
 * 			dt.addSimObjectDDataHandler(new SimObjectDataHandler() {
 * 				public void handleSimObjectData(SimConnect sender, RecvSimObjectData data) {
 * 					// do something else
 * 				}
 * 			});
 * 			// spawn thread
 * 			new Thread(dt).start();
 * 		}
 * 			
 *      // ... class computation code
 * 
 * }
 * </pre></blockquote>
 * 
 * The constructor does not use simConnect instance at all, so
 * you can pass null here, for example if you don't want
 * to use the DispatcherTask in a Thread. 
 * 
 * <br/>
 * Since version 0.4 you can add/remove listeners from this class from within
 * the listener method without throwing ConccurentModificationException
 * 
 * @author lc0277
 *
 */
public class DispatcherTask implements Runnable, Dispatcher {
	protected final SimConnect sc;
	private boolean cont = true;
	
	private int nListeners = 0;		// statistics
	
	/**
	 * Returns the number of registered listeners
	 * @since 0.4
	 * @return number of listeners
	 */
	public int getListenersCount() {
		return nListeners;
	}
	
	/**
	 * Construct a dispatcher task. <code>sc</code> could be null if 
	 * not intended to run in it's own thread
	 * @param sc
	 */
	public DispatcherTask(SimConnect sc) {
		this.sc = sc;
	}
	
	/**
	 * The while(true) dispatch(); method. Could be used in this
	 * dispatcher own thread (if created with {@link #createThread()} or
	 * in your own threads.
	 */
	public void run() {
		cont = true;
		while (cont ) {
			try {
				sc.callDispatch(this);
			} catch (IOException e) {
				// probably a close
				cont = false;
			}
		}
	}
	
	/**
	 * This method will stop the dispatcher.
	 *
	 */
	public void tryStop() {
		cont = false;
	}
	
	/**
	 * Main dispatch method. Parse the received data specified in buffer and pass
	 * appropriate <code>Recv</code> structures to handlers added to this class.
	 */
	public void dispatch(SimConnect simConnect, ByteBuffer data) {
		// get the type of data
		int id = data.getInt(8);
		RecvID rid = RecvID.type(id);
		
		//
		// process add/removals
		//
		processQueuedListeners();
		
		switch (rid) {
		case ID_EVENT:
			if (EventHandlerList != null && EventHandlerList.size() > 0) {
				synchronized (EventHandlerList) {
					RecvEvent re = new RecvEvent(data);
					for (EventHandler ev : EventHandlerList) 
						ev.handleEvent(simConnect, re);
				}
			}
			break;
		case ID_EXCEPTION:
			if (ExceptionHandlerList != null && ExceptionHandlerList.size() > 0) {
				synchronized (ExceptionHandlerList) {
					RecvException rx = new RecvException(data);
					for (ExceptionHandler ev : ExceptionHandlerList) 
						ev.handleException(simConnect, rx);
				}
			}
			break;
		case ID_OPEN:
			if (OpenHandlerList != null && OpenHandlerList.size() > 0) {
				synchronized (OpenHandlerList) {
					RecvOpen ro = new RecvOpen(data);
					for (OpenHandler ev : OpenHandlerList) ev.handleOpen(simConnect, ro);
				}
			}
			break;
		case ID_EVENT_FILENAME:
			if (EventFilenameHandlerList != null && EventFilenameHandlerList.size() > 0) {
				synchronized (EventFilenameHandlerList) {
					RecvEventFilename ref = new RecvEventFilename(data);
					for (EventFilenameHandler ev : EventFilenameHandlerList) ev.handleFilename(simConnect, ref);
				}
			}
			break;
		case ID_CUSTOM_ACTION:
			if (CustomActionHandlerList != null && CustomActionHandlerList.size() > 0) {
				synchronized (CustomActionHandlerList) {
					RecvCustomAction rca = new RecvCustomAction(data);
					for (CustomActionHandler ev : CustomActionHandlerList) ev.handleCustomAction(simConnect, rca);
				}
			}
			break;
		case ID_EVENT_FRAME:
			if (EventFrameHandlerList != null && EventFrameHandlerList.size() > 0) {
				synchronized (EventFrameHandlerList) {
					RecvEventFrame rf = new RecvEventFrame(data);
					for (EventFrameHandler ev : EventFrameHandlerList) ev.handleEventFrame(simConnect, rf);
					
				}
			}
			break;
		case ID_SIMOBJECT_DATA:
			if (SimObjectDataHandlerList != null && SimObjectDataHandlerList.size() > 0) {
				synchronized (SimObjectDataHandlerList) {
					RecvSimObjectData rod = new RecvSimObjectData(data);
					for (SimObjectDataHandler ev : SimObjectDataHandlerList) {
						ev.handleSimObject(simConnect, rod);
						// reset bytebuffer read pointer
						rod.reset();
					}
				}
			}
			break;
		case ID_EVENT_OBJECT_ADDREMOVE:
			if (EventObjectHandlerList != null && EventObjectHandlerList.size() > 0) {
				synchronized (EventObjectHandlerList) {
					RecvEventAddRemove rear = new RecvEventAddRemove(data);
					for (EventObjectHandler ev : EventObjectHandlerList) ev.handleEventObject(simConnect, rear);
				}
			}
			break;
		case ID_SIMOBJECT_DATA_BYTYPE:
			if (SimObjectDataTypeHandlerList != null && SimObjectDataTypeHandlerList.size() > 0) {
				RecvSimObjectDataByType rot = new RecvSimObjectDataByType(data);
				for (SimObjectDataTypeHandler ev : SimObjectDataTypeHandlerList) {
					ev.handleSimObjectType(simConnect, rot);
					// reset bytebuffer read pointer
					rot.reset();
				}
			}
			break;
		case ID_QUIT:
			if (QuitHandlerList != null && QuitHandlerList.size() > 0) {
				RecvQuit rq = new RecvQuit(data);
				for (QuitHandler ev : QuitHandlerList) ev.handleQuit(simConnect, rq);
			}
			break;
		case ID_SYSTEM_STATE:
			if (SystemStateHandlerList != null && SystemStateHandlerList.size() > 0) {
				RecvSystemState ry = new RecvSystemState(data);
				for (SystemStateHandler ev : SystemStateHandlerList) ev.handleSystemState(simConnect, ry);
			}
			break;
		case ID_CLIENT_DATA:
			if (ClientDataHandlerList != null && ClientDataHandlerList.size() > 0) {
				RecvClientData rcd = new RecvClientData(data);
				for (ClientDataHandler ev : ClientDataHandlerList) {
					ev.handleClientData(simConnect, rcd);
					// reset bytebuffer read pointer
					rcd.reset();
				}
			}
			break;
		case ID_ASSIGNED_OBJECT_ID:
			if (AssignedObjectHandlerList != null && AssignedObjectHandlerList.size() > 0) {
				synchronized (AssignedObjectHandlerList) {
					RecvAssignedObjectID rai = new RecvAssignedObjectID(data);
					for (AssignedObjectHandler ev : AssignedObjectHandlerList) ev.handleAssignedObject(simConnect, rai);
				}
			}
			break;
		case ID_CLOUD_STATE:
			if (CloudStateHandlerList != null && CloudStateHandlerList.size() > 0) {
				synchronized (CloudStateHandlerList) {
					RecvCloudState rcl = new RecvCloudState(data);
					for (CloudStateHandler ev : CloudStateHandlerList) ev.handleCloudState(simConnect, rcl);
				}
			}
			break;
		case ID_RESERVED_KEY:
			if (ReservedKeyHandlerList != null && ReservedKeyHandlerList.size() > 0) {
				synchronized (ReservedKeyHandlerList) {
					RecvReservedKey rrk = new RecvReservedKey(data);
					for (ReservedKeyHandler ev : ReservedKeyHandlerList) ev.handleReservedKey(simConnect, rrk);
				}
			}
			break;
		case ID_WEATHER_OBSERVATION:
			if (WeatherObservationHandlerList != null && WeatherObservationHandlerList.size() > 0) {
				synchronized (WeatherObservationHandlerList) {
					RecvWeatherObservation rwo = new RecvWeatherObservation(data);
					for (WeatherObservationHandler ev : WeatherObservationHandlerList) ev.handleWeatherObservation(simConnect, rwo);
				}
			}
			break;
		case ID_EVENT_WEATHER_MODE:
			if (EventWeatherModeList != null && EventWeatherModeList.size() > 0) {
				synchronized (EventWeatherModeList) {
					RecvEventWeatherMode ev = new RecvEventWeatherMode(data);
					for (EventWeatherModeHandler hndler : EventWeatherModeList) {
						hndler.handleWeatherMode(simConnect, ev);
					}
				}
			}
			break;
		case ID_AIRPORT_LIST:
			if (FacilitiesListHandlerList != null && FacilitiesListHandlerList.size() > 0) {
				synchronized (FacilitiesListHandlerList) {
					RecvAirportList list = new RecvAirportList(data);
					for (FacilitiesListHandler hndle : FacilitiesListHandlerList) {
						hndle.handleAirportList(simConnect, list);
					}
				}
			}
			break;
		case ID_VOR_LIST:
			if (FacilitiesListHandlerList != null && FacilitiesListHandlerList.size() > 0) {
				synchronized (FacilitiesListHandlerList) {
					RecvVORList list = new RecvVORList(data);
					for (FacilitiesListHandler hndle : FacilitiesListHandlerList) {
						hndle.handleVORList(simConnect, list);
					}
				}
			}
			break;
		case ID_NDB_LIST:
			if (FacilitiesListHandlerList != null && FacilitiesListHandlerList.size() > 0) {
				synchronized (FacilitiesListHandlerList) {
					RecvNDBList list = new RecvNDBList(data);
					for (FacilitiesListHandler hndle : FacilitiesListHandlerList) {
						hndle.handleNDBList(simConnect, list);
					}
				}
			}
			break;
			
			/* 0.5 (SP1) */
		case ID_WAYPOINT_LIST:
			if (FacilitiesListHandlerList != null && FacilitiesListHandlerList.size() > 0) {
				synchronized (FacilitiesListHandlerList) {
					RecvWaypointList list = new RecvWaypointList(data);
					for (FacilitiesListHandler hndle : FacilitiesListHandlerList) {
						hndle.handleWaypointList(simConnect, list);
					}
				}
			}
			break;
			
			/* 0.7 (SP2) */
		case ID_EVENT_MULTIPLAYER_CLIENT_STARTED:
			if (MultiplayerClientStartedHandlerList != null && MultiplayerClientStartedHandlerList.size() > 0) {
				synchronized (MultiplayerClientStartedHandlerList) {
					RecvEventMultiplayerClientStarted ev = new RecvEventMultiplayerClientStarted(data);
					for (MultiplayerClientStartedHandler hndle : MultiplayerClientStartedHandlerList) {
						hndle.handleMultiplayerClientStarted(simConnect, ev);
					}
				}
			}
			break;

		case ID_EVENT_MULTIPLAYER_SERVER_STARTED:
			if (MultiplayerServerStartedHandlerList != null && MultiplayerServerStartedHandlerList.size() > 0) {
				synchronized (MultiplayerServerStartedHandlerList) {
					RecvEventMultiplayerServerStarted ev = new RecvEventMultiplayerServerStarted(data);
					for (MultiplayerServerStartedHandler hndle : MultiplayerServerStartedHandlerList) {
						hndle.handleMultiplayerServerStarted(simConnect, ev);
					}
				}
			}
			break;

		case ID_EVENT_MULTIPLAYER_SESSION_ENDED:
			if (MultiplayerSessionEndedHandlerList != null && MultiplayerSessionEndedHandlerList.size() > 0) {
				synchronized (MultiplayerSessionEndedHandlerList) {
					RecvEventMultiplayerSessionEnded ev = new RecvEventMultiplayerSessionEnded(data);
					for (MultiplayerSessionEndedHandler hndle : MultiplayerSessionEndedHandlerList) {
						hndle.handleMultiplayerSessionEnded(simConnect, ev);
					}
				}
			}
			break;

		case ID_EVENT_RACE_END:
			if (RaceEndHandlerList != null && RaceEndHandlerList.size() > 0) {
				synchronized (RaceEndHandlerList) {
					RecvEventRaceEnd ev = new RecvEventRaceEnd(data);
					for (RaceEndHandler hndle : RaceEndHandlerList) {
						hndle.handleRaceEnd(simConnect, ev);
					}
				}
			}
			break;

		case ID_EVENT_RACE_LAP:
			if (RaceLapHandlerList != null && RaceLapHandlerList.size() > 0) {
				synchronized (RaceLapHandlerList) {
					RecvEventRaceLap ev = new RecvEventRaceLap(data);
					for (RaceLapHandler hndle : RaceLapHandlerList) {
						hndle.handleRaceLap(simConnect, ev);
					}
				}
			}
			break;

			
		case ID_NULL:
		default:
		}

		//
		// process add/removals
		//
		processQueuedListeners();

	}
	
	private List<AssignedObjectHandler> AssignedObjectHandlerList;
	private List<ClientDataHandler> ClientDataHandlerList;
	private List<CloudStateHandler> CloudStateHandlerList;
	private List<CustomActionHandler> CustomActionHandlerList;
	private List<EventFilenameHandler> EventFilenameHandlerList;
	private List<EventFrameHandler> EventFrameHandlerList;
	private List<EventHandler> EventHandlerList;
	private List<EventObjectHandler> EventObjectHandlerList;
	private List<ExceptionHandler> ExceptionHandlerList;
	private List<OpenHandler> OpenHandlerList;
	private List<QuitHandler> QuitHandlerList;
	private List<ReservedKeyHandler> ReservedKeyHandlerList;
	private List<SimObjectDataHandler> SimObjectDataHandlerList;
	private List<SimObjectDataTypeHandler> SimObjectDataTypeHandlerList;
	private List<SystemStateHandler> SystemStateHandlerList;
	private List<WeatherObservationHandler> WeatherObservationHandlerList;
	private List<EventWeatherModeHandler> EventWeatherModeList;
	private List<FacilitiesListHandler> FacilitiesListHandlerList;
	private List<MultiplayerClientStartedHandler> MultiplayerClientStartedHandlerList;
	private List<MultiplayerServerStartedHandler> MultiplayerServerStartedHandlerList;
	private List<MultiplayerSessionEndedHandler> MultiplayerSessionEndedHandlerList;
	private List<RaceEndHandler> RaceEndHandlerList;
	private List<RaceLapHandler> RaceLapHandlerList;

	
	public void addAssignedObjectHandler(AssignedObjectHandler ev) {
		if (AssignedObjectHandlerList == null)
			AssignedObjectHandlerList = new ArrayList<AssignedObjectHandler>();
		queueds.add(new LateAdd<AssignedObjectHandler>(AssignedObjectHandlerList, ev));
	}

	public void removeAssignedObjectHandler(AssignedObjectHandler ev) {
		queueds.add(new LateRemoval<AssignedObjectHandler>(AssignedObjectHandlerList, ev));
	}

	public void addClientDataHandler(ClientDataHandler ev) {
		if (ClientDataHandlerList == null)
			ClientDataHandlerList = new ArrayList<ClientDataHandler>();
		queueds.add(new LateAdd<ClientDataHandler>(ClientDataHandlerList, ev));
	}

	public void removeClientDataHandler(ClientDataHandler ev) {
		queueds.add(new LateRemoval<ClientDataHandler>(ClientDataHandlerList, ev));
	}

	public void addCloudStateHandler(CloudStateHandler ev) {
		if (CloudStateHandlerList == null)
			CloudStateHandlerList = new ArrayList<CloudStateHandler>();
		queueds.add(new LateAdd<CloudStateHandler>(CloudStateHandlerList, ev));
	}

	public void removeCloudStateHandler(CloudStateHandler ev) {
		queueds.add(new LateRemoval<CloudStateHandler>(CloudStateHandlerList, ev));
	}

	public void addCustomActionHandler(CustomActionHandler ev) {
		if (CustomActionHandlerList == null)
			CustomActionHandlerList = new ArrayList<CustomActionHandler>();
		queueds.add(new LateAdd<CustomActionHandler>(CustomActionHandlerList, ev));
	}

	public void removeCustomActionHandler(CustomActionHandler ev) {
		queueds.add(new LateRemoval<CustomActionHandler>(CustomActionHandlerList, ev));
	}

	public void addEventFilenameHandler(EventFilenameHandler ev) {
		if (EventFilenameHandlerList == null)
			EventFilenameHandlerList = new ArrayList<EventFilenameHandler>();
		queueds.add(new LateAdd<EventFilenameHandler>(EventFilenameHandlerList, ev));
	}

	public void removeEventFilenameHandler(EventFilenameHandler ev) {
		queueds.add(new LateRemoval<EventFilenameHandler>(EventFilenameHandlerList, ev));
	}

	public void addEventFrameHandler(EventFrameHandler ev) {
		if (EventFrameHandlerList == null)
			EventFrameHandlerList = new ArrayList<EventFrameHandler>();
		queueds.add(new LateAdd<EventFrameHandler>(EventFrameHandlerList, ev));
	}

	public void removeEventFrameHandler(EventFrameHandler ev) {
		queueds.add(new LateRemoval<EventFrameHandler>(EventFrameHandlerList, ev));
	}

	public void addEventHandler(EventHandler ev) {
		if (EventHandlerList == null)
			EventHandlerList = new ArrayList<EventHandler>();
		queueds.add(new LateAdd<EventHandler>(EventHandlerList, ev));
	}

	public void removeEventHandler(EventHandler ev) {
		queueds.add(new LateRemoval<EventHandler>(EventHandlerList, ev));
	}

	public void addEventObjectHandler(EventObjectHandler ev) {
		if (EventObjectHandlerList == null)
			EventObjectHandlerList = new ArrayList<EventObjectHandler>();
		queueds.add(new LateAdd<EventObjectHandler>(EventObjectHandlerList, ev));
	}

	public void removeEventObjectHandler(EventObjectHandler ev) {
		queueds.add(new LateRemoval<EventObjectHandler>(EventObjectHandlerList, ev));
	}

	public void addExceptionHandler(ExceptionHandler ev) {
		if (ExceptionHandlerList == null)
			ExceptionHandlerList = new ArrayList<ExceptionHandler>();
		queueds.add(new LateAdd<ExceptionHandler>(ExceptionHandlerList, ev));
	}

	public void removeExceptionHandler(ExceptionHandler ev) {
		queueds.add(new LateRemoval<ExceptionHandler>(ExceptionHandlerList, ev));
	}

	public void addOpenHandler(OpenHandler ev) {
		if (OpenHandlerList == null)
			OpenHandlerList = new ArrayList<OpenHandler>();
		queueds.add(new LateAdd<OpenHandler>(OpenHandlerList, ev));
	}

	public void removeOpenHandler(OpenHandler ev) {
		queueds.add(new LateRemoval<OpenHandler>(OpenHandlerList, ev));
	}

	public void addQuitHandler(QuitHandler ev) {
		if (QuitHandlerList == null)
			QuitHandlerList = new ArrayList<QuitHandler>();
		queueds.add(new LateAdd<QuitHandler>(QuitHandlerList, ev));
	}

	public void removeQuitHandler(QuitHandler ev) {
		queueds.add(new LateRemoval<QuitHandler>(QuitHandlerList, ev));
	}

	public void addReservedKeyHandler(ReservedKeyHandler ev) {
		if (ReservedKeyHandlerList == null)
			ReservedKeyHandlerList = new ArrayList<ReservedKeyHandler>();
		queueds.add(new LateAdd<ReservedKeyHandler>(ReservedKeyHandlerList, ev));
	}

	public void removeReservedKeyHandler(ReservedKeyHandler ev) {
		queueds.add(new LateRemoval<ReservedKeyHandler>(ReservedKeyHandlerList, ev));
	}

	public void addSimObjectDataHandler(SimObjectDataHandler ev) {
		if (SimObjectDataHandlerList == null)
			SimObjectDataHandlerList = new ArrayList<SimObjectDataHandler>();
		queueds.add(new LateAdd<SimObjectDataHandler>(SimObjectDataHandlerList, ev));
	}

	public void removeSimObjectDataHandler(SimObjectDataHandler ev) {
		queueds.add(new LateRemoval<SimObjectDataHandler>(SimObjectDataHandlerList, ev));
	}

	public void addSimObjectDataTypeHandler(SimObjectDataTypeHandler ev) {
		if (SimObjectDataTypeHandlerList == null)
			SimObjectDataTypeHandlerList = new ArrayList<SimObjectDataTypeHandler>();
		queueds.add(new LateAdd<SimObjectDataTypeHandler>(SimObjectDataTypeHandlerList, ev));
	}

	public void removeSimObjectDataTypeHandler(SimObjectDataTypeHandler ev) {
		queueds.add(new LateRemoval<SimObjectDataTypeHandler>(SimObjectDataTypeHandlerList, ev));
	}

	public void addSystemStateHandler(SystemStateHandler ev) {
		if (SystemStateHandlerList == null)
			SystemStateHandlerList = new ArrayList<SystemStateHandler>();
		queueds.add(new LateAdd<SystemStateHandler>(SystemStateHandlerList, ev));
	}

	public void removeSystemStateHandler(SystemStateHandler ev) {
		queueds.add(new LateRemoval<SystemStateHandler>(SystemStateHandlerList, ev));
	}

	public void addWeatherObservationHandler(WeatherObservationHandler ev) {
		if (WeatherObservationHandlerList == null)
			WeatherObservationHandlerList = new ArrayList<WeatherObservationHandler>();
		queueds.add(new LateAdd<WeatherObservationHandler>(WeatherObservationHandlerList, ev));
	}

	public void removeWeatherObservationHandler(WeatherObservationHandler ev) {
		queueds.add(new LateRemoval<WeatherObservationHandler>(WeatherObservationHandlerList, ev));
	}
	
	public void addEventWeatherModeHandler(EventWeatherModeHandler ev) {
		if (EventWeatherModeList == null)
			EventWeatherModeList = new ArrayList<EventWeatherModeHandler>();
		queueds.add(new LateAdd<EventWeatherModeHandler>(EventWeatherModeList, ev));
	}

	public void removeEventWeatherModeHandler(EventWeatherModeHandler ev) {
		queueds.add(new LateRemoval<EventWeatherModeHandler>(EventWeatherModeList, ev));
	}
	
	public void addFacilitiesListHandler(FacilitiesListHandler ev) {
		if (FacilitiesListHandlerList == null)
			FacilitiesListHandlerList = new ArrayList<FacilitiesListHandler>();
		queueds.add(new LateAdd<FacilitiesListHandler>(FacilitiesListHandlerList, ev));
	}

	public void removeFacilitiesListHandler(FacilitiesListHandler ev) {
		queueds.add(new LateRemoval<FacilitiesListHandler>(FacilitiesListHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void addMultiplayerClientStartedHandler(MultiplayerClientStartedHandler ev) {
		if (MultiplayerClientStartedHandlerList == null) 
			MultiplayerClientStartedHandlerList = new ArrayList<MultiplayerClientStartedHandler>();
		queueds.add(new LateAdd<MultiplayerClientStartedHandler>(MultiplayerClientStartedHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void removeMultiplayerClientStartedHandler(MultiplayerClientStartedHandler ev) {
		queueds.add(new LateRemoval<MultiplayerClientStartedHandler>(MultiplayerClientStartedHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void addMultiplayerServerStartedHandler(MultiplayerServerStartedHandler ev) {
		if (MultiplayerServerStartedHandlerList == null) 
			MultiplayerServerStartedHandlerList = new ArrayList<MultiplayerServerStartedHandler>();
		queueds.add(new LateAdd<MultiplayerServerStartedHandler>(MultiplayerServerStartedHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void removeMultiplayerServerStartedHandler(MultiplayerServerStartedHandler ev) {
		queueds.add(new LateRemoval<MultiplayerServerStartedHandler>(MultiplayerServerStartedHandlerList, ev));
	}


	/**
	 * @since 0.7
	 */
	public void addMultiplayerSessionEndedHandler(MultiplayerSessionEndedHandler ev) {
		if (MultiplayerSessionEndedHandlerList == null) 
			MultiplayerSessionEndedHandlerList = new ArrayList<MultiplayerSessionEndedHandler>();
		queueds.add(new LateAdd<MultiplayerSessionEndedHandler>(MultiplayerSessionEndedHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void removeMultiplayerSessionEndedHandler(MultiplayerSessionEndedHandler ev) {
		queueds.add(new LateRemoval<MultiplayerSessionEndedHandler>(MultiplayerSessionEndedHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void addRaceLapHandler(RaceLapHandler ev) {
		if (RaceLapHandlerList == null) 
			RaceLapHandlerList = new ArrayList<RaceLapHandler>();
		queueds.add(new LateAdd<RaceLapHandler>(RaceLapHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void removeRaceLapHandler(RaceLapHandler ev) {
		queueds.add(new LateRemoval<RaceLapHandler>(RaceLapHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void addRaceEndHandler(RaceEndHandler ev) {
		if (RaceEndHandlerList == null) 
			RaceEndHandlerList = new ArrayList<RaceEndHandler>();
		queueds.add(new LateAdd<RaceEndHandler>(RaceEndHandlerList, ev));
	}

	/**
	 * @since 0.7
	 */
	public void removeRaceEndHandler(RaceEndHandler ev) {
		queueds.add(new LateRemoval<RaceEndHandler>(RaceEndHandlerList, ev));
	}


	

	/**
	 * returns a fresh thread (not started) for running
	 * this dispatcher.
	 * only a convenience method. Do not call twice !
	 * @return a new thread
	 */
	public Thread createThread() {
		Thread t = new Thread(this);
		t.setName("SimConnect dispatcher thread"); //$NON-NLS-1$
		return t;
	}
	
	/*
	 * This wrapper stuff will queue all listener add/removal at the end
	 * of the event received loop to avoid conccurent modifications
	 * 
	 */
	
	private abstract class LateProcessItem<T> {
		protected List<T> l;
		protected T item;
		
		protected LateProcessItem(List<T> l, T item) {
			this.l = l;
			this.item = item;
		}
		
		protected abstract void doJob();
	}
	
	private class LateRemoval<T> extends LateProcessItem<T> {
		protected LateRemoval(List<T> l, T item) {
			super(l, item);
		}

		@Override
		protected void doJob() {
			if (l != null) {
				synchronized (l) {
					if (l.remove(item))
						nListeners--;
				}
			}
		}
	}

	private class LateAdd<T> extends LateProcessItem<T> {
		protected LateAdd(List<T> l, T item) {
			super(l, item);
		}

		@Override
		protected void doJob() {
			if (l != null) {
				synchronized (l) {
					l.add(item);
					nListeners++;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Queue<LateProcessItem> queueds = new LinkedList<LateProcessItem>();
	
	@SuppressWarnings("unchecked")
	protected synchronized void processQueuedListeners() {
		while (!queueds.isEmpty()) {
			LateProcessItem ri = queueds.poll();
			if (ri != null)
				ri.doJob();
		}
	}
	
	
	/**
	 * Remove the specified object to all listeners list that corresponds
	 * to an interface implemented by the specified object or its
	 * superclasses.
	 * @since 0.5
	 * @param o
	 */
	public void removeHandlers(Object o) {
		if (o instanceof AssignedObjectHandler) {
			removeAssignedObjectHandler((AssignedObjectHandler) o);
		}
		if (o instanceof ClientDataHandler) {
			removeClientDataHandler((ClientDataHandler) o);
		}
		if (o instanceof CloudStateHandler) {
			removeCloudStateHandler((CloudStateHandler) o);
		}
		if (o instanceof CustomActionHandler) {
			removeCustomActionHandler((CustomActionHandler) o);
		}
		if (o instanceof EventFilenameHandler) {
			removeEventFilenameHandler((EventFilenameHandler) o);
		}
		if (o instanceof EventFrameHandler) {
			removeEventFrameHandler((EventFrameHandler) o);
		}
		if (o instanceof EventHandler) {
			removeEventHandler((EventHandler) o);
		}
		if (o instanceof EventObjectHandler) {
			removeEventObjectHandler((EventObjectHandler) o);
		}
		if (o instanceof EventWeatherModeHandler) {
			removeEventWeatherModeHandler((EventWeatherModeHandler) o);
		}
		if (o instanceof ExceptionHandler) {
			removeExceptionHandler((ExceptionHandler) o);
		}
		if (o instanceof OpenHandler) {
			removeOpenHandler((OpenHandler) o);
		}
		if (o instanceof QuitHandler) {
			removeQuitHandler((QuitHandler) o);
		}
		if (o instanceof ReservedKeyHandler) {
			removeReservedKeyHandler((ReservedKeyHandler) o);
		}
		if (o instanceof SimObjectDataHandler) {
			removeSimObjectDataHandler((SimObjectDataHandler) o);
		}
		if (o instanceof SimObjectDataTypeHandler) {
			removeSimObjectDataTypeHandler((SimObjectDataTypeHandler) o);
		}
		if (o instanceof SystemStateHandler) {
			removeSystemStateHandler((SystemStateHandler) o);
		}
		if (o instanceof WeatherObservationHandler) {
			removeWeatherObservationHandler((WeatherObservationHandler) o);
		}
		/* 0.5 (SP1) */
		if (o instanceof FacilitiesListHandler) {
			removeFacilitiesListHandler((FacilitiesListHandler) o);
		}
		/* 0.7 (SP2) */
		if (o instanceof MultiplayerClientStartedHandler) {
			removeMultiplayerClientStartedHandler((MultiplayerClientStartedHandler) o);
		}
		if (o instanceof MultiplayerServerStartedHandler) {
			removeMultiplayerServerStartedHandler((MultiplayerServerStartedHandler) o);
		}
		if (o instanceof MultiplayerSessionEndedHandler) {
			removeMultiplayerSessionEndedHandler((MultiplayerSessionEndedHandler) o);
		}
		if (o instanceof RaceLapHandler) {
			removeRaceLapHandler((RaceLapHandler) o);
		}
		if (o instanceof RaceEndHandler) {
			removeRaceEndHandler((RaceEndHandler) o);
		}
	}

	/**
	 * Add the specified object to all listeners list that corresponds
	 * to an interface implemented by the specified object or its
	 * superclasses.
	 * @since 0.5
	 * @param o
	 */
	public void addHandlers(Object o) {
		if (o instanceof AssignedObjectHandler) {
			addAssignedObjectHandler((AssignedObjectHandler) o);
		}
		if (o instanceof ClientDataHandler) {
			addClientDataHandler((ClientDataHandler) o);
		}
		if (o instanceof CloudStateHandler) {
			addCloudStateHandler((CloudStateHandler) o);
		}
		if (o instanceof CustomActionHandler) {
			addCustomActionHandler((CustomActionHandler) o);
		}
		if (o instanceof EventFilenameHandler) {
			addEventFilenameHandler((EventFilenameHandler) o);
		}
		if (o instanceof EventFrameHandler) {
			addEventFrameHandler((EventFrameHandler) o);
		}
		if (o instanceof EventHandler) {
			addEventHandler((EventHandler) o);
		}
		if (o instanceof EventObjectHandler) {
			addEventObjectHandler((EventObjectHandler) o);
		}
		if (o instanceof EventWeatherModeHandler) {
			addEventWeatherModeHandler((EventWeatherModeHandler) o);
		}
		if (o instanceof ExceptionHandler) {
			addExceptionHandler((ExceptionHandler) o);
		}
		if (o instanceof OpenHandler) {
			addOpenHandler((OpenHandler) o);
		}
		if (o instanceof QuitHandler) {
			addQuitHandler((QuitHandler) o);
		}
		if (o instanceof ReservedKeyHandler) {
			addReservedKeyHandler((ReservedKeyHandler) o);
		}
		if (o instanceof SimObjectDataHandler) {
			addSimObjectDataHandler((SimObjectDataHandler) o);
		}
		if (o instanceof SimObjectDataTypeHandler) {
			addSimObjectDataTypeHandler((SimObjectDataTypeHandler) o);
		}
		if (o instanceof SystemStateHandler) {
			addSystemStateHandler((SystemStateHandler) o);
		}
		if (o instanceof WeatherObservationHandler) {
			addWeatherObservationHandler((WeatherObservationHandler) o);
		}
		/* 0.5 (SP1) */
		if (o instanceof FacilitiesListHandler) {
			addFacilitiesListHandler((FacilitiesListHandler) o);
		}
		/* 0.7 (SP2) */
		if (o instanceof MultiplayerClientStartedHandler) {
			removeMultiplayerClientStartedHandler((MultiplayerClientStartedHandler) o);
		}
		if (o instanceof MultiplayerServerStartedHandler) {
			removeMultiplayerServerStartedHandler((MultiplayerServerStartedHandler) o);
		}
		if (o instanceof MultiplayerSessionEndedHandler) {
			removeMultiplayerSessionEndedHandler((MultiplayerSessionEndedHandler) o);
		}
		if (o instanceof RaceEndHandler) {
			removeRaceEndHandler((RaceEndHandler) o);
		}
		if (o instanceof RaceLapHandler) {
			removeRaceLapHandler((RaceLapHandler) o);
		}

	}

}
