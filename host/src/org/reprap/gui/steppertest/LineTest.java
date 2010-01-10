package org.reprap.gui.steppertest;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.reprap.AxisMotor;
import org.reprap.Extruder;
import org.reprap.devices.pseudo.LinePrinter;

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
public class LineTest extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;
	private JButton OkButton;
	private JLabel jLabel4;
	private JButton plotButton;
	private JButton moveButton;
	private JLabel jLabel3;
	private JTextField endY;
	private JTextField startY;
	private JTextField endX;
	private JTextField startX;
	private JLabel jLabel2;
	private JLabel jLabel1;
	
	private int motorSpeed, extruderSpeed;
	
	private LinePrinter line;

	public LineTest(JFrame frame, AxisMotor motorX, AxisMotor motorY, Extruder extruder, int motorSpeed, int extruderSpeed) throws IOException {
		super(frame);
		this.motorSpeed = motorSpeed;
		this.extruderSpeed = extruderSpeed;
		
		line = new LinePrinter(motorX, motorY, extruder);
		
		initGUI();
		
		startX.setText(Integer.toString(motorX.getPosition()));
		startY.setText(Integer.toString(motorY.getPosition()));
		
		if (extruder == null || !extruder.isAvailable())
			plotButton.setEnabled(false);
	}
	
	private void initGUI() {
		try {
			{
				OkButton = new JButton();
				getContentPane().add(OkButton);
				OkButton.setText("Done");
				OkButton.setBounds(280, 119, 77, 28);
				OkButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						OkButtonActionPerformed(evt);
					}
				});
			}
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1);
				jLabel1.setText("End position");
				jLabel1.setBounds(14, 70, 98, 28);
				jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2);
				jLabel2.setText("Start position");
				jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
				jLabel2.setBounds(14, 35, 98, 28);
			}
			{
				startX = new JTextField();
				getContentPane().add(startX);
				startX.setText("400");
				startX.setBounds(126, 35, 63, 28);
				startX.setEditable(false);
			}
			{
				endX = new JTextField();
				getContentPane().add(endX);
				endX.setText("3200");
				endX.setBounds(126, 70, 63, 28);
				endX.setName("");
			}
			{
				startY = new JTextField();
				getContentPane().add(startY);
				startY.setText("400");
				startY.setBounds(203, 35, 63, 28);
				startY.setEditable(false);
			}
			{
				endY = new JTextField();
				getContentPane().add(endY);
				endY.setText("2400");
				endY.setName("");
				endY.setBounds(203, 70, 63, 28);
			}
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3);
				jLabel3.setText("Y");
				jLabel3.setBounds(203, 14, 63, 21);
				jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4);
				jLabel4.setText("X");
				jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
				jLabel4.setBounds(126, 14, 63, 21);
			}
			{
				moveButton = new JButton();
				getContentPane().add(moveButton);
				moveButton.setText("Move");
				moveButton.setBounds(280, 35, 77, 28);
				moveButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						moveButtonActionPerformed(evt);
					}
				});
			}
			{
				plotButton = new JButton();
				getContentPane().add(plotButton);
				plotButton.setText("Plot");
				plotButton.setBounds(280, 70, 77, 28);
				plotButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						plotButtonActionPerformed(evt);
					}
				});
			}
			{
				getContentPane().setLayout(null);
				this.setModal(true);
				this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.setTitle("Line Test");
			}
			this.setSize(386, 193);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void OkButtonActionPerformed(ActionEvent evt) {
		dispose();
	}
	
	private void moveButtonActionPerformed(ActionEvent evt) {
		int x1 = Integer.parseInt(endX.getText()); 
		int y1 = Integer.parseInt(endY.getText()); 
		
		try {
			line.moveTo(x1, y1, motorSpeed, false, false);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Move exception: " + ex);
			ex.printStackTrace();
		}
		
		startX.setText(endX.getText());
		startY.setText(endY.getText());
	}
	
	private void plotButtonActionPerformed(ActionEvent evt) {
		int x1 = Integer.parseInt(startX.getText()); 
		int y1 = Integer.parseInt(startY.getText()); 
		int x2 = Integer.parseInt(endX.getText()); 
		int y2 = Integer.parseInt(endY.getText()); 
		
		try {
			line.printLine(x1, y1, x2, y2, motorSpeed, extruderSpeed, true, true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Plot exception: " + ex);
			ex.printStackTrace();
		}

		startX.setText(endX.getText());
		startY.setText(endY.getText());
		
	}

}
