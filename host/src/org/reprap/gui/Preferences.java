package org.reprap.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridLayout;
import java.io.IOException;
import java.lang.System;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 * This reads in the preferences file and constructs a set of menus from it to allow entries
 * to be edited.
 * 
 * Preference keys either start with the string "Extruder" followed by a number
 * and an underscore (that is, they look like "Extruder3_temp(C)") in which case they
 * are assumed to be a characteristic of the extruder with that number; or they don't,
 * in which case they are assumed to be global characteristics of the entire machine.
 * 
 * The keys should end with their dimensions: "Extruder3_temp(C)", "Axis2Scale(steps/mm)", but
 * regrettably can't contain un-escaped space characters (see java.util.Properties).
 * 
 * Some weak type checking is done to prevent obvious crassness being put in the edit
 * boxes.  This is done at save time and prevents the junk being written, but doesn't give
 * a chance to correct it.
 * 
 * Extensively adapted from Simon's old version by Adrian to construct itself from
 * the preferences file.
 * 
 * TODO: make booleans use check boxes or radio buttons, not "true" or "false".
 */

//Boxes must contain one of three types:

enum Category
{
	number, string, bool;
}

public class Preferences extends JFrame {
	private static final long serialVersionUID = 1L;
	// Load of arrays for all the stuff...
	
	private int extruderCount;
	private JLabel[] globals;              // Array of JLabels for the general key names
	private JTextField[] globalValues;     // Array of JTextFields for the general variables
	private Category[] globalCats;         // What are they?
	private JLabel[][] extruders;          // Array of Arrays of JLabels for the extruders' key names
	private JTextField[][] extruderValues; // Array of Arrays of JTextFields for the extruders' variables
	private Category[][] extruderCats;     // What are they?

	// Get the show on the road...
	
	public static void main(String[] args) 
	{
		new Preferences();
	}
	

	/**
	 * Get the value corresponding to name from the preferences file
	 * @param name
	 * @return String
	 */
	private String loadString(String name) throws IOException {
		return org.reprap.Preferences.loadGlobalString(name);
	}
	
	/**
	 * Save the value corresponding to name to the preferences file
	 * @param name
	 * @param value
	 */	
	private void saveString(String name, String value) throws IOException {
		org.reprap.Preferences.setGlobalString(name, value);
	}
	
	/**
	 * Save the lot to the preferences file
	 *
	 */
	public void savePreferences() {
		try {
			for(int i = 0; i < globals.length; i++)
			{
				String s = globalValues[i].getText();
				if(category(s) != globalCats[i])
					System.err.println("Dud format for " + globals[i].getText() + ": " + s);
				else
					saveString(globals[i].getText(), s);
			}
			
			for(int j = 0; j < extruderCount; j++)
			{
				JLabel[] enames = extruders[j];
				JTextField[] evals = extruderValues[j];
				Category[] cats = extruderCats[j];
				for(int i = 0; i < enames.length; i++)
				{
					String s = evals[i].getText();
					if(category(s) != cats[i])
						System.err.println("Dud format for " + enames[i].getText() + ": " + s);
					else
						saveString(enames[i].getText(), s);
				}
			}
			
			org.reprap.Preferences.saveGlobal();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Saving preferences: " + ex);
			ex.printStackTrace();
		}
	}
	
	/**
	 * Constructor loads all the information from the preferences file,
	 * converts it into arrays of JPanels and JTextFields, then builds the
	 * menus from them.
	 * 
	 * @param frame
	 */
	public Preferences() 
	{
		
		// Start with everything that isn't an extruder value.
		
		try {
			String[] g = org.reprap.Preferences.notStartsWith("Extruder");
			Arrays.sort(g);
			globals = makeLabels(g);
			globalValues = makeValues(globals);
			globalCats = categorise(globalValues);
		}catch (Exception ex)
		{
			System.err.println("Preferences window: Can't load the globals!");
			ex.printStackTrace();
		}
		
		// Next we need to know how many extruders we've got.
		
		try{
			extruderCount = Integer.parseInt(loadString("NumberOfExtruders"));
		} catch (Exception ex)
		{
			System.err.println("Preferences window: Can't load the extruder count!");
			ex.printStackTrace();
		}
		
		// Now build a set of arrays for each extruder in turn.
		
		extruders= new JLabel[extruderCount][];
		extruderValues= new JTextField[extruderCount][];
		extruderCats = new Category[extruderCount][];
		try {
			for(int i = 0; i < extruderCount; i++)
			{
				String[] a = org.reprap.Preferences.startsWith("Extruder" + i);
				Arrays.sort(a);
				extruders[i] = makeLabels(a);
				extruderValues[i]= makeValues(extruders[i]);
				extruderCats[i] = categorise(extruderValues[i]);
			}
		}catch (Exception ex)
		{
			System.err.println("Preferences window: Can't load extruder(s)!");
			ex.printStackTrace();
		}
		
		// Paint the lot on the screen...
		
		initGUI();
        //Utility.centerWindowOnParent(this, frame);
	}
	
	private JButton OKButton()
	{
		JButton jButtonOK = new JButton();
		jButtonOK.setText("OK");
		jButtonOK.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent evt) 
			{
				jButtonOKMouseClicked(evt);
			}
		});
		return jButtonOK;
	}
	
	private JButton CancelButton()
	{
		JButton jButtonCancel = new JButton();
		jButtonCancel.setText("Cancel");
		jButtonCancel.addMouseListener(new MouseAdapter() 
		{
			public void mouseClicked(MouseEvent evt) 
			{
				jButtonCancelMouseClicked(evt);
			}
		});
		return jButtonCancel;
	}	
	
	/**
	 * Set up the panels with all the right boxes in
	 *
	 */
	private void initGUI() 
	{
		setSize(600, 700);
		
		//Dimension box = new Dimension(30, 10);

		// Put it all together

		try {

			// We'll have a tab for the globals, then one 
			// for each extruder

			JTabbedPane jTabbedPane1 = new JTabbedPane();
			add(jTabbedPane1);


			// Do the global panel

			JPanel jPanelGeneral = new JPanel();
			boolean odd = globals.length%2 != 0;
			int rows;
			if(odd)
				rows = globals.length/2 + 2;
			else
				rows = globals.length/2 + 1;
			jPanelGeneral.setLayout(new GridLayout(rows, 4, 5, 5));

			jTabbedPane1.addTab("Globals", null, jPanelGeneral, null);

			// Do it in two chunks, so they're vertically ordered, not horizontally
			
			int half = globals.length/2;
			int next;
			int i;
			for(i = 0; i < half; i++)
			{
				jPanelGeneral.add(globals[i]);
				jPanelGeneral.add(globalValues[i]);
				next = i + half;
				if(next < globals.length)
				{
					jPanelGeneral.add(globals[next]);
					jPanelGeneral.add(globalValues[next]);
				}
			}
			
			if(odd)
			{
				jPanelGeneral.add(globals[globals.length - 1]);
				jPanelGeneral.add(globalValues[globals.length - 1]);
				jPanelGeneral.add(new JLabel());
				jPanelGeneral.add(new JLabel());
			}
			jPanelGeneral.add(OKButton());
			jPanelGeneral.add(new JLabel());
			jPanelGeneral.add(new JLabel());			
			jPanelGeneral.add(CancelButton());
			jPanelGeneral.setSize(600, 700);

			// Do all the extruder panels

			for(int j = 0; j < extruderCount; j++)
			{
				JLabel[] keys = extruders[j];
				JTextField[] values = extruderValues[j];

				JPanel jPanelExtruder = new JPanel();
				odd = keys.length%2 != 0;
				if(odd)
					rows = keys.length/2 + 2;
				else
					rows = keys.length/2 + 1;
				jPanelExtruder.setLayout(new GridLayout(rows, 4, 5, 5));
				jTabbedPane1.addTab("Extruder " + j, null, jPanelExtruder, null);
				
				// Do it in two chunks, so they're vertically ordered, not horizontally
				
				half = keys.length/2;
				for(i = 0; i < keys.length/2; i++)
				{
					jPanelExtruder.add(keys[i]);
					jPanelExtruder.add(values[i]);
					next = i + half;
					if(next < keys.length)
					{
						jPanelExtruder.add(keys[next]);
						jPanelExtruder.add(values[next]);
					}
				}		
				
				if(odd)
				{
					jPanelExtruder.add(keys[keys.length - 1]);
					jPanelExtruder.add(values[keys.length - 1]);
					jPanelExtruder.add(new JLabel());
					jPanelExtruder.add(new JLabel());
				}
				jPanelExtruder.add(OKButton());
				jPanelExtruder.add(new JLabel());
				jPanelExtruder.add(new JLabel());
				jPanelExtruder.add(CancelButton());
				jPanelExtruder.setSize(600, 700);
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Wrap it all up
		//getContentPane().setLayout(null);
		setTitle("RepRap Preferences");
//		setSize(xall, yall);
		pack();
	}

	/**
	 * What to do when OK is clicked
	 * @param evt
	 */
	private void jButtonOKMouseClicked(MouseEvent evt) {
		// Update all preferences
		savePreferences();
		dispose();
	}
	
	/**
	 * What to do when Cancel is clicked
	 * @param evt
	 */
	private void jButtonCancelMouseClicked(MouseEvent evt) {
		// Close without saving
		dispose();
	}
	
	/**
	 * Take an array of strings and turn them into labels (right justified).
	 * @param a
	 * @return
	 */
	private JLabel[] makeLabels(String[] a)
	{
		JLabel[] result = new JLabel[a.length];
		for(int i = 0; i < a.length; i++)
		{
			result[i] = new JLabel();
			result[i].setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
			result[i].setText(a[i]);
		}
		return result;
	}	
	
	/**
	 * Take an array of labels and use their string values as keys to look up
	 * the corresponding values.  Make those into an array of editable boxes.
	 * @param a
	 * @return
	 */
	private JTextField[] makeValues(JLabel[] a)
	{
		JTextField[] result = new JTextField[a.length];
		for(int i = 0; i < a.length; i++)
		{
			try{
				result[i] = new JTextField();
				result[i].setText(loadString(a[i].getText()));
			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * Is a string saying a boolean?
	 * @param s
	 * @return
	 */
	private boolean isBoolean(String s)
	{
		if(s.equalsIgnoreCase("true"))
			return true;
		if(s.equalsIgnoreCase("false"))
			return true;
		return false;
	}
	
	/**
	 * Is a string a number (int or double)?
	 * 
	 * There must be a better way to do this; also this doesn't allow
	 * for 1.3e-5...
	 * 
	 * @param s
	 * @return
	 */
	private boolean isNumber(String s)
	{
		// Bulletproofing.
		if ((s==null)||(s.length()==0)) return false;
		
		int start = 0;
		
		while(Character.isSpaceChar(s.charAt(start)))
			start++;
		
		if(s.charAt(start) == '-' || s.charAt(start) == '+')
			start++;
		
		// Last we checked, only one decimal point allowed per number.
		int dotCount = 0;
		for(int i = start; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if(!Character.isDigit(c))
			{
				 if(c != '.')
					return false;
				 else
				 {
					 dotCount++;
					 if(dotCount > 1)
						 return false;
				 }
			}
		}
		return true;
	}
	
	/**
	 * Find if a string is a boolean, a number, or a string
	 * @param s
	 * @return
	 */
	private Category category(String s)
	{
		if(isBoolean(s))
			return Category.bool;
		
		if(isNumber(s))
			return Category.number;		
		
		return Category.string;
	}
	
	/**
	 * Generate an array of categories corresponsing to the text in 
	 * an array of edit boxes so they can be checked later.
	 * @param a
	 * @return
	 */
	private Category[] categorise(JTextField[] a)
	{
		Category[] result = new Category[a.length];
		for(int i = 0; i < a.length; i++)
			result[i] = category(a[i].getText());
		
		return result;
	}
}
