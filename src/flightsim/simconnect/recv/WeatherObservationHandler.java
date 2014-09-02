package flightsim.simconnect.recv;

import flightsim.simconnect.SimConnect;

public interface WeatherObservationHandler {
	public void handleWeatherObservation(SimConnect sender, RecvWeatherObservation e);

}
