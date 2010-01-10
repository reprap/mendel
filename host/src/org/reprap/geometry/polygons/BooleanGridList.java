package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.List;
import org.reprap.Attributes;
import org.reprap.Extruder;
import org.reprap.geometry.LayerRules;
import org.reprap.utilities.Debug;

/**
 * Class to hold a list of BooleanGrids with associated atributes for each
 * @author ensab
 *
 */

public class BooleanGridList 
{

		private List<BooleanGrid> shapes = null;
		private List<Attributes> a = null;
		
		protected void finalize() throws Throwable
		{
			shapes = null;
			a = null;
			super.finalize();
		}
		
		public BooleanGridList()
		{
			a = new ArrayList<Attributes>();
			shapes = new ArrayList<BooleanGrid>();
		}
		
		/**
		 * Return the ith shape
		 * @param i
		 * @return
		 */
		public BooleanGrid get(int i)
		{
			return shapes.get(i);
		}
		
		/**
		 * Return the ith attribute
		 * @param i
		 * @return
		 */
		public Attributes attribute(int i)
		{
			return a.get(i);
		}
		
		/**
		 * How many shapes are there in the list?
		 * @return
		 */
		public int size()
		{
			return shapes.size();
		}
		
		/**
		 * Remove an entry and close the gap
		 * @param i
		 */
		public void remove(int i)
		{
			a.remove(i);
			shapes.remove(i);
		}

		
		/**
		 * Add a shape on the end
		 * @param p
		 */
		public void add(BooleanGrid b, Attributes aa)
		{
			a.add(aa);
			shapes.add(b);
		}
		
		/**
		 * Add another list of shapes on the end
		 * @param a
		 */
		public void add(BooleanGridList aa)
		{
			for(int i = 0; i < a.size(); i++)
				add(aa.get(i), aa.attribute(i));
		}
		
		public BooleanGridList offset(LayerRules lc, boolean outline)
		{
			boolean foundation = lc.getLayingSupport();
			if(outline && foundation)
				Debug.e("Offsetting a foundation outline!");
			
			BooleanGridList result = new BooleanGridList();
			for(int i = 0; i < size(); i++)
			{
				Attributes att = attribute(i);
				if(att == null)
					Debug.e("BooleanGridList.offset(): null attribute!");
				else
				{
					Extruder [] es = lc.getPrinter().getExtruders();
					Extruder e;
					int shells;
					if(foundation)
					{
						e = es[0];  // By convention extruder 0 builds the foundation
						shells = 1;
					} else
					{
						e = att.getExtruder();
						shells = e.getShells();					
					}
					if(outline)
					{
						for(int shell = 0; shell < shells; shell++)
							result.add(get(i).offset(-((double)shell + 0.5)*e.getExtrusionSize()), att);
					} else
					{
						// Must be a hatch.  Only do it if the gap is +ve or we're building the foundation
						double offSize; 
						if(foundation)
							offSize = 3;
						else
							offSize = -((double)shells + 0.5)*e.getExtrusionSize() + e.getInfillOverlap();
						if (e.getExtrusionInfillWidth() > 0 || foundation)  // Z valuesn't mattere here
							result.add(get(i).offset(offSize), att);
					}
				}
			}
			return result;			
		}
		
		/**
		 * Work out all the polygons forming a set of borders
		 * @return
		 */
		public RrPolygonList borders()
		{
			RrPolygonList result = new RrPolygonList();
			for(int i = 0; i < size(); i++)
				result.add(get(i).allPerimiters(attribute(i))); 
			return result;
		}
		
		/**
		 * Work out all the open polygond forming a set of infill hatches
		 * @param layerConditions
		 * @return
		 */
		public RrPolygonList hatch(LayerRules layerConditions)
		{
			RrPolygonList result = new RrPolygonList();
			boolean foundation = layerConditions.getLayingSupport();
			Extruder [] es = layerConditions.getPrinter().getExtruders();
			for(int i = 0; i < size(); i++)
			{
				Extruder e;
				if(foundation)
					e = es[0]; // Extruder 0 is used for foundations
				else
					e = attribute(i).getExtruder();
				result.add(get(i).hatch(layerConditions.getHatchDirection(e), layerConditions.getHatchWidth(e), attribute(i)));
			}
			return result;
		}
		
		/**
		 * Make a list with a single entry: the union of all the entries.
		 * Set its attributes to that of extruder 0 in the extruder list.
		 * @param a
		 * @return
		 */
		public BooleanGridList union(Extruder[] es)
		{	
			BooleanGridList result = new BooleanGridList();
			if(size() <= 0)
				return result;
			
			BooleanGrid contents = get(0);
			
			Attributes att = attribute(0);
			Boolean foundAttribute0 = false;
			if(att.getExtruder() == es[0])
				foundAttribute0 = true;
			for(int i = 1; i < size(); i++)
			{
				if(!foundAttribute0)
				{
					if(attribute(i).getExtruder() == es[0])
					{
						att = attribute(i);
						foundAttribute0 = true;
					}
				}
				contents = BooleanGrid.union(contents, get(i));
			}
			if(!foundAttribute0)
				Debug.e("RrCSGPolygonList.union(): Attribute of extruder 0 not found.");
			result.add(contents, att);
			return result;
		}

}
