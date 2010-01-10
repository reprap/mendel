package org.reprap.geometry.polygons;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.reprap.Attributes;
//import org.reprap.Extruder;
//import org.reprap.geometry.LayerRules;
//
///**
// * It's convenient to have lists of CSG polygons (even though they
// * can be multiple polygons themselves) so that you each entry
// * can be one collection of polygons per material (attribute).
// * 
// * @author Adrian
// *
// */
//public class RrCSGPolygonList {
//	/**
//	 * The list of polygons
//	 */
//	List<RrCSGPolygon> csgPolygons = null;
//	
//	/**
//	 * Flag to prevent cyclic graphs going round forever
//	 */
//	private boolean beingDestroyed = false;
//	
//	/**
//	 * Destroy me and all that I point to
//	 */
//	public void destroy() 
//	{
//		if(beingDestroyed) // Prevent infinite loop
//			return;
//		beingDestroyed = true;
//		if(csgPolygons != null)
//		{
//			for(int i = 0; i < csgPolygons.size(); i++)
//			{
//				csgPolygons.get(i).destroy();
//				csgPolygons.set(i, null);
//			}
//		}
//		csgPolygons = null;
//		beingDestroyed = false;
//	}
//	
//	/**
//	 * Destroy just me
//	 */
//	protected void finalize() throws Throwable
//	{
//		csgPolygons = null;
//		super.finalize();
//	}
//	
//	/**
//	 * 
//	 */
//	public RrCSGPolygonList()
//	{
//		csgPolygons = new ArrayList<RrCSGPolygon>();
//	}
//	
//	/**
//	 * Add polygon c
//	 * @param c
//	 */
//	public void add(RrCSGPolygon c)
//	{
//		csgPolygons.add(c);
//	}
//	
//	/**
//	 * Add polygon list ls
//	 * @param ls
//	 */
//	public void add(RrCSGPolygonList ls)
//	{
//		for(int i = 0; i < ls.size(); i ++)
//			csgPolygons.add(ls.get(i));
//	}
//	
//	/**
//	 * Get the ith polygon in the list
//	 * @param i
//	 * @return
//	 */
//	public RrCSGPolygon get(int i)
//	{
//		return csgPolygons.get(i);
//	}
//	
//	/**
//	 * How many polygons?
//	 * @return
//	 */
//	public int size()
//	{
//		return csgPolygons.size();
//	}
//	
//	/**
//	 * The minimum enclosing rectangle
//	 * @return
//	 */
//	public RrRectangle box()
//	{
//		RrRectangle result = new RrRectangle();
//		for(int i = 0; i < size(); i++)
//			result.expand(get(i).box());
//		return result;
//	}
//	
//	/**
//	 * Find the polygon in the list with a given attribute
//	 * @param a
//	 * @return
//	 */
//	public RrCSGPolygon find(Attributes a)
//	{
//		for(int i = 0; i < size(); i++)
//		{
//			if(get(i).getAttributes() == a)
//				return get(i);
//		}
//		return null;
//	}
//	
//	/**
//	 * Offset the lot by appropriate extruder widths
//	 * @param es
//	 * @param outline
//	 * @return
//	 */
//	public RrCSGPolygonList offset(LayerRules lc, boolean outline)
//	{
//		boolean foundation = lc.getLayingSupport();
//		if(outline && foundation)
//			System.err.println("Offsetting a foundation outline!");
//		
//		RrCSGPolygonList result = new RrCSGPolygonList();
//		for(int i = 0; i < size(); i++)
//		{
//			Attributes att = get(i).getAttributes();
//			if(att == null)
//				System.err.println("RrCSGPolygonList.offset(): null attribute!");
//			else
//			{
//				Extruder [] es = lc.getPrinter().getExtruders();
//				Extruder e;
//				int shells;
//				if(foundation)
//				{
//					e = es[0];  // By convention extruder 0 builds the foundation
//					shells = 1;
//				} else
//				{
//					e = att.getExtruder();
//					shells = e.getShells();					
//				}
//				if(outline)
//				{
//					for(int shell = 0; shell < shells; shell++)
//						result.add(get(i).offset(-((double)shell + 0.5)*e.getExtrusionSize()));
//				} else
//				{
//					// Must be a hatch.  Only do it if the gap is +ve or we're building the foundation
//					double offSize; 
//					if(foundation)
//						offSize = 3;
//					else
//						offSize = -((double)shells + 0.5)*e.getExtrusionSize() + e.getInfillOverlap();
//					if (e.getExtrusionInfillWidth() > 0 || foundation)  // Z valuesn't mattere here
//						result.add(get(i).offset(offSize));
//				}
//			}
//		}
//		return result;
//	}
//	
//	/**
//	 * Make a list with a single entry: the union of all the entries.
//	 * Set its attributes to that of extruder 0 in the extruder list.
//	 * @param a
//	 * @return
//	 */
//	public RrCSGPolygonList union(Extruder[] es)
//	{	
//		RrCSGPolygonList result = new RrCSGPolygonList();
//		if(size() <= 0)
//			return result;
//		RrCSG contents = get(0).csg();
//		RrRectangle b = get(0).box();
//		Attributes a = get(0).getAttributes();
//		Boolean foundAttribute0 = false;
//		if(a.getExtruder() == es[0])
//			foundAttribute0 = true;
//		for(int i = 1; i < size(); i++)
//		{
//			if(!foundAttribute0)
//			{
//				if(get(i).getAttributes().getExtruder() == es[0])
//				{
//					a = get(i).getAttributes();
//					foundAttribute0 = true;
//				}
//			}
//			contents = RrCSG.union(contents, get(i).csg());
//			b = RrRectangle.union(get(i).box(), b);
//		}
//		if(!foundAttribute0)
//			System.err.println("RrCSGPolygonList.union(): Attribute of extruder 0 not found.");
//		result.add(new RrCSGPolygon(contents, b, a));
//		return result;
//	}
//	
//	/**
//	 * Recursively divide the lot
//	 * @param res_2
//	 * @param swell
//	 */
//	public void divide(double res_2, double swell)
//	{
//		for(int i = 0; i < size(); i++)
//			get(i).divide(res_2, swell);
//	}
//	
//	/**
//	 * Generate boundary lists for the lot
//	 * @return
//	 */
//	public RrPolygonList megList()
//	{
//		RrPolygonList result = new RrPolygonList();
//		for(int i = 0; i < size(); i++)
//			result.add(get(i).megList());
//		return result;
//	}
//	
//	/**
//	 * Cross hatch the lot
//	 * @param hp
//	 * @param es
//	 * @return
//	 */
//	public RrPolygonList hatch(LayerRules lc) 
//	{
//		RrPolygonList result = new RrPolygonList();
//		boolean foundation = lc.getLayingSupport();
//		Extruder [] es = lc.getPrinter().getExtruders();
//		for(int i = 0; i < size(); i++)
//		{
//			Extruder e;
//			if(foundation)
//				e = es[0]; // Extruder 0 is used for foundations
//			else
//				e = get(i).getAttributes().getExtruder();
////			double width = lc.getHatchWidth(e);
////			if(foundation)
////				width = e.getExtrusionFoundationWidth();
////			else
////				width = e.getExtrusionInfillWidth();
//			result.add(get(i).hatch(lc.getHatchDirection(e), lc.getHatchWidth(e)));
//		}
//		return result;
//	}
//}
