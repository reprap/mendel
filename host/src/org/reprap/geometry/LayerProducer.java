/*
 * Created on May 1, 2006
 *
 * Changed by Vik to reject polts of less than 0.05mm
 */
package org.reprap.geometry;

import java.io.IOException;

import javax.media.j3d.BranchGroup;

import org.reprap.Printer;
import org.reprap.Attributes;
import org.reprap.Preferences;
import org.reprap.ReprapException;
import org.reprap.Extruder;
import org.reprap.devices.pseudo.LinePrinter;
import org.reprap.geometry.polygons.Rr2Point;
//import org.reprap.geometry.polygons.RrCSGPolygonList;
import org.reprap.geometry.polygons.BooleanGridList;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;
import org.reprap.geometry.polygons.RrRectangle;
import org.reprap.geometry.polygons.BooleanGrid;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;

/**
 *
 */
class segmentSpeeds
{
	/**
	 * 
	 */
	public Rr2Point p1, p2, p3;
	
	/**
	 * 
	 */
	public double ca;
	
	/**
	 * 
	 */
	public boolean plotMiddle;
	
	/**
	 * 
	 */
	public boolean abandon;
	
	/**
	 * @param before
	 * @param now
	 * @param after
	 * @param fastLength
	 */
	public segmentSpeeds(Rr2Point before, Rr2Point now, Rr2Point after, double fastLength)
	{
		Rr2Point a = Rr2Point.sub(now, before);
		double amod = a.mod();
		abandon = amod == 0;
		if(abandon)
			return;
		Rr2Point b = Rr2Point.sub(after, now);
		if(b.mod() == 0)
			ca = 0;
		else
			ca = Rr2Point.mul(a.norm(), b.norm());
		plotMiddle = true;
		if(amod <= 2*fastLength)
		{
			fastLength = amod*0.5;
			plotMiddle = false;
		}
		a = a.norm();
		p1 = Rr2Point.add(before, Rr2Point.mul(a, fastLength));
		p2 = Rr2Point.add(p1, Rr2Point.mul(a, amod - 2*fastLength));
		p3 = Rr2Point.add(p2, Rr2Point.mul(a, fastLength));
	}
	
	int speed(int currentSpeed, double angFac)
	{
		double fac = (1 - 0.5*(1 + ca)*angFac);
		return LinePrinter.speedFix(currentSpeed, fac);
	}
}

/**
 *
 */
public class LayerProducer {
	
	private RrGraphics simulationPlot = null;
	
	/**
	 * 
	 */
	private LayerRules layerConditions = null;
	
	/**
	 * 
	 */
	private boolean paused = false;

	/**
	 * The shape of the object built so far under the current layer
	 */
	private BranchGroup lowerShell = null;

	
	/**
	 * The polygons to infill
	 */
	private RrPolygonList hatchedPolygons = null;
	
	/**
	 * The polygons to outline
	 */
	private RrPolygonList borderPolygons = null;
	
	/**
	 * Boolean Grid representation of the polygons as input
	 */
	private BooleanGridList boolGrdSlice = null;
	
	/**
	 * CSG representation of the polygons offset by the width of
	 * the extruders
	 */
	//RrCSGPolygonList offBorder = null;
	
	/**
	 * CSG representation of the polygons offset by the factor of the
	 * width of the extruders needed to lay down internal cross-hatching
	 */	
	BooleanGridList offHatch = null;
	
	/**
	 * Counters use so that each material is plotted completely before
	 * moving on to the next.
	 */
	private int commonBorder, commonHatch;
	
	/**
	 * The clue is in the name...
	 */
	private double currentFeedrate;
	
	/**
	 * Record the end of each polygon as a clue where to start next
	 */
	private Rr2Point startNearHere = null;
	
	private boolean shellSet = false;
	
	/**
	 * Flag to prevent cyclic graphs going round forever
	 */
	private boolean beingDestroyed = false;
	
	/**
	 * Destroy me and all that I point to
	 */
	public void destroy() 
	{
		if(beingDestroyed) // Prevent infinite loop
			return;
		beingDestroyed = true;
		
		// Keep the lower shell - the graphics system is using it
		
		//lowerShell = null;

		// Keep the printer; that's needed for the next layer

		//printer = null;
		
		if(hatchedPolygons != null)
			hatchedPolygons.destroy();
		hatchedPolygons = null;
		
		if(borderPolygons != null)
			borderPolygons.destroy();
		borderPolygons = null;
		
		//if(boolGrdSlice != null)
		//	boolGrdSlice.destroy();
		boolGrdSlice = null;
		
		//if(offHatch != null)
			//offHatch.destroy();
		offHatch = null;
		
		if(startNearHere != null)
			startNearHere.destroy();
		startNearHere = null;
		beingDestroyed = false;
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		// Keep the lower shell - the graphics system is using it
		
		//lowerShell = null;

		// Keep the printer; that's needed for the next layer

		//printer = null;
		
		hatchedPolygons = null;
		borderPolygons = null;
		boolGrdSlice = null;
		offHatch = null;
		startNearHere = null;
		super.finalize();
	}
	
	/**
	 * Set up a layer consisting of a single material in a single pre-computed list.
	 * Used for creating foundations.
	 * @param pols
	 * @param lc
	 * @param simPlot
	 * @throws Exception
	 */
	public LayerProducer(RrPolygonList pols, LayerRules lc, RrGraphics simPlot) throws Exception 
	{
		layerConditions = lc;
		startNearHere = null; //new Rr2Point(0, 0);
		lowerShell = null;
		shellSet = false;
		simulationPlot = simPlot;
		
		boolGrdSlice = null;
		
		borderPolygons = null;
		
		hatchedPolygons = pols;
		
		
		if(simulationPlot != null)
		{
			if(!simulationPlot.isInitialised())
				simulationPlot.init(lc.getBox(), false);
			else
				simulationPlot.cleanPolygons();
		}

	}
	
	
	/**
	 * Set up a normal layer
	 * @param boolGrdSliceols
	 * @param ls
	 * @param lc
	 * @param simPlot
	 * @throws Exception
	 */
	public LayerProducer(BooleanGridList bgPols, BranchGroup ls, LayerRules lc, RrGraphics simPlot) throws Exception 
	{
		layerConditions = lc;
		startNearHere = null;
		lowerShell = ls;
		shellSet = false;
		simulationPlot = simPlot;
		
		boolGrdSlice = bgPols;
		
		// The next two are experimental for the moment...
		
		//inFillCalculations();
		//supportCalculations();
		
		offHatch = boolGrdSlice.offset(layerConditions, false);
		
		if(layerConditions.getLayingSupport())
		{
			borderPolygons = null;
			offHatch = offHatch.union(lc.getPrinter().getExtruders());
		} else
		{
			BooleanGridList offBorder = boolGrdSlice.offset(layerConditions, true);
			borderPolygons = offBorder.borders();
		}
			
		hatchedPolygons = offHatch.hatch(layerConditions);


		if(borderPolygons != null && borderPolygons.size() > 0)
		{
			borderPolygons.middleStarts(hatchedPolygons, layerConditions);
			if(Preferences.loadGlobalBool("Shield"))
			{
				RrRectangle rr = lc.getBox();
				Rr2Point corner = Rr2Point.add(rr.sw(), new Rr2Point(-3, -3));
				RrPolygon ell = new RrPolygon(borderPolygons.polygon(0).getAttributes(), false);
				ell.add(corner);
				ell.add(Rr2Point.add(corner, new Rr2Point(-2, 10)));
				ell.add(Rr2Point.add(corner, new Rr2Point(-2, -2)));
				ell.add(Rr2Point.add(corner, new Rr2Point(20, -2)));
				borderPolygons.add(0, ell);
			}
		}
		
		if(simulationPlot != null)
		{
			if(!simulationPlot.isInitialised())
				simulationPlot.init(lc.getBox(), false);
			else
				simulationPlot.cleanPolygons();
		}
	}
	
//	/**
//	 * look at the layer above where we are, and support everything
//	 * in it that has nothing under it in this layer.
//	 *
//	 */
//	private void supportCalculations()
//	{
//		// We can only work out support if we're going top down
//		
//		if(!layerConditions.getTopDown())
//			return;
//		
//		// Get the layer immediately above
//		
//		RrCSGPolygonList above = layerConditions.getLayerAbove(1);
//		
//		// If there was no layer immediately above, record this layer to 
//		// be the one above for the next layer down and return.
//		
//		if(above == null)
//		{
//			layerConditions.recordThisLayer(boolGrdSlice);
//			return;
//		}
//		
//		// Pick up our materials
//		
//		Extruder [] es = layerConditions.getPrinter().getExtruders();
//		
//		// A list for the supports for the materials in the layer above
//		
//		RrCSGPolygonList supports = new RrCSGPolygonList();
//		
//		// Each material in this layer unioned with the same material in the layer above
//		// This may be what needs support on the next layer down.
//		
//		RrCSGPolygonList thisForTheRecord = new RrCSGPolygonList();
//		
//		// Compute the union of everything on this layer and grow it by the
//		// extrusion height (i.e. grow to a 45 degree overhang).  Nothing in that region
//		// will need any support.
//		
//		RrCSGPolygon allThisLayer = new RrCSGPolygon();
//		for(int i = 0; i < boolGrdSlice.size(); i++)
//		{
//			allThisLayer = RrCSGPolygon.union(boolGrdSlice.get(i), allThisLayer);
//			if(i > 0)
//				allThisLayer = allThisLayer.reEvaluate();
//		}
//		RrCSGPolygon allThisLayerGrown = allThisLayer.offset(layerConditions.getZStep());
//	
//		// This material's shape in the above layer, its attributes,
//		// the extruder used for it, and the name of its support material.
//		
//		RrCSGPolygon pgAboveLevel;
//		Attributes aAboveLevel;
//		Extruder eAboveLevel;
//		String supportName;
//		
//		// The polygons at this level
//		
//		RrCSGPolygon thisLevel;
//		
//		// For each material in the layer above...
//		
//		for(int i = 0; i < above.size(); i++)
//		{
//			// Get this material's shape in the above layer, its attributes,
//			// the extruder used for it, and the name of its support material.
//			
//			pgAboveLevel = above.get(i);
//			aAboveLevel = pgAboveLevel.getAttributes();
//			eAboveLevel = aAboveLevel.getExtruder(es);
//			supportName = eAboveLevel.getSupportMaterial();
//			
//			// If this stuff's support is not called "null"...
//			
//			if(!supportName.contentEquals("null"))
//			{
//				// Pick up the support extruder
//				
//				Extruder supportExtruder = es[GenericExtruder.getNumberFromMaterial(supportName)];
//				
//				// Find the same material at this level
//				
//				thisLevel = boolGrdSlice.find(aAboveLevel);
//				
//				// If the material is in this level and the one above...
//				
//				if(thisLevel != null)
//				{
//					// The union of them both is the shape that may need support at the next level down
//					
//					RrCSGPolygon toRemember = RrCSGPolygon.union(pgAboveLevel, thisLevel);
//					toRemember = toRemember.reEvaluate();
//					thisForTheRecord.add(toRemember);
//					
//					// The bit left over of the level above after we subtract all this layer
//					// is what needs support
//					
//					RrCSGPolygon sup = RrCSGPolygon.difference(pgAboveLevel, allThisLayerGrown);
//					sup = sup.reEvaluate();
//					sup.setAttributes(new Attributes(supportName, null, null, 
//							supportExtruder.getAppearance()));
//					supports.add(sup);
//				} else
//					
//					// If the material wasn't in this layer, carry the need to support it on down
//					
//					thisForTheRecord.add(pgAboveLevel);
//			}
//		}
//		
//		// Add every material in this layer that has no equivalent in the layer
//		// above as it may need support in the next layer down.
//		
//		for(int i = 0; i < boolGrdSlice.size(); i++)
//		{
//			thisLevel = boolGrdSlice.get(i);
//			if(above.find(thisLevel.getAttributes()) == null)
//				thisForTheRecord.add(thisLevel);	
//		}
//		
//		// Record everything in this layer as potentially needing support
//		// in the next layer down.
//		
//		layerConditions.recordThisLayer(thisForTheRecord);
//		
//		// Add all the supports needed to this layer
//		
//		boolGrdSlice.add(supports);
//		
//	}
//	
//	/**
//	 * look at the layer above where we are, and calculate the infill depending
//	 * on whether something is an open surface or not.
//	 *
//	 */
//	private void inFillCalculations()
//	{
//		// We can only work out infill if we're going top down
//		
//		if(!layerConditions.getTopDown())
//			return;
//		
//		// Get the layer immediately above
//		
//		RrCSGPolygonList above = layerConditions.getLayerAbove(2);
//		if(above == null)
//		{
//			layerConditions.recordThisLayer(boolGrdSlice);
//			return;
//		}
//		
//		// Pick up our materials
//		
//		Extruder [] es = layerConditions.getPrinter().getExtruders();
//		
//		//Horrid hack...
//		
//		if(layerConditions.getModelLayer() < es[0].getLowerFineLayers())
//			return;
//		
//		// Compute the union of everything on the layer above
//		// Nothing in that region will have a free surface in this layer.
//
//		RrCSGPolygon allAboveLayer = new RrCSGPolygon();
//		for(int i = 0; i < above.size(); i++)
//		{
//			allAboveLayer = RrCSGPolygon.union(above.get(i), allAboveLayer);
//			if(i > 0)
//				allAboveLayer = allAboveLayer.reEvaluate();
//		}
//		
//
//		
//		// A list for the infills
//		
//		RrCSGPolygonList inFills = new RrCSGPolygonList();
//		
//		// A list for the free surface bits
//		
//		RrCSGPolygonList surfaces = new RrCSGPolygonList();	
//
//		// This material's shape, its attributes,
//		// the extruder used for it, and the name of its infill material.
//		
//		RrCSGPolygon pgThisLevel;
//		Attributes aThisLevel;
//		Extruder eThisLevel;
//		String inFillName;
//		
//		// For each material in this layer
//		
//		for(int i = 0; i < boolGrdSlice.size(); i++)
//		{
//			// Get this material's shape in the above layer, its attributes,
//			// the extruder used for it, and the name of its support material.
//			
//			pgThisLevel = boolGrdSlice.get(i);
//			aThisLevel = pgThisLevel.getAttributes();
//			eThisLevel = aThisLevel.getExtruder(es);
//			inFillName = eThisLevel.getBroadInfillMaterial();
//			
//			// If this stuff's infill is not called "null"...
//
//			if(!inFillName.contentEquals("null"))
//			{
//				// Pick up the infill extruder
//
//				Extruder inFillExtruder = es[GenericExtruder.getNumberFromMaterial(inFillName)];
//
//				// The exposed region is whatever's in this layer that isn't above
//				
//				RrCSGPolygon exposed = RrCSGPolygon.difference(pgThisLevel, allAboveLayer);
//				exposed = exposed.reEvaluate();
//				
//				// Make the exposed layer bigger...
//				
//				exposed = exposed.offset(layerConditions.getZStep()*eThisLevel.getLowerFineLayers());
//				
//				// ...Then intersect it with what we have already.  That will cause the exposed
//				// region to penetrate the solid a bit.
//				
//				exposed = RrCSGPolygon.intersection(exposed, pgThisLevel);
//				exposed = exposed.reEvaluate();
//				surfaces.add(exposed);
//				
//				// What's left of this layer after the exposed bit is subtracted is the infill part
//				
//				pgThisLevel = RrCSGPolygon.difference(pgThisLevel, exposed);
//				pgThisLevel = pgThisLevel.reEvaluate();
//				pgThisLevel.setAttributes(new Attributes(inFillName, null, null, 
//						inFillExtruder.getAppearance()));
//				inFills.add(pgThisLevel);
//			}
//		}
//		
//		// We now have two new collections of polygons representing this layer
//		
//		boolGrdSlice = inFills;
//		boolGrdSlice.add(surfaces);
//		layerConditions.recordThisLayer(boolGrdSlice);
//	}
	
	/**
	 * Stop printing
	 *
	 */
	public void pause()
	{
		paused = true;
	}
	
	/**
	 * Start printing
	 *
	 */
	public void resume()
	{
		paused = false;
	}
	
	/**
	 * @return current X and Y position of the printer
	 */
	private Rr2Point posNow()
	{
		return new Rr2Point(layerConditions.getPrinter().getX(), layerConditions.getPrinter().getY());
	}
	
	/**
	 * speed up for short lines
	 * @param p
	 * @return
	 * @throws ReprapException
	 * @throws IOException
	 */
	private boolean shortLine(Rr2Point p, boolean stopExtruder, boolean closeValve) throws ReprapException, IOException
	{
		Printer printer = layerConditions.getPrinter();
		double shortLen = printer.getExtruder().getShortLength();
		if(shortLen < 0)
			return false;
		Rr2Point a = Rr2Point.sub(posNow(), p);
		double amod = a.mod();
		if(amod > shortLen) {
//			Debug.d("Long segment.  Current feedrate is: " + currentFeedrate);
			return false;
		}

		//printer.setFeedrate(printer.getExtruder().getShortLineFeedrate());
// TODO: FIX THIS
//		printer.setSpeed(LinePrinter.speedFix(printer.getExtruder().getXYSpeed(), 
//				printer.getExtruder().getShortSpeed()));
		printer.printTo(p.x(), p.y(), layerConditions.getMachineZ(), printer.getExtruder().getShortLineFeedrate(), stopExtruder, closeValve);
		//printer.setFeedrate(currentFeedrate);
		return true;	
	}
	
	/**
	 * @param first First point, the end of the line segment to be plotted to from the current position.
	 * @param second Second point, the end of the next line segment; used for angle calculations
	 * @param turnOff True if the extruder should be turned off at the end of this segment.
	 * @throws ReprapException
	 * @throws IOException
	 */
	private void plot(Rr2Point first, Rr2Point second, boolean stopExtruder, boolean closeValve) throws ReprapException, IOException
	{
		Printer printer = layerConditions.getPrinter();
		if (printer.isCancelled()) return;
		
		// Don't call delay; this isn't controlling the printer
		while(paused)
		{
			try
			{
				Thread.sleep(200);
			} catch (Exception ex) {}
		}
		
		if(shortLine(first, stopExtruder, closeValve))
			return;
		
		double z = layerConditions.getMachineZ();
		
		double speedUpLength = printer.getExtruder().getAngleSpeedUpLength();
		if(speedUpLength > 0)
		{
			segmentSpeeds ss = new segmentSpeeds(posNow(), first, second, 
					speedUpLength);
			if(ss.abandon)
				return;

			printer.printTo(ss.p1.x(), ss.p1.y(), z, currentFeedrate, false, false);

			if(ss.plotMiddle)
			{
//TODO: FIX THIS.
//				int straightSpeed = LinePrinter.speedFix(currentSpeed, (1 - 
//						printer.getExtruder().getAngleSpeedFactor()));
				//printer.setFeedrate(printer.getExtruder().getAngleFeedrate());
				printer.printTo(ss.p2.x(), ss.p2.y(), z, printer.getExtruder().getAngleFeedrate(), false, false);
			}

			//printer.setSpeed(ss.speed(currentSpeed, printer.getExtruder().getAngleSpeedFactor()));
			
			//printer.setFeedrate(printer.getExtruder().getAngleFeedrate());
			printer.printTo(ss.p3.x(), ss.p3.y(), z, printer.getExtruder().getAngleFeedrate(), stopExtruder, closeValve);
			//pos = ss.p3;
		// Leave speed set for the start of the next line.
		} else
			printer.printTo(first.x(), first.y(), z, currentFeedrate, stopExtruder, closeValve);
	}
	
	private void singleMove(Rr2Point p)
	{
		Printer pt = layerConditions.getPrinter();
		pt.singleMove(p.x(), p.y(), pt.getZ(), pt.getFastXYFeedrate());
	}
	
	/**
	 * @param first
	 * @param second
	 * @param startUp
	 * @param endUp
	 * @throws ReprapException
	 * @throws IOException
	 */
	private void move(Rr2Point first, Rr2Point second, boolean startUp, boolean endUp, boolean fast) 
		throws ReprapException, IOException
	{
		Printer printer = layerConditions.getPrinter();
		
		if (printer.isCancelled()) return;
		
//		 Don't call delay; this isn't controlling the printer
		while(paused)
		{
			try
			{
				Thread.sleep(200);
			} catch (Exception ex) {}
		}
		
		double z = layerConditions.getMachineZ();
		
		//if(startUp)
		if(fast)
		{
			//printer.setFeedrate(printer.getFastFeedrateXY());
			printer.moveTo(first.x(), first.y(), z, printer.getExtruder().getFastXYFeedrate(), startUp, endUp);
			return;
		}
		
		double speedUpLength = printer.getExtruder().getAngleSpeedUpLength();
		if(speedUpLength > 0)
		{
			segmentSpeeds ss = new segmentSpeeds(posNow(), first, second, 
					speedUpLength);
			if(ss.abandon)
				return;

			printer.moveTo(ss.p1.x(), ss.p1.y(), z, printer.getCurrentFeedrate(), startUp, startUp);

			if(ss.plotMiddle)
			{
				//printer.setFeedrate(currentFeedrate);
				printer.moveTo(ss.p2.x(), ss.p2.y(), z, currentFeedrate, startUp, startUp);
			}

			//TODO: FIX ME!
			//printer.setSpeed(ss.speed(currentSpeed, printer.getExtruder().getAngleSpeedFactor()));
			
			//printer.setFeedrate(printer.getExtruder().getAngleFeedrate());
			printer.moveTo(ss.p3.x(), ss.p3.y(), z, printer.getExtruder().getAngleFeedrate(), startUp, endUp);
			//pos = ss.p3;
			// Leave speed set for the start of the next movement.
		} else
			printer.moveTo(first.x(), first.y(), z, currentFeedrate, startUp, endUp);
	}


	/**
	 * Plot a polygon
	 * @throws IOException
	 * @throws ReprapException
	 * @return
	 */
	private void plot(RrPolygon p, boolean firstOneInLayer) throws ReprapException, IOException
	{
		Attributes att = p.getAttributes();
		Printer printer = layerConditions.getPrinter();
		double outlineFeedrate = att.getExtruder().getOutlineFeedrate();
		double infillFeedrate = att.getExtruder().getInfillFeedrate();
		
		boolean acc = att.getExtruder().getMaxAcceleration() > 0; //Preferences.loadGlobalBool("Accelerating");
			
		//int leng = p.size();
	
		if(p.size() <= 1)
		{
			startNearHere = null;
			return;
		}

		// If the length of the plot is <0.05mm, don't bother with it.
		// This will not spot an attempt to plot 10,000 points in 1mm.
		double plotDist=0;
		Rr2Point lastPoint=p.point(0);
		for (int i=1; i<p.size(); i++)
		{
			Rr2Point n=p.point(i);
			plotDist+=Rr2Point.d(lastPoint, n);
			lastPoint=n;
		}
		if (plotDist<Preferences.machineResolution()*0.5) {
			Debug.d("Rejected line with "+p.size()+" points, length: "+plotDist);
			startNearHere = null;
			return;
		}	
		
		printer.selectExtruder(att);
		
// Don't do these with mid-point starting
		
//		if(p.isClosed() && att.getExtruder().incrementedStart())
//			p = p.incrementedStart(layerConditions);
//		else if(p.isClosed() && att.getExtruder().randomStart())
//			p = p.randomStart();
		
		int stopExtruding = p.size() + 10;
		int stopValve = stopExtruding;
		double extrudeBackLength = att.getExtruder().getExtrusionOverRun();
		double valveBackLength = att.getExtruder().getValveOverRun();
		if(extrudeBackLength >= valveBackLength)
		{
			if(extrudeBackLength > 0)
				stopExtruding = p.backStep(extrudeBackLength);
			if(valveBackLength > 0)
				stopValve = p.backStep(valveBackLength);
		} else
		{
			if(valveBackLength > 0)
				stopValve = p.backStep(valveBackLength);
			if(extrudeBackLength > 0)
				stopExtruding = p.backStep(extrudeBackLength);			
		}
		
		if (printer.isCancelled()) return;
		
		// If getMinLiftedZ() is negative, never lift the head
		
		Boolean lift = att.getExtruder().getMinLiftedZ() >= 0;
		
		if(acc)
			p.setSpeeds(att.getExtruder().getSlowXYFeedrate(), p.isClosed()?outlineFeedrate:infillFeedrate, 
					att.getExtruder().getMaxAcceleration());
		
		
		if(extrudeBackLength <= 0)
			stopExtruding = Integer.MAX_VALUE;
		else if(acc)
			stopExtruding = p.findBackPoint(extrudeBackLength);
		
		if(valveBackLength <= 0)
			stopValve = Integer.MAX_VALUE;
		else if(acc)
			stopValve = p.findBackPoint(valveBackLength);

		currentFeedrate = att.getExtruder().getFastXYFeedrate();
		singleMove(p.point(0));
		
		if(acc)
			currentFeedrate = p.speed(0);
		else
		{
			if(p.isClosed())
			{
				currentFeedrate = outlineFeedrate;			
			} else
			{
				currentFeedrate = infillFeedrate;			
			}
		}
		
		plot(p.point(0), p.point(1), false, false);
		
		// Print any lead-in.
		printer.printStartDelay(firstOneInLayer);
		
		boolean extrudeOff = false;
		boolean valveOff = false;
		boolean oldexoff;
		
//		if(p.isClosed())
//		{
//			for(int j = 1; j <= p.size(); j++)
//			{
//				int i = j%p.size();
//				Rr2Point next = p.point((j+1)%p.size());
//				
//				if (printer.isCancelled())
//				{
//					printer.stopMotor();
//					singleMove(posNow());
//					move(posNow(), posNow(), lift, true, true);
//					return;
//				}
//				if(acc)
//					currentFeedrate = p.speed(i);
//				
//				oldexoff = extrudeOff;
//				extrudeOff = j > stopExtruding || j == p.size();
//				valveOff = j > stopValve || j == p.size();
//				
//				plot(p.point(i), next, extrudeOff, valveOff);
//				if(oldexoff ^ extrudeOff)
//					printer.printEndReverse();
//			}
//		} else
		
//		{
			for(int i = 1; i < p.size(); i++)
			{
				Rr2Point next = p.point((i+1)%p.size());
				
				if (printer.isCancelled())
				{
					printer.stopMotor();
					singleMove(posNow());
					move(posNow(), posNow(), lift, lift, true);
					return;
				}
				
				if(acc)
					currentFeedrate = p.speed(i);
				
				oldexoff = extrudeOff;
				extrudeOff = i > stopExtruding || i == p.size()-1;
				valveOff = i > stopValve || i == p.size()-1;
				
				plot(p.point(i), next, extrudeOff, valveOff);
				if(oldexoff ^ extrudeOff)
					printer.printEndReverse();
			}
//		}
		
		if(p.isClosed())
			move(p.point(0), p.point(0), false, false, true);
			
		move(posNow(), posNow(), lift, lift, true);
		
		// The last point is near where we want to start next
		if(p.isClosed())
			startNearHere = p.point(0);	
		else
			startNearHere = p.point(p.size() - 1);
		
		if(simulationPlot != null)
		{
			RrPolygonList pgl = new RrPolygonList();
			pgl.add(p);
			simulationPlot.add(pgl);
		}
		
	}
	


	private int plotOneMaterial(RrPolygonList polygons, int i, boolean firstOneInLayer)
		throws ReprapException, IOException
	{
		String material = polygons.polygon(i).getAttributes().getMaterial();
		
		while(i < polygons.size() && polygons.polygon(i).getAttributes().getMaterial().equals(material))
		{
			if (layerConditions.getPrinter().isCancelled())
				return i;
			plot(polygons.polygon(i), firstOneInLayer);
			firstOneInLayer = false;
			i++;
		}
		return i;
	}
	
	private boolean nextCommon(int ib, int ih)
	{
		if(borderPolygons == null || hatchedPolygons == null)
			return false;
		
		commonBorder = ib;
		commonHatch = ih;
		
		if(borderPolygons.size() <= 0)
		{
			commonHatch = hatchedPolygons.size();
			return false;
		}


		if(hatchedPolygons.size() <= 0)
		{
			commonBorder = borderPolygons.size();
			return false;
		}

		for(int jb = ib; jb < borderPolygons.size(); jb++)
		{
			for(int jh = ih; jh < hatchedPolygons.size(); jh++)
			{
				if(borderPolygons.polygon(ib).getAttributes().getMaterial().equals(
						hatchedPolygons.polygon(ih).getAttributes().getMaterial()))
				{
					commonBorder = jb;
					commonHatch = jh;
					return true;
				}
			}
		}
		return false;
	}
		
	/**
	 * Master plot function - draw everything.  Supress border and/or hatch by
	 * setting borderPolygons and/or hatchedPolygons null
	 * @throws IOException
	 * @throws ReprapException
	 */
	public void plot() throws ReprapException, IOException
	{
		int ib, jb, ih, jh;

		boolean firstOneInLayer = true;

		//borderPolygons = borderPolygons.filterShorts(Preferences.machineResolution()*2);
		//hatchedPolygons = hatchedPolygons.filterShorts(Preferences.machineResolution()*2);

		ib = 0;
		ih = 0;
		
		Printer printer = layerConditions.getPrinter();	
		
		while(nextCommon(ib, ih)) 
		{	
			for(jb = ib; jb < commonBorder; jb++)
			{
				if (printer.isCancelled())
					break;
				plot(borderPolygons.polygon(jb), firstOneInLayer);
				firstOneInLayer = false;
			}
			ib = commonBorder;

			for(jh = ih; jh < commonHatch; jh++)
			{
				if (printer.isCancelled())
					break;
				plot(hatchedPolygons.polygon(jh), firstOneInLayer);
				firstOneInLayer = false;
			}
			ih = commonHatch;

			firstOneInLayer = true;
			borderPolygons = borderPolygons.nearEnds(startNearHere);
			
			ib = plotOneMaterial(borderPolygons, ib, firstOneInLayer);
			firstOneInLayer = false;
			hatchedPolygons = hatchedPolygons.nearEnds(startNearHere);
			ih = plotOneMaterial(hatchedPolygons, ih, firstOneInLayer);	
		}

		firstOneInLayer = true; // Not sure about this - AB

		if(borderPolygons != null)
		{
			for(jb = ib; jb < borderPolygons.size(); jb++)
			{
				if (printer.isCancelled())
					break;
				plot(borderPolygons.polygon(jb), firstOneInLayer);
				firstOneInLayer = false;
			}
		}

		if(hatchedPolygons != null)
		{
			for(jh = ih; jh < hatchedPolygons.size(); jh++)
			{
				if (printer.isCancelled())
					break;
				plot(hatchedPolygons.polygon(jh), firstOneInLayer);
				firstOneInLayer = false;
			}
		}
		if(!shellSet)
		{
			printer.setLowerShell(lowerShell);
			shellSet = true;
		}
	}		
	
}
