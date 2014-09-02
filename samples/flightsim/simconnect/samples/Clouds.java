package flightsim.simconnect.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.data.LatLonAlt;
import flightsim.simconnect.recv.CloudStateHandler;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.RecvCloudState;
import flightsim.simconnect.recv.RecvSimObjectData;
import flightsim.simconnect.recv.SimObjectDataHandler;

/**
 * A mini window showing cloud data. 
 * @author lc0277
 *
 */
@SuppressWarnings("serial")
public class Clouds extends JFrame implements KeyListener {
	private final WeatherRadar wr;
	private float range = 0.4f;
	private float rangeAlt = 1000f;
	
	
	float getRange() {
		return range;
	}
	
	float getRangeAlt() {
		return rangeAlt;
	}
	
	public Clouds() {
		super("Clouds");
		setSize(500, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel jp = new JPanel(new BorderLayout());
		wr = new WeatherRadar();
		jp.add(wr, BorderLayout.CENTER);
		getContentPane().add(jp);
		
		addKeyListener(this);
		
		try {
			SimConnect sc = new SimConnect("Weather", "10.1.0.6", 48447);
			sc.addToDataDefinition(1, "STRUCT LATLONALT", null, SimConnectDataType.LATLONALT);
			sc.requestDataOnSimObject(3, 1, SimConnectConstants.OBJECT_ID_USER, SimConnectPeriod.SECOND);
			
			DispatcherTask dt = new DispatcherTask(sc);
			dt.addSimObjectDataHandler(new SimObjectDataHandler(){

				public void handleSimObject(SimConnect sender, RecvSimObjectData e) {
					LatLonAlt l = e.getLatLonAlt();
					l.altitude *= 3.2808;	// convert to feet
					float r = getRange();
					float ra = getRangeAlt();
					l.altitude = 4711;
					try {
						sender.weatherRequestCloudState(1,
								(float) l.latitude - r, 
								(float) l.longitude -r, 
								(float) l.altitude - ra, 
								(float) l.latitude +r, 
								(float) l.longitude +r, 
								(float) l.altitude+ra);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}});
			dt.addCloudStateHandler(new CloudStateHandler() {
				public void handleCloudState(SimConnect sender, RecvCloudState e) {
					wr.setData(e.getRequestID(), e.getData());
					repaint();
				}});
			new Thread(dt).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Clouds app = new Clouds();
		app.setVisible(true);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_A) {
			range += 0.005;
		}
		if (e.getKeyCode() == KeyEvent.VK_Z) {
			range -= 0.005;
		}
		if (e.getKeyCode() == KeyEvent.VK_Q) {
			rangeAlt += 100;
		}
		if (e.getKeyCode() == KeyEvent.VK_S) {
			rangeAlt -= 100;
		}
		wr.setInfo("Range: " + range + " Alt " + rangeAlt);
		wr.repaint();
	
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
}

@SuppressWarnings("serial")
class WeatherRadar extends JComponent {
	private byte[][] data = new byte[64][64];
	private String info = "";
	
	void setData(int req, byte[][] b) {
		this.data = b;
	}
	
	void setInfo(String i) {
		info = i;
	}
	
	private static Color[] COLORS;
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(5*64, 5*64);
	}
	
	static {
		COLORS = new Color[256];
		for (int i = 0; i < 256; i++) {
			
			COLORS[i] = new Color(i, i, i);
		}
	}
	
	@Override
	protected void paintComponent(Graphics g1) {
		int w = getWidth() / 64;		// size of ONE square
		int h = getHeight() / 64;
		Graphics2D g = (Graphics2D) g1;
		
		if (data != null) {
			for (int i = 0; i < 64; i++) {
				for (int j = 0; j < 64; j++) {
					int col = (data[i][j] & 0xff);
					if (col > 255) col = 255;
					g.setColor(COLORS[col]);
					g.fillRect(j*w, getHeight() - i*h, w, h);
					if (col != 0) {
						System.out.println("GOT a non null at " + i+ " " + j + " = " + col);
					}
				}
			}
		}
		g.setColor(Color.green);
		g.setFont(Font.decode("PLAIN-14"));
		g.drawString(info, 0, 15);		
	}
}