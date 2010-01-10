/* @author Ed Sells 27 July 2007
 * 
 * Dialogue for fast head positioning (maintenance/research scenarios)
 * 
 * Status: Working on it. Appologies for bosnian code - 
 * one of my first ever coding attempts. Advice welcome!
 * en0es@bath.ac.uk 
 * 
 * Code borrowed from org.reprap.gui.steppertest.Main v818
 * 
 */

package org.reprap.gui.steppertest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.comms.snap.SNAPCommunicator;
import org.reprap.devices.SNAPStepperMotor;

import java.awt.Color;

public class MaintenancePositionsFrame  extends JFrame {
	private static final long serialVersionUID = 1L;
	// Panel globals
	private JLabel warning;
	private JLabel area;
	private JLabel status;
	private final int fastSpeed = 245;
	
	// 'Talk to bot' globals

	private final int localNodeNumber = 0;
	Communicator communicator = org.reprap.Main.getCommunicator();
	
	// Operation globals
	SNAPStepperMotor motorX, motorY;
	boolean homePositionAlreadyFound = false;
	
	
	
	public MaintenancePositionsFrame()
	{
		/*
		 * TODO: Trying to recognise null/cartesian status: Won't work?
		 * 
		 * //Establish connection type
		String connection = "simulator";
		try {
		connection = Preferences.loadGlobalString("RepRap_Machine");
		}
		
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Can't establish 'Geometry parameter'" + e);
			return;
		}
		
		//Initialise comms with bot
		if (connection == "SNAPRepRap"){*/
			try { 
				talkToBot();
			}
			catch (Exception e){
				JOptionPane.showMessageDialog(null, "Can't talk to bot: " + e);
				return;
			}
		//}
		
		//Establish motors
		try { 
			motorX = new SNAPStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("XAxisAddress")),  1);
			motorY = new SNAPStepperMotor(communicator, new SNAPAddress(Preferences.loadGlobalInt("YAxisAddress")),  2);
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't initialise motors" + e);
			return;
		}
		
		//Build frame
		setTitle("Maintenance positions");
		warning = new JLabel("WARNING: Use Working Volume Probe to establish WorkingAxis(mm) preferences first.");
		area = new JLabel("Finding working area...");
		status = new JLabel("To activate the sector buttons, click home...");
		status.setForeground(Color.red);

		
		JPanel text = new JPanel();
		text.setLayout(new GridLayout(3,1));	
		text.add(warning);
		text.add(area);
		text.add(status);
		
		getContentPane().add(text, BorderLayout.NORTH);
		
		//Add grid of buttons for sector positioning of head
		try { 
			addSectorGrid();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(null, "Couldn't add controls" + e);
			return;
		}
		
		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
	
	public void talkToBot() throws Exception {
		
		SNAPAddress myAddress = new SNAPAddress(localNodeNumber);
		
		String port = Preferences.loadGlobalString("Port(name)");
		String err = "";
		
		try {
			communicator = new SNAPCommunicator(port, myAddress);
		}
		catch (gnu.io.NoSuchPortException e)
		{
			err = "There was an error opening " + port + ".\n\n";
			err += "Check to make sure that is the right path.\n";
			err += "Check that you have your serial connector plugged in.";
			
			throw new Exception(err);
		}
		catch (gnu.io.PortInUseException e)
		{
			err = "The " + port + " port is already in use by another program.";
			
			throw new Exception(err);
		}
	}
	
	private void addSectorGrid() throws Exception {
		
		double stepsPerMMX = Preferences.loadGlobalDouble("XAxisScale(steps/mm)");
		double axisLengthX = Preferences.loadGlobalDouble("WorkingX(mm)");
		int maxStepsX = (int)Math.round(stepsPerMMX*axisLengthX);
		
		double stepsPerMMY = Preferences.loadGlobalDouble("YAxisScale(steps/mm)");
		double axisLengthY = Preferences.loadGlobalDouble("WorkingY(mm)");
		int maxStepsY = (int)Math.round(stepsPerMMY*axisLengthY);
		
		area.setText("Current working area = " + axisLengthX + " mm x " + axisLengthY + " mm ("
				+ maxStepsX + " steps x " + maxStepsY + " steps)");
		area.repaint();
		
		JPanel sectorXY = new JPanel();
		sectorXY.setLayout(new GridLayout(3,3));		
		
		JButton topLeft = makeSectorXYButton(0, maxStepsY, 0, axisLengthY, "", false);
		JButton topMiddle = makeSectorXYButton(maxStepsX/2, maxStepsY, axisLengthX/2, axisLengthY, "^", false);
		JButton topRight = makeSectorXYButton(maxStepsX, maxStepsY, axisLengthX, axisLengthY, "", false);
		JButton middleLeft = makeSectorXYButton(0, maxStepsY/2, 0, axisLengthY/2, "<", true);
		JButton middle = makeSectorXYButton(maxStepsX/2, maxStepsY/2, axisLengthX/2, axisLengthY/2, "X", true);
		JButton middleRight = makeSectorXYButton(maxStepsX, maxStepsY/2, axisLengthX, axisLengthY/2, ">", false);
		JButton home = homeButton();
		JButton bottomMiddle= makeSectorXYButton(maxStepsX/2, 0, axisLengthX/2, 0, "V", true);
		JButton bottomRight = makeSectorXYButton(maxStepsX, 0, axisLengthX, 0, "", false);
		 
		sectorXY.add(topLeft);
		sectorXY.add(topMiddle);
		sectorXY.add(topRight);
		sectorXY.add(middleLeft);
		sectorXY.add(middle);
		sectorXY.add(middleRight);
		sectorXY.add(home);
		sectorXY.add(bottomMiddle);
		sectorXY.add(bottomRight);
		
		getContentPane().add(sectorXY, BorderLayout.SOUTH);
		
	}

	private JButton makeSectorXYButton(int xSteps, int ySteps, double xmm, double ymm, String pointer, boolean activate)
	{
		final int xCoord = xSteps;
		final int yCoord = ySteps;
		final double xMM = xmm;
		final double yMM = ymm;
		final boolean active = activate;
		
		final JButton sector = new JButton(pointer);
		sector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (active)
				{
					if (homePositionAlreadyFound) 
					{
						status.setText("Travelling...");
						status.repaint();
						try {
								motorX.seek(fastSpeed, xCoord);
								motorY.seek(fastSpeed, yCoord);
	
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(null, "Could not position motor: " + ex);
						}
						
						status.setText("Head position: " + xMM
									+ ", " + yMM); 
						status.repaint();
					}	
					else {
						status.setText("Find the home position before sectors can activate."); 
						status.repaint();
					}
				}
				else
				{
					status.setText("Button has already been DEACTIVATED for safety. Reactivate in source if happy with working volume."); 
					status.repaint();
				}
		
			}
		});
		
		return sector;
	}

	private JButton homeButton()
	{
		JButton home = new JButton("Home");
		home.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				status.setText("Homing... ");
				status.repaint();
				try {
					motorX.homeReset(fastSpeed);
					motorY.homeReset(fastSpeed);
					homePositionAlreadyFound = true;
				} 
				catch (Exception ex) {
					homePositionAlreadyFound = false;
					JOptionPane.showMessageDialog(null, "Could not home motor: " + ex);
				}
				if (homePositionAlreadyFound = true)
				{
				status.setText("Axes homed. You may now click any sector button...");
				status.repaint();
				}
			}
		});
		return home;
	}
}