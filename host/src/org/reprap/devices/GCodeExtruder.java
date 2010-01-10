package org.reprap.devices;

import java.io.IOException;

import org.reprap.comms.GCodeReaderAndWriter;
import org.reprap.Printer;
import org.reprap.Preferences;

public class GCodeExtruder extends GenericExtruder
{
	GCodeReaderAndWriter gcode;
	
	/**
	 * @param prefs
	 * @param extruderId
	 */
	public GCodeExtruder(GCodeReaderAndWriter writer, int extruderId, Printer p)
	{
		super(extruderId, p);
		es.setSpeed(0);
		gcode = writer;
	}
	
	/**
	 * Zero the extruded length
	 *
	 */
	public void zeroExtrudedLength()
	{
		if(es.length() > 0)
		{
			super.zeroExtrudedLength();
			gcode.queue("G92 E0 ;zero the extruded length");
		}
	}
	
	/**
	 * Purge the extruder
	 */
	public void purge()
	{
		if(purgeTime <= 0)
			return;
		getPrinter().moveToPurge();
		try
		{
			heatOn(true);
			setExtrusion(getFastXYFeedrate(), false);
			getPrinter().machineWait(purgeTime);
			setExtrusion(0, false);
		} catch (Exception e)
		{}
		getPrinter().home();
		zeroExtrudedLength();
	}
	
	public void setTemperature(double temperature, boolean wait) throws Exception
	{
		if(wait)
			gcode.queue("M109 S" + temperature + " ;set temperature and wait");
		else
			gcode.queue("M104 S" + temperature + " ;set temperature and return");
		super.setTemperature(temperature, wait);
	}
	
	public void setHeater(int heat, double maxTemp) {}
	
	public double getTemperature()
	{
		es.setCurrentTemperature(Double.parseDouble(gcode.queueRespond("M105; get temperature").substring(2))); // Throw away "T:"
		return es.currentTemperature();
	}
	
	public void setExtrusion(double speed, boolean reverse) throws IOException
	{
		if(getExtruderSpeed() < 0)
			return;
		
		if (speed < Preferences.tiny())
		{
			if(!fiveD)
				gcode.queue("M103" + " ;extruder off");
		} else
		{
			if(!fiveD)
			{
				if (speed != es.speed())
					gcode.queue("M108 S" + speed + " ;extruder speed in RPM");

				if (es.reverse())
					gcode.queue("M102" + " ;extruder on, reverse");
				else
					gcode.queue("M101" + " ;extruder on, forward");
			}
		}
		super.setExtrusion(speed, reverse);
	}
	
	//TODO: make these real G codes.
	public void setCooler(boolean coolerOn) throws IOException
	{
		if (coolerOn)
			gcode.queue("M106 ;cooler on");
		else
			gcode.queue("M107 ;cooler off");
	}
	

	public void setValve(boolean valveOpen) throws IOException
	{
		if(valvePulseTime <= 0)
			return;
		if (valveOpen)
			gcode.queue("M126 P" + valvePulseTime + ";valve open");
		else
			gcode.queue("M127 P" + valvePulseTime + ";valve closed");
	}
	
	public boolean isEmpty()
	{
		return false;
	} 
}