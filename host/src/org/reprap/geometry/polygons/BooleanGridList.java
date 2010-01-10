package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.List;
import org.reprap.Attributes;

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
		public BooleanGrid shape(int i)
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
				add(aa.shape(i), aa.attribute(i));
		}

}
