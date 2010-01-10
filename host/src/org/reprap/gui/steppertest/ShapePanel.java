package org.reprap.gui.steppertest;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.reprap.devices.GenericExtruder;
import org.reprap.devices.GenericStepperMotor;
import org.reprap.devices.pseudo.LinePrinter;

public class ShapePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField x0, x1, y0, y1;
	
	private StepperPanel motorX, motorY;
	private JSlider speed;
	private GenericExtruder extruder;
	
	public ShapePanel(JSlider speed, StepperPanel motorX, StepperPanel motorY, GenericExtruder extruder) {
		super();

		this.speed = speed;
		this.motorX = motorX;
		this.motorY = motorY;
		this.extruder = extruder;
		
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        x0 = new JTextField(5);
        y0 = new JTextField(5);

        x1 = new JTextField(5);
        y1 = new JTextField(5);

        x0.setText("50");
        y0.setText("50");
        x1.setText("3000");
        y1.setText("1000");
        
        
        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("Start co-ord: "), c);
        
        c.gridx = 1;
        add(x0, c);
        c.gridx = 2;
        add(y0, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("End co-ord: "), c);
        c.gridx = 1;
        add(x1, c);
        c.gridx = 2;
        add(y1, c);
        
        JButton button = new JButton("Line");
        button.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		onLineButton();
        	}
        });

        c.gridx = 3;
        c.gridy = 0;
        add(button, c);
	
	}
	
	protected void onLineButton()
	{
		Thread t = new Thread() {
			public void run() {
				try {
					GenericStepperMotor smx = motorX.getMotor();
					GenericStepperMotor smy = motorY.getMotor();
					
					LinePrinter lp = new LinePrinter(smx, smy, extruder);
					
					int x0val = Integer.parseInt(x0.getText());
					int y0val = Integer.parseInt(y0.getText());
					int x1val = Integer.parseInt(x1.getText());
					int y1val = Integer.parseInt(y1.getText());
					
					motorX.setMoved();
					motorY.setMoved();
					//motorX.monitor(true);
					//motorY.monitor(true);
					lp.printLine(x0val, y0val, x1val, y1val, speed.getValue(), 
							speed.getValue(), true, true);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				motorX.monitor(false);
				motorY.monitor(false);
			}
		};
		t.start();			
	}

}
