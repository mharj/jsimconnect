package flightsim.simconnect.samples;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.recv.AbstractDispatcher;
import flightsim.simconnect.recv.RecvPacket;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.RecvWeatherObservation;

/**
 * A little app to show metar at current position
 * @author lc0277
 *
 */
@SuppressWarnings("serial")
public class ShowMetar extends JFrame {

	private final JTextArea text = new JTextArea("");
	
	public ShowMetar() {
		super("Metar");
		setSize(400, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel jp = new JPanel(new BorderLayout());
		jp.add(new JScrollPane(text), BorderLayout.CENTER);
		JButton but = new JButton("Refresh");
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}});
		jp.add(but, BorderLayout.SOUTH);
		getContentPane().add(jp);
		
	}
	
	void refresh() {
		
		try {
			final SimConnect sc = new SimConnect("BGLFileNumber", "10.1.0.6", 48447);
			sc.addToDataDefinition(1, "Plane longitude", "degrees", SimConnectDataType.FLOAT32);
			sc.addToDataDefinition(1, "Plane latitude", "degrees", SimConnectDataType.FLOAT32);
			sc.requestDataOnSimObject(1, 1, SimConnectConstants.OBJECT_ID_USER, SimConnectPeriod.ONCE);
			
			boolean cont = true;
			while (cont) {
				sc.callDispatch(new AbstractDispatcher() {

				@Override
				public void onDispatch(SimConnect simConnect, RecvPacket recv) {
					if (recv instanceof RecvOpen) {
						RecvOpen open = (RecvOpen) recv;
						text.append("\nConnected to " + open.toString());
					}
					
					if (recv instanceof RecvException) {
						RecvException exc = (RecvException) recv;
						text.append("\nError : " + exc.getException());
						try {
							sc.close();
						} catch (IOException e) {}
					}
					
					if (recv instanceof RecvSimObjectData) {
						RecvSimObjectData data = (RecvSimObjectData) recv;
						float longitude = data.getDataFloat32();
						float latitude = data.getDataFloat32();
						text.append("\nRequesting METAR at " + latitude + "," + longitude);
						try {
							sc.weatherRequestObservationAtNearestStation(1, latitude, longitude);
						} catch (IOException e) {}
		                
					}
					
					if (recv instanceof RecvWeatherObservation) {
						RecvWeatherObservation weather = (RecvWeatherObservation) recv;
						text.append("\nReceived weather: \n" + weather.getMetar() + "\n\n");
						try {
							sc.close();
						} catch (IOException e) {}
					}
					
				}});
				
				if (sc.isClosed()) cont = false;

			}
			
			
		} catch (Exception e) {
			text.setText(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		ShowMetar app = new ShowMetar();
		app.setVisible(true);
	}

}
