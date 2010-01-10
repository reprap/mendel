package org.reprap.geometry;

import javax.swing.JCheckBoxMenuItem;
import org.reprap.Preferences;
import org.reprap.Printer;
import org.reprap.Extruder;
import org.reprap.Attributes;
import org.reprap.geometry.polygons.Rr2Point;
import org.reprap.geometry.polygons.RrRectangle;
import org.reprap.geometry.polygons.RrCSGPolygonList;
import org.reprap.geometry.polygons.STLSlice;
import org.reprap.geometry.polygons.RrCSG;
import org.reprap.geometry.polygons.RrCSGPolygon;
import org.reprap.geometry.polygons.RrPolygonList;
import org.reprap.gui.RepRapBuild;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;

public class Producer {
	
	private boolean paused = false;
	
	private LayerProducer layer = null;
	
	protected LayerRules layerRules = null;
	
	private RrGraphics simulationPlot = null;
	
	
	/**
	 * The list of objects to be built
	 */
	protected RepRapBuild bld;
	
//	protected boolean interLayerCooling;

	protected STLSlice stlc;
	
	/**
	 * @param preview
	 * @param builder
	 * @throws Exception
	 */
	public Producer(Printer pr, RepRapBuild builder) throws Exception 
	{
		bld = builder;	
		
		stlc = new STLSlice(bld.getSTLs());
		
		RrRectangle gp = stlc.ObjectPlanRectangle();

		if(Preferences.loadGlobalBool("DisplaySimulation"))
		{
			simulationPlot = new RrGraphics("RepRap building simulation");
		} else
			simulationPlot = null;
		
		double modZMax = stlc.maxZ();
		double stepZ = pr.getExtruders()[0].getExtrusionHeight();
		int foundationLayers = Math.max(0, pr.getFoundationLayers());
		
		int modLMax = (int)(modZMax/stepZ);
		
		layerRules = new LayerRules(pr, modZMax, modZMax + foundationLayers*stepZ,
				modLMax, modLMax + foundationLayers, true, gp);
	}
	
	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between segments.
	 * 
	 * @param segmentPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly. 
	 */
	public void setSegmentPause(JCheckBoxMenuItem segmentPause) {
		layerRules.getPrinter().setSegmentPause(segmentPause);
	}

	/**
	 * Set the source checkbox used to determine if there should
	 * be a pause between layers.
	 * 
	 * @param layerPause The source checkbox used to determine
	 * if there should be a pause.  This is a checkbox rather than
	 * a boolean so it can be changed on the fly.
	 */
	public void setLayerPause(JCheckBoxMenuItem layerPause) {
		layerRules.getPrinter().setLayerPause(layerPause);
	}
	
	public void setCancelled(boolean c)
	{
		layerRules.getPrinter().setCancelled(c);
	}
	
	public void pause()
	{
		paused = true;
		if(layer != null)
			layer.pause();
	}
	
	public void resume()
	{
		paused = false;
		if(layer != null)
			layer.resume();
	}
	
	/**
	 * NB - this does not call wait - this is a purely interactive function and
	 * does not control the machine
	 *
	 */
	private void waitWhilePaused()
	{
		while(paused)
		{
			try
			{
				Thread.sleep(200);
			} catch (Exception ex) {}
		}
	}
	
	public int getLayers()
	{
		return layerRules.getMachineLayerMax();
	}
	
	public int getLayer()
	{
		return layerRules.getMachineLayer();
	}	
	
	public void produce() throws Exception
	{
		RrRectangle gp = layerRules.getBox();
		
		gp = new RrRectangle(new Rr2Point(gp.x().low() - 6, gp.y().low() - 6), 
				new Rr2Point(gp.x().high() + 6, gp.y().high() + 6));
		
		
		layerRules.getPrinter().startRun();
		
		if(Preferences.loadGlobalBool("Subtractive"))
			produceSubtractive();
		else
		{
			if(layerRules.getTopDown())
				produceAdditiveTopDown(gp);
			else
				produceAdditiveGroundUp(gp);
		}
	}
	
	private void fillFoundationRectangle(Printer reprap, RrRectangle gp) throws Exception
	{
		RrCSG rect = RrCSG.RrCSGFromBox(gp);
		gp = gp.scale(1.1);
		Extruder e = reprap.getExtruder();
		RrCSGPolygon rcp = new RrCSGPolygon(rect, gp, new Attributes(e.getMaterial(), null, null, 
				e.getAppearance()));
		rcp.divide(Preferences.tiny(), 1.01);
		RrPolygonList h = rcp.hatch(layerRules.getHatchDirection(e), layerRules.getHatchWidth(e));
		LayerProducer lp = new LayerProducer(h, layerRules, simulationPlot);
		lp.plot();
		reprap.getExtruder().stopExtruding();
		//reprap.setFeedrate(reprap.getFastFeedrateXY());
	}
	
	private void layFoundationGroundUp(RrRectangle gp) throws Exception
	{
		if(layerRules.getFoundationLayers() <= 0)
			return;
		
		Printer reprap = layerRules.getPrinter();

		while(layerRules.getMachineLayer() < layerRules.getFoundationLayers()) 
		{
			
			if (reprap.isCancelled())
				break;
			waitWhilePaused();
			
			Debug.d("Commencing foundation layer at " + layerRules.getMachineZ());

			reprap.startingLayer(layerRules);
			// Change Z height
			//reprap.singleMove(reprap.getX(), reprap.getY(), layerRules.getMachineZ(), reprap.getFastFeedrateZ());
			fillFoundationRectangle(reprap, gp);
			reprap.finishedLayer(layerRules);
			reprap.betweenLayers(layerRules);
			layerRules.stepMachine(reprap.getExtruder());
		}
		layerRules.setLayingSupport(false);
	}
	
	private void layFoundationTopDown(RrRectangle gp) throws Exception
	{
		if(layerRules.getFoundationLayers() <= 0)
			return;
		
		layerRules.setLayingSupport(true);
		layerRules.getPrinter().setSeparating(false);
		
		Printer reprap = layerRules.getPrinter();
		
		while(layerRules.getMachineLayer() >= 0) 
		{
			
			if (reprap.isCancelled())
				break;
			waitWhilePaused();
			
			Debug.d("Commencing foundation layer at " + layerRules.getMachineZ());

			reprap.startingLayer(layerRules);
			// Change Z height
			//reprap.singleMove(reprap.getX(), reprap.getY(), layerRules.getMachineZ(), reprap.getFastFeedrateZ());
			fillFoundationRectangle(reprap, gp);		
			reprap.finishedLayer(layerRules);
			reprap.betweenLayers(layerRules);
			layerRules.stepMachine(reprap.getExtruder());
		}
	}
	

	
	/**
	 * @throws Exception
	 */
	private void produceAdditiveGroundUp(RrRectangle gp) throws Exception 
	{		
		bld.mouseToWorld();
		
		Printer reprap = layerRules.getPrinter();

		layFoundationGroundUp(gp);
		
		reprap.setSeparating(true);
		
		while(layerRules.getMachineLayer() < layerRules.getMachineLayerMax()) 
		{
			
			if (reprap.isCancelled())
				break;
			waitWhilePaused();
			
			Debug.d("Commencing layer at " + layerRules.getMachineZ());
			
			reprap.startingLayer(layerRules);
			
			// Change Z height
			//reprap.singleMove(reprap.getX(), reprap.getY(), layerRules.getMachineZ(), reprap.getFastFeedrateZ());
			
			reprap.waitWhileBufferNotEmpty();
			reprap.slowBuffer();
			
			RrCSGPolygonList slice = stlc.slice(layerRules.getModelZ() + layerRules.getZStep()*0.5,
					reprap.getExtruders()); 
			
			layer = null;
			if(slice.size() > 0)
				layer = new LayerProducer(slice, stlc.getBelow(), layerRules, simulationPlot);
			else
				Debug.d("Null slice at model Z = " + layerRules.getModelZ());

						
			if (reprap.isCancelled())
				break;		
			waitWhilePaused();
			
			reprap.speedBuffer();
			
			if(layer != null)
			{
				layer.plot();
				layer.destroy();
			} else
				Debug.d("Null layer at model Z = " + layerRules.getModelZ());
			
			reprap.finishedLayer(layerRules);
			reprap.betweenLayers(layerRules);
			layer = null;
			
			slice.destroy();
			stlc.destroyLayer();

			layerRules.step(reprap.getExtruder());
			reprap.setSeparating(false);
		}
		
		reprap.terminate();
	}
	
	/**
	 * @throws Exception
	 */
	private void produceAdditiveTopDown(RrRectangle gp) throws Exception 
	{		
		bld.mouseToWorld();
		
		Printer reprap = layerRules.getPrinter();
		
		layerRules.setLayingSupport(false);
		
		
		while(layerRules.getModelLayer() >= 0 ) 
		{
			if(layerRules.getModelLayer() == 0)
				reprap.setSeparating(true);
			else
				reprap.setSeparating(false);
			
			if (reprap.isCancelled())
				break;
			waitWhilePaused();
			
			Debug.d("Commencing model layer " + layerRules.getModelLayer() + " at " + layerRules.getMachineZ());
			
			reprap.startingLayer(layerRules);
			
			// Change Z height
			//layerRules.moveZAtStartOfLayer();
			//reprap.singleMove(reprap.getX(), reprap.getY(), layerRules.getMachineZ(), reprap.getFastFeedrateZ());
			
			reprap.waitWhileBufferNotEmpty();
			reprap.slowBuffer();
			
			RrCSGPolygonList slice = stlc.slice(layerRules.getModelZ() + layerRules.getZStep()*0.5,
					reprap.getExtruders()); 
			
			layer = null;
			if(slice.size() > 0)
				layer = new LayerProducer(slice, stlc.getBelow(), layerRules, simulationPlot);
			else
				Debug.d("Null slice at model Z = " + layerRules.getModelZ());

						
			if (reprap.isCancelled())
				break;		
			waitWhilePaused();
			
			reprap.speedBuffer();
			
			if(layer != null)
			{
				layer.plot();
				//layer.destroy();
			} else
				Debug.d("Null layer at model Z = " + layerRules.getModelZ());
			
			reprap.finishedLayer(layerRules);
			reprap.betweenLayers(layerRules);
			layer = null;
			
			slice.destroy();
			stlc.destroyLayer();

			layerRules.step(reprap.getExtruder());
		}
		
		layFoundationTopDown(gp);
		
		reprap.terminate();
	}
	

	private void produceSubtractive() throws Exception 
	{
		System.err.println("Need to implement the Producer.produceSubtractive() function... :-)");
	}

	/**
	 * The total distance moved is the total distance extruded plus 
	 * plus additional movements of the extruder when no materials 
	 * was deposited
	 * 
	 * @return total distance the extruder has moved 
	 */
	public double getTotalDistanceMoved() {
		return layerRules.getPrinter().getTotalDistanceMoved();
	}
	
	/**
	 * @return total distance that has been extruded in millimeters
	 */
	public double getTotalDistanceExtruded() {
		return layerRules.getPrinter().getTotalDistanceExtruded();
	}
	
	/**
	 * TODO: This figure needs to get added up as we go along to allow for different extruders
	 * @return total volume that has been extruded
	 */
	public double getTotalVolumeExtruded() {
		return layerRules.getPrinter().getTotalDistanceExtruded() * layerRules.getPrinter().getExtruder().getExtrusionHeight() * 
		layerRules.getPrinter().getExtruder().getExtrusionSize();
	}
	
	/**
	 * 
	 */
	public void dispose() {
		layerRules.getPrinter().dispose();
	}

	/**
	 * @return total elapsed time in seconds between start and end of building the 3D object
	 */
	public double getTotalElapsedTime() {
		return layerRules.getPrinter().getTotalElapsedTime();
	}
	
}
