package org.reprap.gui.extruderprofile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.devices.SNAPExtruder;
import org.reprap.Extruder;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class Main extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;
	private final static int stablePeriod = 60000;
	
	private JLabel jLabel1;
	private JButton startButton;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JTextField elapsedTime;
	private JTextField currentTemp;
	private JTextField powerSetting;
	private JLabel jLabel3;
	private JTextField maxTemp;

//	private final int localNodeNumber = 0;
	
	private Thread pollThread = null;
	private boolean pollThreadExiting = false;
	
	private Communicator communicator =  org.reprap.Main.getCommunicator();;
	private Extruder extruder;

	private boolean profileInProgress = false;
	private int currentHeaterOutput = -1;
	private double previousTemperature;
	private long lastChangeTime = 0;
	private long segmentStartTime = 0;
	
	private double ambientTemperature;
	
	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) throws Exception {
		JFrame frame = new JFrame();
		Main inst = new Main(frame);
		inst.setVisible(true);
	}
	
	public Main(JFrame frame) throws Exception {
		super(frame);
		
//		SNAPAddress myAddress = new SNAPAddress(localNodeNumber); 
		this.setResizable(false);
//		communicator = new SNAPCommunicator(Preferences.loadGlobalString("Port(name)"),
//				myAddress);

		extruder = new SNAPExtruder(communicator,
				new SNAPAddress(Preferences.loadGlobalString("Extruder0_Address")), 0, null);
		
		initGUI();
		
		if (!extruder.isAvailable()) {
			startButton.setEnabled(false);
			return;
		}
		
		pollThread = new Thread() {
			public void run() {
				Thread.currentThread().setName("GUI Poll");
				while(!pollThreadExiting) {
					try {
						Thread.sleep(500);
						RefreshTemperature();
					}
					catch (InterruptedException ex) {
						// This is normal when shutting down, so ignore
					}
					catch (Exception ex) {
						System.err.println("Exception during profiler temperature update: " + ex.getMessage());
					}
				}
			}
		};
		pollThread.start();
	}
	
	protected void RefreshTemperature() throws IOException {
		int temperature = (int)Math.round(extruder.getTemperature());
		currentTemp.setText(Integer.toString(temperature));
		
		// Decide if we should set a new heater output
		if (currentHeaterOutput != -1 && !profileInProgress) {
			// Cancel
			extruder.setHeater(0, 0);
			return;
		}
		
		if (!profileInProgress)
			return;

		if (currentHeaterOutput == -1) {
			// Set first level
			ambientTemperature = temperature;
			System.out.println("Heater output 0, ambient temp is " + ambientTemperature + "C");
			currentHeaterOutput = 15;
			powerSetting.setText(String.valueOf(currentHeaterOutput));
			extruder.setHeater(currentHeaterOutput, Integer.parseInt(maxTemp.getText()));
			previousTemperature = temperature;
			segmentStartTime = lastChangeTime = System.currentTimeMillis();
			return;
		}
		
		if (temperature > Integer.parseInt(maxTemp.getText())) {
			System.out.println("Reached max temperature -- aborted");
			startButtonActionPerformed(null);
			return;
		}
			
		
		// If temperature max stable for period, bump up level
		// (ie ignore small decreases)
		if (temperature > previousTemperature) {
			lastChangeTime = System.currentTimeMillis();
			previousTemperature = temperature;
		} else {
			long now = System.currentTimeMillis();
			long elapsed = now - lastChangeTime; 
			elapsedTime.setText(String.valueOf(elapsed));
			if (elapsed > stablePeriod) {
				long duration = now - segmentStartTime;
				System.out.println("Heater output " + currentHeaterOutput + " is " +
						previousTemperature + "C after " + duration + "ms");
				currentHeaterOutput += 16;
				if (currentHeaterOutput > 255) {
					startButtonActionPerformed(null);
				} else {
					powerSetting.setText(String.valueOf(currentHeaterOutput));
					extruder.setHeater(currentHeaterOutput, Integer.parseInt(maxTemp.getText()));
					previousTemperature = temperature;
					segmentStartTime = lastChangeTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	public void dispose() {
		if (pollThread != null) {
			pollThreadExiting = true;
			pollThread.interrupt();
		}
		extruder.dispose();
//		communicator.dispose();
		super.dispose();
	}
	
	private void initGUI() {
		try {
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1);
				jLabel1.setText("Max Temperature");
				jLabel1.setBounds(-7, 35, 133, 28);
				jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				maxTemp = new JTextField();
				getContentPane().add(maxTemp);
				maxTemp.setText("150");
				maxTemp.setBounds(147, 35, 63, 28);
			}
			{
				startButton = new JButton();
				getContentPane().add(startButton);
				startButton.setText("Profile...");
				startButton.setBounds(147, 77, 105, 28);
				startButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						startButtonActionPerformed(evt);
					}
				});
			}
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2);
				jLabel2.setText("Power setting");
				jLabel2.setBounds(7, 126, 119, 28);
				jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3);
				jLabel3.setText("Current temp");
				jLabel3.setBounds(7, 154, 119, 28);
				jLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4);
				jLabel4.setText("Elapsed Time");
				jLabel4.setBounds(7, 182, 119, 28);
				jLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				powerSetting = new JTextField();
				getContentPane().add(powerSetting);
				powerSetting.setBounds(147, 126, 77, 28);
				powerSetting.setEditable(false);
			}
			{
				currentTemp = new JTextField();
				getContentPane().add(currentTemp);
				currentTemp.setBounds(147, 154, 77, 28);
				currentTemp.setEditable(false);
			}
			{
				elapsedTime = new JTextField();
				getContentPane().add(elapsedTime);
				elapsedTime.setBounds(147, 182, 77, 28);
				elapsedTime.setEditable(false);
			}
			{
				this.setTitle("Extruder Heat Profiler");
				getContentPane().setLayout(null);
				this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
			this.setSize(288, 277);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void startButtonActionPerformed(ActionEvent evt) {
		if (profileInProgress) {
			startButton.setText("Start...");
			profileInProgress = false;
		} else {
			startButton.setText("Cancel");
			profileInProgress = true;
		}
	}

}
