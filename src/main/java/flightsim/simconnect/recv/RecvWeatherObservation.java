package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.SimConnect;

/**
 * The <code>RecvWeatherObservation</code> structure is used to 
 * return weather observation data, after calls to one of:
 * {@link SimConnect#weatherRequestObservationAtNearestStation(int, float, float)} 
 * {@link SimConnect#weatherRequestObservationAtStation(int, String)} 
 * {@link SimConnect#weatherRequestInterpolatedObservation(int, float, float, float)} 
 * 
 * @author lc0277
 *
 */
public class RecvWeatherObservation extends RecvPacket {

	private final int requestID;
	private final String metar;
	
	
	RecvWeatherObservation(ByteBuffer bf) {
		super(bf, RecvID.ID_WEATHER_OBSERVATION);
		requestID = bf.getInt();
		metar = super.makeString(bf, bf.remaining());
	}
	
	/**
	 * Returns The ID of the client defined request.
	 * @return request id
	 */
	public int getRequestID() {
		return requestID;
	}
	
	/**
	 * Returns a string containing the Metar weather data. The maximum length of this string is 2000 chars.
	 * @return metar
	 */
	public String getMetar() {
		return metar;
	}
	
	
}
