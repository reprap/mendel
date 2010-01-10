package org.reprap.machines;

/*
 * TODO: To do's:
 * 
 * TODO: fixup warmup segments GCode (forgets to turn on extruder) 
 * TODO: fixup all the RR: println commands 
 * TODO: find a better place for the code. You cannot even detect a layer change without hacking now. 
 * TODO: read Zach's GCode examples to check if I messed up. 
 * TODO: make GCodeWriter a subclass of NullCartesian, so I don't have to fix code all over the place.
 */

import org.reprap.ReprapException;
import org.reprap.Extruder;
import org.reprap.comms.GCodeReaderAndWriter;
import org.reprap.utilities.Debug;
import org.reprap.devices.GCodeExtruder;
import org.reprap.devices.GCodeStepperMotor;
import org.reprap.geometry.LayerRules;

import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 *
 */
public class GCodeRepRap extends GenericRepRap {
	
	/**
	* our class to send gcode instructions
	*/
	GCodeReaderAndWriter gcode;
	
	/**
	 * Force an extruder to be selected on startup
	 */
	Boolean forceSelection;
	
	/**
	 * @param prefs
	 * @throws Exception
	 */
	public GCodeRepRap() throws Exception {
		
		super();

		gcode = new GCodeReaderAndWriter();
		
		loadExtruders();
		
		forceSelection = true;
	}
	
	public void loadMotors()
	{
		motorX = new GCodeStepperMotor(this, 1);
		motorY = new GCodeStepperMotor(this, 2);
		motorZ = new GCodeStepperMotor(this, 3);
	}
	
	public void loadExtruders()
	{
		extruders = new GCodeExtruder[extruderCount];
		
		super.loadExtruders();
	}
	
	public Extruder extruderFactory(int count)
	{
		return new GCodeExtruder(gcode, count, this);
	}
	
	private void qFeedrate(double feedrate)
	{		
		if(currentFeedrate == feedrate)
			return;
		gcode.queue("G1 F" + feedrate + "; feed for start of next move");
		currentFeedrate = feedrate;		
	}
	
	private void qXYMove(double x, double y, double feedrate)
	{	
		double dx = x - currentX;
		double dy = y - currentY;
		
		double xyFeedrate = round(extruders[extruder].getFastXYFeedrate(), 1);
		
		if(xyFeedrate < feedrate)
		{
			System.err.println("GCodeRepRap().qXYMove: feedrate (" + feedrate + ") exceeds maximum (" + xyFeedrate + ").");
			feedrate = xyFeedrate;
		}
		
		if(getExtruder().getMaxAcceleration() <= 0)
			qFeedrate(feedrate);
		
		
		if(dx == 0.0 && dy == 0.0)
		{
			if(currentFeedrate != feedrate)
				qFeedrate(feedrate);
			return;
		}
		
		double extrudeLength;
		String code = "G1 ";

		if (dx != 0)
			code += "X" + x;
		if (dy != 0)
			code += " Y" + y;

		extrudeLength = extruders[extruder].getDistance(Math.sqrt(dx*dx + dy*dy));

		if(extrudeLength > 0)
		{
			if(extruders[extruder].getReversing())
				extruders[extruder].getExtruderState().add(-extrudeLength);
			else
				extruders[extruder].getExtruderState().add(extrudeLength);
			if(extruders[extruder].get5D())
				code += " E" + round(extruders[extruder].getExtruderState().length(), 1);
		}
		
		if (currentFeedrate != feedrate)
		{
			code += " F" + feedrate;
			currentFeedrate = feedrate;
		}
		
		code += " ;horizontal move";
		gcode.queue(code);
		currentX = x;
		currentY = y;
	}
	
	private void qZMove(double z, double feedrate)
	{	
		// Z doesn't accelerate (yet); note we set the feedrate whether we move or not
		
		double zFeedrate = round(getMaxFeedrateZ(), 1);
		
		if(zFeedrate < feedrate)
		{
			System.err.println("GCodeRepRap().qZMove: feedrate (" + feedrate + ") exceeds maximum (" + zFeedrate + ").");
			feedrate = zFeedrate;
		}
		
		if(getMaxZAcceleration() <= 0)
			qFeedrate(feedrate);
		
		double dz = z - currentZ;
		
		if(dz == 0.0)
			return;
		
		String code;
		double extrudeLength;
		
		code = "G1 Z" + z;

		extrudeLength = extruders[extruder].getDistance(dz);

		if(extrudeLength > 0)
		{
			if(extruders[extruder].getReversing())
				extruders[extruder].getExtruderState().add(-extrudeLength);
			else
				extruders[extruder].getExtruderState().add(extrudeLength);
			if(extruders[extruder].get5D())
				code += " E" + round(extruders[extruder].getExtruderState().length(), 1);
		}
		
		if (currentFeedrate != feedrate)
		{
			code += " F" + feedrate;
			currentFeedrate = feedrate;
		}
		
		code += " ;z move";
		gcode.queue(code);
		currentZ = z;	
	}



	/* (non-Javadoc)
	 * @see org.reprap.Printer#moveTo(double, double, double, boolean, boolean)
	 */
	public void moveTo(double x, double y, double z, double feedrate, boolean startUp, boolean endUp) throws ReprapException, IOException
	{
		if (isCancelled())
			return;

		x = round(x, 1);
		y = round(y, 1);
		z = round(z, 4);
		feedrate = round(feedrate, 1);
		
		double dx = x - currentX;
		double dy = y - currentY;
		double dz = z - currentZ;
		
		if (dx == 0.0 && dy == 0.0 && dz == 0.0)
			return;
		
		// This should either be a Z move or an XY move, but not all three
		
		boolean zMove = dz != 0;
		boolean xyMove = dx!= 0 || dy != 0;
		
		if(zMove && xyMove)
			System.err.println("GcodeRepRap.moveTo(): attempt to move in X|Y and Z simultaneously: (x, y, z) = (" + x + ", " + y + ", " + z + ")");

		double zFeedrate = round(getMaxFeedrateZ(), 1);
		
		double liftIncrement = extruders[extruder].getExtrusionHeight()/2;
		double liftedZ = round(currentZ + liftIncrement, 4);

		//go up first?
		if (startUp)
		{
			qZMove(liftedZ, zFeedrate);
			qFeedrate(feedrate);
		}
		
		if(xyMove)
			qXYMove(x, y, feedrate);
		
		if(zMove)
			qZMove(z, feedrate);
		
		if(endUp && !startUp)
		{
			qZMove(liftedZ, zFeedrate);
			qFeedrate(feedrate);			
		}
		
		if(!endUp && startUp)
		{
			qZMove(liftedZ - liftIncrement, zFeedrate);
			qFeedrate(feedrate);			
		}		
		
		super.moveTo(x, y, z, feedrate, startUp, endUp);
	}
	

	
	/**
	 * make a single, non building move (between plots, or zeroing an axis etc.)
	 */
	public void singleMove(double x, double y, double z, double feedrate)
	{
		double x0 = getX();
		double y0 = getY();
		double z0 = getZ();
		x = round(x, 1);
		y = round(y, 1);
		z = round(z, 4);
		double dx = x - x0;
		double dy = y - y0;
		double dz = z - z0;
		
		boolean zMove = dz != 0;
		boolean xyMove = dx != 0 || dy != 0;
		
		if(zMove && xyMove)
			System.err.println("GcodeRepRap.singleMove(): attempt to move in X|Y and Z simultaneously: (x, y, z) = (" + x + ", " + y + ", " + z + ")");
		
		try
		{
			if(xyMove && getExtruder().getMaxAcceleration() <= 0)
			{
				moveTo(x, y, z, feedrate, false, false);
				return;
			}

			if(xyMove)
			{
				double s = Math.sqrt(dx*dx + dy*dy);

				VelocityProfile vp = new VelocityProfile(s, getExtruder().getSlowXYFeedrate(), 
						feedrate, getExtruder().getSlowXYFeedrate(), getExtruder().getMaxAcceleration());
				switch(vp.flat())
				{
				case 0:
					qFeedrate(feedrate);
					moveTo(x, y, z0, feedrate, false, false);
					break;
					
				case 1:
					qFeedrate(getExtruder().getSlowXYFeedrate());
					moveTo(x0 + dx*vp.s1()/s, y0 + dy*vp.s1()/s, z0, vp.v(), false, false);
					moveTo(x, y, z0, getExtruder().getSlowXYFeedrate(), false, false);
					break;
					
				case 2:
					qFeedrate(getExtruder().getSlowXYFeedrate());
					moveTo(x0 + dx*vp.s1()/s, y0 + dy*vp.s1()/s, z0, feedrate, false, false);
					moveTo(x0 + dx*vp.s2()/s, y0 + dy*vp.s2()/s, z0, feedrate, false, false);
					moveTo(x, y, z0, getExtruder().getSlowXYFeedrate(), false, false);
					break;
					
				default:
					System.err.println("GCodeRepRap.singleMove(): dud VelocityProfile XY flat value.");	
				}
			}

			if(zMove)
			{
				VelocityProfile vp = new VelocityProfile(Math.abs(dz), getSlowZFeedrate(), 
						feedrate, getSlowZFeedrate(), getMaxZAcceleration());
				double s = 1;
				if(dz < 0)
					s = -1;
				switch(vp.flat())
				{
				case 0:
					qFeedrate(feedrate);
					moveTo(x0, y0, z, feedrate, false, false);
					break;
					
				case 1:
					qFeedrate(getSlowZFeedrate());
					moveTo(x0, y0, z0 + s*vp.s1(), vp.v(), false, false);
					moveTo(x0, y0, z, getSlowZFeedrate(), false, false);
					break;
					
				case 2:
					qFeedrate(getSlowZFeedrate());
					moveTo(x0, y0, z0 + s*vp.s1(), feedrate, false, false);
					moveTo(x0, y0, z0 + s*vp.s2(), feedrate, false, false);
					moveTo(x0, y0, z, getSlowZFeedrate(), false, false);
					break;
					
				default:
					System.err.println("GCodeRepRap.singleMove(): dud VelocityProfile Z flat value.");	
				}				
			}
		} catch (Exception e)
		{
			System.err.println(e.toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#printTo(double, double, double)
	 */
	public void printTo(double x, double y, double z, double feedrate, boolean stopExtruder, boolean closeValve) throws ReprapException, IOException
	{
		moveTo(x, y, z, feedrate, false, false);
		
		if(stopExtruder)
			getExtruder().stopExtruding();
		if(closeValve)
			getExtruder().setValve(false);
	}
	
	/* (non-Javadoc)
	 * @see org.reprap.Printer#dispose()
	 */
	public void dispose() {
		// TODO: fix this to be more flexible
		gcode.startingEpilogue();
		try
		{
			// Fan off
			getExtruder().setCooler(false);

			// Extruder off
			getExtruder().setExtrusion(0, false);

			// heater off
			getExtruder().heatOff();
		} catch(Exception e){
			//oops
		}
		//write/close our file/serial port
		gcode.reverseLayers();
		gcode.finish();

		super.dispose();
	}
	
	/**
	 * Go to the purge point
	 */
	public void moveToPurge()
	{
		singleMove(dumpX, dumpY, currentZ, getExtruder().getFastXYFeedrate());
	}


	/* (non-Javadoc)
	 * @see org.reprap.Printer#initialise()
	 */
	public void startRun()
	{	
		// If we are printing from a file, that should contain all the headers we need.
		if(gcode.buildingFromFile())
			return;
		
		gcode.startRun();
		
		gcode.queue("; GCode generated by RepRap Java Host Software");
		Date myDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
		String myDateString = sdf.format(myDate);
		gcode.queue("; Created: " + myDateString);

		//take us to fun, safe metric land.
		gcode.queue("G21 ;metric is good!");
		
		// Set absolute positioning, which is what we use.
		gcode.queue("G90 ;absolute positioning");
		
		currentX = 0;
		currentY = 0;
		currentZ = 0;
		currentFeedrate = -100; // Force it to set the feedrate at the start
		
		forceSelection = true;  // Force it to set the extruder to use at the start
				
		try	{
			super.startRun();
		} catch (Exception E) {
			Debug.d("Initialization error: " + E.toString());
		}

	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#printStartDelay(long)
	 */
	public void printStartDelay(long msDelay) {
		// This would extrude for the given interval to ensure polymer flow.
		getExtruder().startExtruding();
		
		delay(msDelay);
	}

	public void home() {

		gcode.queue("G28; go home");
//		// Assume the extruder is off...
//		try
//		{
//			homeToZeroX();
//			homeToZeroY();
//			homeToZeroZ();
//		} catch (Exception e)
//		{
//			
//		}

		extruders[extruder].zeroExtrudedLength();
		super.home();
	}
	
	private void delay(long millis)
	{
		double extrudeLength = getExtruder().getDistanceFromTime(millis);
		if(extrudeLength > 0)
		{
			if(extruders[extruder].get5D())
			{
				qFeedrate(getExtruder().getFastXYFeedrate());
				// Fix the value for possible feedrate change
				extrudeLength = getExtruder().getDistanceFromTime(millis); 
			}

			if(extruders[extruder].getReversing())
				extruders[extruder].getExtruderState().add(-extrudeLength);
			else
				extruders[extruder].getExtruderState().add(extrudeLength);
			if(extruders[extruder].get5D())
			{
				String op = "G1 E" + round(extruders[extruder].getExtruderState().length(), 1);
				if(extruders[extruder].getReversing())
					op += "; extruder retraction";
				else
					op += "; extruder dwell";
				gcode.queue(op);
				qFeedrate(getExtruder().getSlowXYFeedrate());
				return;
			}
		}
		
		gcode.queue("G4 P" + millis + " ;delay");
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroX()
	 */
	public void homeToZeroX() throws ReprapException, IOException {

		
		// Assume extruder is off...
		try
		{
			singleMove(-250, currentY, currentZ, getExtruder().getFastXYFeedrate());
		} catch (Exception e)
		{}
		gcode.queue("G92 X0 ;set x 0");
		super.homeToZeroX();
	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroY()
	 */
	public void homeToZeroY() throws ReprapException, IOException {

		// Assume extruder is off...
		
		try
		{
			singleMove(currentX, -250, currentZ, getExtruder().getFastXYFeedrate());
		} catch (Exception e)
		{}
		gcode.queue("G92 Y0 ;set y 0");
		super.homeToZeroY();

	}

	/* (non-Javadoc)
	 * @see org.reprap.Printer#homeToZeroY()
	 */
	public void homeToZeroZ() throws ReprapException, IOException {

		// Assume extruder is off...
		try
		{
			singleMove(currentX, currentY, -250, getMaxFeedrateZ());
		} catch (Exception e)
		{}
		gcode.queue("G92 Z0 ;set z 0");	
		super.homeToZeroZ();
	}
	
	public double round(double c, double d)
	{
		double power = Math.pow(10.0, d);
		
		return Math.round(c*power)/power;
	}
	
	public void waitTillNotBusy() throws IOException {}

	//TODO: make this work normally.
	public void stopMotor() throws IOException
	{
		getExtruder().stopExtruding();
	}
	
	//TODO: make this work normally.
	public void stopValve() throws IOException
	{
		getExtruder().setValve(false);
	}
	
	/**
	 * All machine dwells and delays are routed via this function, rather than 
	 * calling Thread.sleep - this allows them to generate the right G codes (G4) etc.
	 * 
	 * The RS232/USB etc comms system doesn't use this - it sets its own delays.
	 * @param milliseconds
	 */
	public void machineWait(double milliseconds)
	{
		if(milliseconds <= 0)
			return;
		delay((long)milliseconds);
	}
	
	/**
	 * Wait until the GCodeWriter has exhausted its buffer.
	 */
	public void waitWhileBufferNotEmpty()
	{
		while(!gcode.bufferEmpty())
			gcode.sleep(97);
	}
	
	public void slowBuffer()
	{
		gcode.slowBufferThread();
	}
	
	public void speedBuffer()
	{
		gcode.speedBufferThread();
	}
	
	/**
	 * Load a GCode file to be made.
	 * @return the name of the file
	 */
	public String loadGCodeFileForMaking()
	{
		super.loadGCodeFileForMaking();
		return gcode.loadGCodeFileForMaking();
	}
	
	/**
	 * Set an output file
	 * @return
	 */
	public String setGCodeFileForOutput(String fileRoot)
	{
		return gcode.setGCodeFileForOutput(getTopDown(), fileRoot);
	}
	
	/**
	 * If a file replay is being done, do it and return true
	 * otherwise return false.
	 * @return
	 */
	public boolean filePlay()
	{
		return gcode.filePlay();
	}
	
	/**
	 * Stop the printer building.
	 * This _shouldn't_ also stop it being controlled interactively.
	 */
	public void pause()
	{
		gcode.pause();
	}
	
	/**
	 * Resume building.
	 *
	 */
	public void resume()
	{
		gcode.resume();
	}
	
	public void startingLayer(LayerRules lc) throws Exception
	{
		currentFeedrate = -1;  // Force it to set the feedrate
		gcode.startingLayer(lc);
		gcode.queue(";#!LAYER: " + (lc.getMachineLayer() + 1) + "/" + lc.getMachineLayerMax());		
		super.startingLayer(lc);
	}
	
	/**
	 * Tell the printer class it's Z position.  Only to be used if
	 * you know what you're doing...
	 * @param z
	 */
	public void setZ(double z)
	{
		currentZ = round(z, 4);
	}
	
	public void finishedLayer(LayerRules lc) throws Exception
	{
		super.finishedLayer(lc);
		gcode.finishedLayer();
	}
	
	public void selectExtruder(int materialIndex)
	{
		int oldPhysicalExtruder = getExtruder().getPhysicalExtruderNumber();
		super.selectExtruder(materialIndex);
		int newPhysicalExtruder = getExtruder().getPhysicalExtruderNumber();
		if(newPhysicalExtruder != oldPhysicalExtruder || forceSelection)
		{
			gcode.queue("T" + newPhysicalExtruder + "; select new extruder");
			double pwm = getExtruder().getPWM();
			if(pwm >= 0)
				gcode.queue("M108 S" + pwm + "; set extruder PWM");
			else
				gcode.queue("M110; set extruder to use pot for PWM");
			forceSelection = false;
		}
	}
}
