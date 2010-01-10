package org.reprap.gui.extrudertest;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import javax.swing.JCheckBox;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.reprap.Preferences;
import org.reprap.comms.Communicator;
import org.reprap.comms.snap.SNAPAddress;
import org.reprap.devices.SNAPExtruder;
import org.reprap.Extruder;
import org.reprap.gui.Utility;

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
	private JLabel jLabel1;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JLabel jLabel5;	
	private JCheckBox reverseCheckbox;
	private JCheckBox coolerActive;
	private JCheckBox materialEmpty;
	private JButton extrudeButton;
	private JLabel jLabel6;
	private JLabel jLabel7;	
	private JSlider extruderSpeed;
	private JCheckBox heaterActive;
	private JTextField currentTemperature;
	private JLabel jLabel3;
	private JTextField desiredTemperature;
	private JTextField desiredExtruder;
	
	private Communicator communicator = org.reprap.Main.getCommunicator();;
	private Extruder extruders[];
	private int extruderCount;
	private int extruder;
	
	private Thread pollThread = null;
	private boolean pollThreadExiting = false;

	private boolean extruding = false;
	
	private boolean reverse = false;
	
	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Extruder Exerciser");
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
		
		extruderCount = Preferences.loadGlobalInt("NumberOfExtruders");
		extruders = new SNAPExtruder[extruderCount];
		if (extruderCount < 1)
			throw new Exception("A Reprap printer must contain at least one extruder");
		
		for(int i = 0; i < extruderCount; i++)
		{
			String prefix = "Extruder" + i + "_";
			extruders[i] = new SNAPExtruder(communicator,
				new SNAPAddress(Preferences.loadGlobalInt(prefix + "Address")), 
				 i, null);
		}
		
		extruder = -1;
		
		for(int i = 0; i < extruderCount; i++)
		{
			if (extruders[i].isAvailable())
			{
				extruder = i;
				break;
			}
		}
		
		if(extruder < 0)
		{
			System.err.println("No extruders available!");
			System.exit(0);
		}

		initGUI();
		
		extruderSpeed.setMinimum(0);
		extruderSpeed.setMaximum(255);
		extruderSpeed.setValue(200);
		extruderSpeed.setMajorTickSpacing(64);
		extruderSpeed.setMinorTickSpacing(16);

		pollThread = new Thread() {
			public void run() {
				Thread.currentThread().setName("GUI Poll");
				while(!pollThreadExiting) {
					try {
						Thread.sleep(500);
						RefreshTemperature();
						RefreshMaterialSensor();
					}
					catch (InterruptedException ex) {
						// This is normal when shutting down, so ignore
					}
				}
			}
		};
		pollThread.start();

	}
	
	protected void RefreshTemperature() {
		int temperature = (int)Math.round(extruders[extruder].getTemperature());
		currentTemperature.setText(Integer.toString(temperature));
	}
	
	protected void RefreshMaterialSensor() {
		boolean empty = extruders[extruder].isEmpty();
		materialEmpty.setSelected(empty);
	}

	public void dispose() {
		if (pollThread != null) {
			pollThreadExiting = true;
			pollThread.interrupt();
		}
		extruders[extruder].dispose();
//		communicator.dispose();
		super.dispose();
	}
	
	private void initGUI() {
		try {
			{
				jLabel6 = new JLabel();
				getContentPane().add(jLabel6);
				jLabel6.setText("Extruder");
				jLabel6.setHorizontalAlignment(SwingConstants.RIGHT);
				jLabel6.setBounds(5, 14, 100, 28);
			}
			
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3);
				jLabel3.setText("Desired temperature");
				jLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
				jLabel3.setBounds(35, 133, 140, 28);
			}
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1);
				jLabel1.setText("Current temperature");
				jLabel1.setBounds(35, 98, 140, 28);
				jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				desiredTemperature = new JTextField();
				getContentPane().add(desiredTemperature);
				desiredTemperature.setText("0");
				desiredTemperature.setBounds(182, 133, 63, 28);
				desiredTemperature.setHorizontalAlignment(SwingConstants.RIGHT);
				desiredTemperature.setEnabled(false);
				desiredTemperature.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						desiredTemperatureActionPerformed(evt);
					}
				});
			}
			{
				desiredExtruder = new JTextField();
				getContentPane().add(desiredExtruder);
				desiredExtruder.setText(Integer.valueOf(extruder).toString());
				desiredExtruder.setBounds(110, 14, 30, 28);
				desiredExtruder.setHorizontalAlignment(SwingConstants.RIGHT);
				desiredExtruder.setEnabled(true);
				desiredExtruder.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						desiredExtruderActionPerformed(evt);
					}
				});
			}
			{
				jLabel7 = new JLabel();
				getContentPane().add(jLabel7);
				jLabel7.setText(extruders[extruder].toString());
				jLabel7.setHorizontalAlignment(SwingConstants.RIGHT);
				jLabel7.setBounds(40, 42, 100, 28);
			}
			
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2);
				jLabel2.setText("deg C");
				jLabel2.setBounds(252, 133, 63, 28);
			}
			{
				currentTemperature = new JTextField();
				getContentPane().add(currentTemperature);
				currentTemperature.setHorizontalAlignment(SwingConstants.RIGHT);
				currentTemperature.setText("");
				currentTemperature.setEditable(false);
				currentTemperature.setBounds(182, 98, 63, 28);
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4);
				jLabel4.setText("deg C");
				jLabel4.setBounds(252, 98, 63, 28);
			}
			{
				heaterActive = new JCheckBox();
				getContentPane().add(heaterActive);
				heaterActive.setText("Heater Active");
				heaterActive.setBounds(182, 35, 126, 21);
				heaterActive.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						heaterActiveActionPerformed(evt);
					}
				});
			}
			{
				extruderSpeed = new JSlider();
				getContentPane().add(extruderSpeed);
				extruderSpeed.setBounds(147, 168, 238, 28);
				extruderSpeed.setPaintTicks(true);
				extruderSpeed.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent evt) {
						extruderSpeedStateChanged(evt);
					}
				});
			}
			{
				coolerActive = new JCheckBox();
				getContentPane().add(coolerActive);
				coolerActive.setText("Cooler Active");
				coolerActive.setBounds(182, 56, 126, 21);
				coolerActive.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						coolerActiveActionPerformed(evt);
					}
				});
			}
			{
				jLabel5 = new JLabel();
				getContentPane().add(jLabel5);
				jLabel5.setText("Extruder speed: " + extruderSpeed.getValue());
				jLabel5.setBounds(7, 168, 140, 28);
				jLabel5.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				extrudeButton = new JButton();
				getContentPane().add(extrudeButton);
				extrudeButton.setText("Extrude");
				extrudeButton.setBounds(154, 203, 105, 28);
				extrudeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						extrudeButtonActionPerformed(evt);
					}
				});
			}
			{
				materialEmpty = new JCheckBox();
				getContentPane().add(materialEmpty);
				materialEmpty.setText("Feedstock empty");
				materialEmpty.setBounds(182, 14, 175, 21);
			}
			{
				reverseCheckbox = new JCheckBox();
				getContentPane().add(reverseCheckbox);
				reverseCheckbox.setText("Reverse");
				reverseCheckbox.setBounds(273, 203, 91, 28);
				reverseCheckbox.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent evt) {
						reverseCheckboxStateChanged(evt);
					}
				});
			}
			{
				getContentPane().setLayout(null);
				this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.setTitle("Extruder Exerciser");
			}
			this.setSize(400, 263);
		} catch (Exception e) {
			e.printStackTrace();
		}
        Utility.centerWindowOnScreen(this);

	}
	
	protected void coolerActiveActionPerformed(ActionEvent evt) {
		try {
			extruders[extruder].setCooler(coolerActive.isSelected());
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Exception setting cooler: " + ex);
			ex.printStackTrace();
		}
	}

	private void desiredTemperatureActionPerformed(ActionEvent evt) {
		setTemperature();
	}
	
	private void desiredExtruderActionPerformed(ActionEvent evt) {
		setExtruder();
	}
	
	private void heaterActiveActionPerformed(ActionEvent evt) {
		desiredTemperature.setEnabled(heaterActive.isSelected());
		setTemperature();
	}

	private void setTemperature() {
		try {
			if (heaterActive.isSelected())
				extruders[extruder].setTemperature(Integer.parseInt(desiredTemperature.getText()), false);
			else
				extruders[extruder].setTemperature(0, false);
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Exception setting temperature: " + ex);
			ex.printStackTrace();
		}
	}
	
	private void setExtruder() {
		try {
				extruder= Integer.parseInt(desiredExtruder.getText());
				if(extruder < 0)
					extruder = 0;
				if(extruder >= extruderCount)
					extruder = extruderCount - 1;
				desiredExtruder.setText(Integer.valueOf(extruder).toString());
				jLabel7.setText(extruders[extruder].toString());
				if (!extruders[extruder].isAvailable()) {
					extrudeButton.setEnabled(false);
					extruderSpeed.setEnabled(false);
					currentTemperature.setEnabled(false);
					desiredTemperature.setEnabled(false);
					heaterActive.setEnabled(false);
					materialEmpty.setEnabled(false);
					coolerActive.setEnabled(false);
				} else
				{
					extrudeButton.setEnabled(true);
					extruderSpeed.setEnabled(true);
					currentTemperature.setEnabled(true);
					desiredTemperature.setEnabled(true);
					heaterActive.setEnabled(true);
					materialEmpty.setEnabled(true);
					coolerActive.setEnabled(true);					
				}
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Exception setting extruder: " + ex);
			ex.printStackTrace();
		}
	}
	
	private void extruderSpeedStateChanged(ChangeEvent evt) {

		jLabel5.setText("Extruder speed: " + extruderSpeed.getValue());
		jLabel5.repaint();
		
		if (extruding)
			setExtruderSpeed();
	}
	
	private void extrudeButtonActionPerformed(ActionEvent evt) {
		if (extruding) {
			extruding = false;
			extrudeButton.setText("Extrude");
		
		} else {
			extruding = true;
			extrudeButton.setText("Stop");
			
			System.out.println("Extruding at speed: " + extruderSpeed.getValue());
		}
		setExtruderSpeed();
	}

	private void setExtruderSpeed() {
		try {
			extruders[extruder].setExtrusion(extruding?extruderSpeed.getValue():0, reverse);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Extruder exception: " + ex);
			ex.printStackTrace();
		}
	}
	
	private void reverseCheckboxStateChanged(ChangeEvent evt) {
		reverse = reverseCheckbox.isSelected();
		setExtruderSpeed();
	}

}
