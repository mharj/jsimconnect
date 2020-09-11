package flightsim.simconnect.recv;

import java.nio.ByteBuffer;

import flightsim.simconnect.WeatherMode;

public class RecvEventWeatherMode extends RecvEvent {

	private WeatherMode mode;
	
	RecvEventWeatherMode(ByteBuffer bf) {
		super(bf, RecvID.ID_EVENT_WEATHER_MODE);
		mode = WeatherMode.type(getData());
	}
	
	/**
	 * Returns the current weather mode
	 * @return weather mode
	 */
	public WeatherMode getWeatherMode() {
		return mode;
	}
	

}
