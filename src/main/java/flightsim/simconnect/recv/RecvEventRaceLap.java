package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.wrappers.GUID;

/**
 * The RecvEventRaceLap structure is used in multi-player racing to hold the results for one player at the
 *  end of a lap.
 *  
 * @since 0.7
 * @author lc0277
 *
 */
public class RecvEventRaceLap extends RecvEvent {


	private int lapIndex;  // The index of the lap the results are for
	private int numberRacers;   // The total number of racers
	private GUID missionGUID;                      // The name of the mission to execute, NULL if no mission
	private String playerName;       // The name of the player
	private String sessionType;      // The type of the multiplayer session: "LAN", "GAMESPY")
	private String aircraft;         // The aircraft type 
	private String playerRole;       // The player role in the mission
	private double totalTime;                              // Total time in seconds, 0 means DNF
	private double penaltyTime;                            // Total penalty time in seconds
	private boolean disqualified;        // non 0 - disqualified, 0 - not disqualified

	RecvEventRaceLap(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_RACE_LAP);
		lapIndex = bf.getInt();
		numberRacers = bf.getInt();
		missionGUID = new GUID();
		missionGUID.read(bf);
		playerName = makeString(bf, 260);
		sessionType = makeString(bf, 260);
		aircraft = makeString(bf, 260);
		playerRole = makeString(bf, 260);
		totalTime = bf.getDouble();
		penaltyTime = bf.getDouble();
		disqualified = bf.getInt() == 1;
	}
	
	/**
	 * Returns  The aircraft type
	 * @return  The aircraft type
	 */
	public String getAircraft() {
		return aircraft;
	}

	/**
	 * Returns true if player was disqualified
	 * @return true if player was disqualified
	 */
	public boolean isDisqualified() {
		return disqualified;
	}

	/**
	 * The name of the mission to execute, NULL guid if no mission (null guid is
	 * probably blank, all-zeros)
	 * @return The name of the mission to execute
	 */
	public GUID getMissionGUID() {
		return missionGUID;
	}

	/**
	 * Returns the total number of racers
	 * @return the total number of racers
	 */
	public int getNumberRacers() {
		return numberRacers;
	}

	/**
	 * Total penalty time in seconds
	 * @return Total penalty time in seconds
	 */
	public double getPenaltyTime() {
		return penaltyTime;
	}

	/**
	 *  The name of the player
	 * @return  The name of the player
	 */
	public String getPlayerName() {
		return playerName;
	}
	
	/**
	 * The player role in the mission
	 * @return The player role in the mission
	 */
	public String getPlayerRole() {
		return playerRole;
	}

	/**
	 * The index of the laps the results are for. Laps are indexed from 0
	 * @return The index of the Laps the results are for
	 */
	public int getLapIndex() {
		return lapIndex;
	}

	/**
	 * The type of the multiplayer session: "LAN", "GAMESPY")
	 * @return The type of the multiplayer session: "LAN", "GAMESPY")
	 */
	public String getSessionType() {
		return sessionType;
	}

	/**
	 * Total time in seconds, 0 means DNF
	 * @return Total time in seconds, 0 means DNF
	 */
	public double getTotalTime() {
		return totalTime;
	}


}
