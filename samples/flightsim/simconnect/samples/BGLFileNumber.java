package flightsim.simconnect.samples;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimConnectPeriod;
import flightsim.simconnect.recv.AbstractDispatcher;
import flightsim.simconnect.recv.RecvPacket;
import flightsim.simconnect.recv.RecvException;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectData;

/**
 * Guess file name of BGL scenery actually shown in FS.
 * Calculation code courtesy of Dick Ludowise
 * @author lc0277
 *
 */
@SuppressWarnings("serial")
public class BGLFileNumber extends JFrame {
	private final JLabel remote = new JLabel();
	private final JLabel pos = new JLabel();
	public BGLFileNumber() {
		super("BGL File Number");
		setSize(600, 100);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		remote.setText("Not connected");
		pos.setText("");
		JPanel jp = new JPanel(new FlowLayout());
		jp.add(remote); jp.add(pos);
		JButton but = new JButton("Refresh");
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}});
		jp.add(but);
		getContentPane().add(jp);
		
	}
	
	void refresh() {
		
		try {
			final SimConnect sc = new SimConnect("BGLFileNumber", "10.1.0.6", 48447);
			sc.addToDataDefinition(1, "Plane longitude", "degrees", SimConnectDataType.FLOAT64);
			sc.addToDataDefinition(1, "Plane latitude", "degrees", SimConnectDataType.FLOAT64);
			sc.requestDataOnSimObject(1, 1, SimConnectConstants.OBJECT_ID_USER, SimConnectPeriod.ONCE);
			
			boolean cont = true;
			while (cont) {
				sc.callDispatch(new AbstractDispatcher() {

				@Override
				public void onDispatch(SimConnect simConnect, RecvPacket recv) {
					if (recv instanceof RecvOpen) {
						RecvOpen open = (RecvOpen) recv;
						System.out.println("\nConnected to " + open.toString());
						System.out.println(open.getApplicationName().length());
						remote.setText(open.toString());
					}
					
					if (recv instanceof RecvException) {
						RecvException exc = (RecvException) recv;
						System.err.println("\nReceived error: " + exc.getException());
						remote.setText("Remote error: " + exc.getException());
						pos.setText("");
						try {
							sc.close();
						} catch (IOException e) {}
					}
					
					if (recv instanceof RecvSimObjectData) {
						RecvSimObjectData data = (RecvSimObjectData) recv;
						double longitude = data.getDataFloat64();
						double latitude = data.getDataFloat64();
						System.out.println("Current position " + latitude + ", " + longitude);
						String dir1Text, dir2Text, file1Text, file2Text;
						
			            int dir1 = ((int)(((180.0 + (longitude)) * 12) / 360.0));
		                if (dir1 < 10) dir1Text = "0" + dir1;
		                else dir1Text = "" + dir1;
		                
		                int dir2 = ((int)(((90.0 - (latitude)) * 8) / 180.0));
		                if (dir2 < 10) dir2Text = "0" + dir2;
		                else dir2Text = "" + dir2;
		                
		                int file1 = ((int)(((180.0 + (longitude)) * 96) / 360.0));
		                if (file1 < 10) file1Text = "0" + file1;
		                else file1Text = "" + file1;
		                int file2 = ((int)(((90.0 - (latitude)) * 64) / 180.0));
		                if (file2 < 10) file2Text = "0" + file2;
		                else file2Text = "" + file2;
		                
		                System.out.println("File is : 'Scenery/" + dir1Text + dir2Text + "/*X" + file1Text + file2Text + ".bgl'");
		                pos.setText("Dir: " + dir1Text + dir2Text + " File: " + file1Text + file2Text);
		                
						try {
							sc.close();
						} catch (IOException e) {}
					}
					
				}});
				
				if (sc.isClosed()) cont = false;

			}
			
			
		} catch (Exception e) {
			remote.setText("Cannot connect: " + e.getMessage());
			pos.setText("");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		BGLFileNumber bfn = new BGLFileNumber();
		bfn.setVisible(true);
	}
}
