/*
 
 RepRap
 ------
 
 The Replicating Rapid Prototyper Project
 
 
 Copyright (C) 2005
 Adrian Bowyer & The University of Bath
 
 http://reprap.org
 
 Principal author:
 
 Adrian Bowyer
 Department of Mechanical Engineering
 Faculty of Engineering and Design
 University of Bath
 Bath BA2 7AY
 U.K.
 
 e-mail: A.Bowyer@bath.ac.uk
 
 RepRap is free; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 Licence as published by the Free Software Foundation; either
 version 2 of the Licence, or (at your option) any later version.
 
 RepRap is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public Licence for more details.
 
 For this purpose the words "software" and "library" in the GNU Library
 General Public Licence are taken to mean any and all computer programs
 computer files data results documents and other copyright information
 available from the RepRap project.
 
 You should have received a copy of the GNU Library General Public
 Licence along with RepRap; if not, write to the Free
 Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA,
 or see
 
 http://www.gnu.org/
 
 =====================================================================
 
 
 STLSlice: deals with the slices through an STL object
 
 */

package org.reprap.geometry.polygons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import org.reprap.gui.STLObject;
import org.reprap.utilities.Debug;
import org.reprap.utilities.RrGraphics;
import org.reprap.Attributes;
import org.reprap.Extruder;
import org.reprap.Preferences;
import org.reprap.devices.GenericExtruder;

// Small class to hold line segments and the quads in which their ends lie

class LineSegment
{	
	/**
	 * The ends of the line segment
	 */
	public Rr2Point a = null, b = null;
	
	/**
	 * The quads that the ends lie in
	 */
	public STLSlice qa = null, qb = null;
	
	/**
	 * The attribute (i.e. RepRap material) of the segment.
	 */
	public Attributes att = null;
	
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
		if(a != null)
			a.destroy();
		a = null;
		if(b != null)
			b.destroy();
		b = null;
		if(qa != null)
			qa.destroyLayer();
		qa = null;
		if(qb != null)
			qb.destroyLayer();
		qb = null;
		
		// Don't destroy the attribute - it may be needed
		
		//if(att != null)
		//	att.destroy();
		att = null;
		
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		a = null;
		b = null;
		qa = null;
		qb = null;
		att = null;
		super.finalize();
	}
	
	/**
	 * Constructor takes two intersection points with an STL triangle edge.
	 * @param p
	 * @param q
	 */
	public LineSegment(Rr2Point p, Rr2Point q, Attributes at)
	{
		if(at == null)
			System.err.println("LineSegment(): null attributes!");
		a = p;
		b = q;
		att = at;
		qa = null;
		qb = null;
	}
	
	public String toString()
	{
		return "edge ends: (" + a.toString() + ") to (" + b.toString() + ")"; 
	}

	/**
	 * A quad contains (we hope...) the ends of two segments - record that
	 * @param q
	 */
	public static void setQuad(STLSlice q)
	{
		if(q.edges().size() != 2)
			Debug.d("LineSegment.setQuad(): dud edge count: " + q.edges().size());
		
		int count = 0;
		
		for(int i = 0; i < q.edges().size(); i++)
		{
			if(q.box().pointRelative(q.segmentA(i)) == 0)
			{
				q.segment(i).qa = q;
				count++;
			}
			if(q.box().pointRelative(q.segmentB(i)) == 0)
			{
				q.segment(i).qb = q;
				count++;
			}
		}
		
		//if(count != 2)
			//System.err.println("LineSegment.setQuad(): dud end count = " + count);
	}
}

/**
 * Small holder for quads and points as they get visited
 * @author ensab
 *
 */
class trackPolygon
{
	public STLSlice nextQ = null;
	public LineSegment nextE = null;
	public Rr2Point here = null;
	
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
		if(nextQ != null)
			nextQ.destroyLayer();
		nextQ = null;
		if(nextE != null)
			nextE.destroy();
		nextE = null;
		if(here != null)
			here.destroy();
		here = null;
		
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		nextQ = null;
		nextE = null;
		here = null;
		super.finalize();
	}
	
	public trackPolygon()
	{
		nextQ = null;
		nextE = null;
		here = null;
	}
}

/**
 * Very small class to hold attributes (i.e. material made from) and transforms for
 * the objects made from them.
 * @author ensab
 *
 */
class AandT
{
	public Attributes att = null;
	public Transform3D trans = null;
	
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
		
		// Both the attribute and transform may be needed again
		
		if(att != null)
		//	att.destroy();
		att = null;
		//if(trans != null)
		//	trans.destroy();
		trans = null;
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		att = null;
		trans = null;
		super.finalize();
	}
	
	public AandT(Attributes a, Transform3D t)
	{
		att = a;
		trans = t;
	}
}

/**
 * list of materials and transforms of the objects made from them.
 * @author ensab
 *
 */
class MaterialLists
{
	private ArrayList<AandT> ats[] = null;
	int extruderCount;
	
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
		if(ats != null)
		{
			for(int i = 0; i < ats.length; i++)
			{
				for(int j = 0; j < ats[i].size(); j++)
				{
					ats[i].get(j).destroy();
					ats[i].set(j, null);
				}
				ats[i] = null;
			}
			
		}
		ats = null;
	}
		
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		ats = null;
		super.finalize();
	}	
	
	@SuppressWarnings("unchecked")
	public MaterialLists()
	{
		extruderCount = 0;
		
		try
		{
			extruderCount = Preferences.loadGlobalInt("NumberOfExtruders");
		} catch (Exception ex)
		{
			System.err.println("MaterialLists(): " + ex.toString());
		}
		
		ats = new ArrayList[extruderCount]; // Javanonsense: Why can't this be ats = new ArrayList<AandT>[extruderCount]; then?
		for(int i = 0; i < extruderCount; i++)
			ats[i] = new ArrayList<AandT>();
	}
	
	public void add(Attributes a, Transform3D t)
	{
		int i = GenericExtruder.getNumberFromMaterial(a.getMaterial());
		if(i < 0 || i >= extruderCount)
			System.err.println("MaterialLists.add() - dud material: " + a.getMaterial());
		else
			ats[i].add(new AandT(a, t));
	}
	
	public ArrayList<AandT> getAandTs(int i)
	{
		return ats[i];
	}
	
	public int getExtruderCount() { return extruderCount; }
}

/**
 * Class to hold all the STL objects in the scene and to compute slices through them.
 * The slices are computed by dividing a rectangle (box) in a quad tree down to the point
 * where each quad contains two ends of different line segments.  These ends are then assumed
 * to join together.
 */
public class STLSlice 
{
	/**
	 * For debugging
	 */
	private static RrGraphics picture = null;
	
	/**
	 * Used to make unbiased but arbitrary decisions
	 */
	private static Random rangen = new Random(739127);
	
	/**
	 * The STL objects in 3D
	 */
	private List<STLObject> shapeList = null;
	
	/**
	 * List of the edges with points in this quad
	 */
	private List<LineSegment> edges = null;
	
	/**
	 * Its enclosing box
	 */
	private RrRectangle box = null;
	
	/**
	 * Quad tree division - NW, NE, SE, SW
	 */
	private STLSlice q1 = null, q2 = null, q3 = null, q4 = null;
	
	/**
	 * Lists of the x coordinates of the segment endpoints in this quad
	 */
	private ArrayList<Double> xCoords = null;
	
	/**
	 * Lists of the y coordinates of the segment endpoints in this quad
	 */	
	private ArrayList<Double> yCoords = null;
	
	/**
	 * Squared diagonal of the smallest box to go to 
	 */
	private static double resolution_2 = Preferences.tiny();
	
	/**
	 * Swell factor for division
	 */
	private static double sFactor = 1;
	
	/**
	 * All the STL triangles and part-triangles below slice-height, Z 
	 */
	private List<Point3d> triangles = null;
	
	/**
	 * Made from the below-Z triangles 
	 */
	private BranchGroup below = null;
	
	/**
	 * The lists of parts sorted by material
	 */
	private MaterialLists materialList = null;
	/**
	 * For debugging
	 */
	RrGraphics qp = null;
	
	/**
	 * Do we need the lower shell for the simulation window?
	 */
	private boolean generateLowerTriangles = true;
	
	/**
	 * Flag to prevent cyclic graphs going round forever
	 */
	private boolean beingDestroyed = false;
	
	/**
	 * Destroy me and (almost) all that I point to,
	 * setting things up for the next layer
	 */
	public void destroyLayer() 
	{
		if(beingDestroyed) // Prevent infinite loop
			return;
		beingDestroyed = true;
		
		// Don't destroy the shapeList - we need it for the next layer
		
//		if(shapeList != null)
//		{
//			for(int i = 0; i < shapeList.size(); i++)
//			{
//				shapeList.get(i).destroy();
//				shapeList.set(i, null);
//			}
//		}
//		shapeList = null;
		
		if(edges != null)
		{
			for(int i = 0; i < edges.size(); i++)
			{
				edges.get(i).destroy();
				edges.set(i, null);
			}
		}
		edges = null;
		if(box != null)
			box.destroy();
		box = null;
		if(q1 != null)
			q1.destroyLayer();
		q1 = null;
		if(q2 != null)
			q2.destroyLayer();
		q2 = null;
		if(q3 != null)
			q3.destroyLayer();
		q3 = null;
		if(q4 != null)
			q4.destroyLayer();
		q4 = null;
		xCoords = null;
		yCoords = null;
		triangles = null;
		
		// We need below
		//below = null;
		
		// We need the material lists next time
		
		//if(materialList != null)
		//	materialList.destroy();
		//materialList = null;
		
		qp = null;		
		
		edges = new ArrayList<LineSegment>();
		xCoords = new ArrayList<Double>();
		yCoords = new ArrayList<Double>();
		box = new RrRectangle();
		triangles = new ArrayList<Point3d>();
		beingDestroyed = false;
	}
	
	/**
	 * Destroy just me
	 */
	protected void finalize() throws Throwable
	{
		shapeList = null;
		edges = null;
		box = null;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		xCoords = null;
		yCoords = null;
		triangles = null;
		below = null;
		materialList = null;
		qp = null;				
		super.finalize();
	}	
	
	/**
	 * Initialises a few things
	 */
	private void setUp()
	{
		edges = new ArrayList<LineSegment>();
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		xCoords = new ArrayList<Double>();
		yCoords = new ArrayList<Double>();
		box = new RrRectangle();
		triangles = new ArrayList<Point3d>();
	}
	
	/**
	 * This constructor records the list of STL objects. 
	 * @param s
	 */
	public STLSlice(List<STLObject> s)
	{
		setUp();
		shapeList = s;
		materialList = new MaterialLists();
		try
		{
			generateLowerTriangles = false; //Preferences.loadGlobalBool("DisplaySimulation");
		} catch (Exception e)
		{}
		
		// For each object, record its material and transform
		
		for(int i = 0; i < shapeList.size(); i++)
		{
			STLObject stl = shapeList.get(i);
			Transform3D trans = stl.getTransform();
			//for(int c = 0; c < stl.numChildren(); c++)
			//{
				BranchGroup bg = stl.getSTL();
				java.util.Enumeration<?> enumKids = bg.getAllChildren();

				while(enumKids.hasMoreElements())
				{
					Object ob = enumKids.nextElement();

					if(ob instanceof BranchGroup)
					{
						BranchGroup bg1 = (BranchGroup)ob;
						Attributes att = (Attributes)(bg1.getUserData());
						materialList.add(att, trans);
					}
				}
			//}
		}
	}
	
	/**
	 * This quad is a leaf if it has no children; just check the first.
	 * @return
	 */
	public boolean leaf()
	{
		return q1 == null;
	}
	
	/**
	 * Add a new line segment to the list.  Also add its end coordinates to
	 * the lists of those.
	 * @param p
	 * @param q
	 */
	public void add(Rr2Point p, Rr2Point q, Attributes att)
	{
		xCoords.add(new Double(p.x()));
		xCoords.add(new Double(q.x()));
		yCoords.add(new Double(p.y()));
		yCoords.add(new Double(q.y()));
		
		edges.add(new LineSegment(p, q, att));
	}
	
	/**
	 * Return the box
	 * @return 
	 */
	public RrRectangle box()
	{
		return box;
	}
	

	/**
	 * Run through a Shape3D and find its enclosing XY box
	 * @param shape
	 * @param trans
	 * @param z
	 */
	private RrRectangle BBoxPoints(Shape3D shape, Transform3D trans)
    {
		RrRectangle r = null;
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p1 = new Point3d();
        Point3d q1 = new Point3d();
        
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i++) 
            {
                g.getCoordinate(i, p1);
                trans.transform(p1, q1);
                if(r == null)
                	r = new RrRectangle(new RrInterval(q1.x, q1.x), new RrInterval(q1.y, q1.y));
                else
                	r.expand(new Rr2Point(q1.x, q1.y));
            }
        }
        return r;
    }
	
	/**
	 * Unpack the Shape3D(s) from value and find their exclosing XY box
	 * @param value
	 * @param trans
	 * @param z
	 */
	private RrRectangle BBox(Object value, Transform3D trans) 
    {
		RrRectangle r = null;
		RrRectangle s;
		
        if(value instanceof SceneGraphObject) 
        {
            SceneGraphObject sg = (SceneGraphObject)value;
            if(sg instanceof Group) 
            {
                Group g = (Group)sg;
                java.util.Enumeration<?> enumKids = g.getAllChildren( );
                while(enumKids.hasMoreElements())
                {
                	if(r == null)
                		r = BBox(enumKids.nextElement(), trans);
                	else
                	{
                		s = BBox(enumKids.nextElement(), trans);
                		if(s != null)
                			r = RrRectangle.union(r, s);
                	}
                }
            } else if (sg instanceof Shape3D) 
            {
                r = BBoxPoints((Shape3D)sg, trans);
            }
        }
        
        return r;
    }
	
	/**
	 * Find the minimum XY enclosing box round the corners of the object
	 * @return
	 */
	public RrRectangle ObjectPlanRectangle()
	{
		RrRectangle r = null;
		RrRectangle s;
		
		for(int mat = 0; mat < materialList.getExtruderCount(); mat++)
		{
			ArrayList<AandT> aats = materialList.getAandTs(mat);

			if(aats.size() > 0)
			{
				for(int obj = 0; obj < aats.size(); obj++)
				{
					AandT aat = aats.get(obj);
					Transform3D trans = aat.trans;
					Attributes attr = aat.att;
					if(r == null)
						r = BBox(attr.getPart(), trans);
					else
					{
						s = BBox(attr.getPart(), trans);
						if(s != null)
						r = RrRectangle.union(r, s);
					}
				}
			}
		}
		
		return r;
	}
//		//BoundingBox r = null;
//		
//		for(int mat = 0; mat < materialList.getExtruderCount(); mat++)
//		{
//			ArrayList<AandT> aats = materialList.getAandTs(mat);
//
//			if(aats.size() > 0)
//			{
//				for(int obj = 0; obj < aats.size(); obj++)
//				{
//					AandT aat = aats.get(obj);
//					Transform3D trans = aat.trans;
//					Attributes attr = aat.att;
//					BranchGroup bg = attr.getPart();
//					Bounds bd = bg.getBounds();
//					bd.transform(trans);
//					BoundingBox bx = new BoundingBox(bd);
//					if(r == null)
//						r = bx;
//					else
//						r.combine(bx);
//				}
//			}
//		}
//		Point3d p1 = new Point3d();
//		r.getLower(p1); 
//		Point3d p2 = new Point3d();
//		r.getUpper(p2);
//		return new RrRectangle(new Rr2Point(p1.x, p1.y), new Rr2Point(p2.x, p2.y));
//	}

	
	/**
	 * @return the edges of the STL slice
	 */
	public List<LineSegment> edges()
	{
		return edges;
	}
	
	/**
	 * get the quad children
	 * @return
	 */
	public STLSlice c_1()
	{
		return q1;
	}
	public STLSlice c_2()
	{
		return q2;
	}
	public STLSlice c_3()
	{
		return q3;
	}
	public STLSlice c_4()
	{
		return q4;
	}
	
	/**
	 * @param i index of line segement
	 * @return Linesegment object of the STL slice at index i
	 */
	protected LineSegment segment(int i)
	{
		return edges.get(i);
	}
	
	private RrPolygon getNextPolygon()
	{
		if(edges.size() <= 0)
			return null;
		LineSegment next = segment(0);
		edges.remove(0);
		RrPolygon result = new RrPolygon(next.att, true);
		result.add(next.a);
		result.add(next.b);
		Rr2Point start = next.a;
		Rr2Point end = next.b;
		
		while(edges.size() > 0)
		{
			double d2 = Rr2Point.dSquared(start, end);
			boolean aEnd = false;
			int index = -1;
			for(int i = 0; i < edges.size(); i++)
			{
				double dd = Rr2Point.dSquared(segment(i).a, end);
				if(dd < d2)
				{
					d2 = dd;
					aEnd = true;
					index = i;
				}
				dd = Rr2Point.dSquared(segment(i).b, end);
				if(dd < d2)
				{
					d2 = dd;
					aEnd = false;
					index = i;
				}
			}

			if(index >= 0)
			{
				next = edges.get(index);
				edges.remove(index);
				int ipt = result.size() - 1;
				if(aEnd)
				{
					result.set(ipt, Rr2Point.mul(Rr2Point.add(next.a, result.point(ipt)), 0.5));
					result.add(next.b);
					end = next.b;
				} else
				{
					result.set(ipt, Rr2Point.mul(Rr2Point.add(next.b, result.point(ipt)), 0.5));
					result.add(next.a);
					end = next.a;				
				}
			} else
				return result;
		}
		
		Debug.d("STLSlice.getNextPolygon(): exhausted edge list!");
		
		return result;
	}
	
	private RrPolygonList simpleCull()
	{
		RrPolygonList result = new RrPolygonList();
		RrPolygon next = getNextPolygon();
		while(next != null)
		{
			if(next.size() >= 3)
				result.add(next);
			next = getNextPolygon();
		}
		
		return result;
	}
	
	public Rr2Point segmentA(int i)
	{
		return edges.get(i).a;
	}
	
	public Rr2Point segmentB(int i)
	{
		return edges.get(i).b;
	}
	
	/**
	 * Get the triangulation below the current slice level.
	 * @return
	 */
	public BranchGroup getBelow()
	{
		if(generateLowerTriangles)
			return below;
		else
			return null;
	}
	
	/**
	 * FIXME: Not sure about this - at the moment it clicks all points
	 * onto an 0.01 mm grid.
	 * @param x
	 * @return grid value nearest x
	 */
	private double toGrid(double x)
	{
		return x;
		//return (double)((int)(x*Preferences.grid() + 0.5))*Preferences.gridRes();
	}
	
	/**
	 * Add the edge where the plane z cuts the triangle (p, q, r) (if it does).
	 * Also update the triangulation of the object below the current slice used
	 * for the simulation window.
	 * @param p
	 * @param q
	 * @param r
	 * @param z
	 */
	private void addEdge(Point3d p, Point3d q, Point3d r, double z, Attributes att)
	{
		Point3d odd = null, even1 = null, even2 = null;
		int pat = 0;
		boolean twoBelow = false;
		
		if(p.z < z)
			pat = pat | 1;
		if(q.z < z)
			pat = pat | 2;
		if(r.z < z)
			pat = pat | 4;
		
		switch(pat)
		{
		// All above
		case 0:
			return;
			
		// All below
		case 7:
			if(generateLowerTriangles)
			{
				triangles.add(new Point3d(p));
				triangles.add(new Point3d(q));
				triangles.add(new Point3d(r));
			}
			return;
			
		// q, r below, p above	
		case 6:
			twoBelow = true;
		// p below, q, r above
		case 1:
			odd = p;
			even1 = q;
			even2 = r;
			break;
			
		// p, r below, q above	
		case 5:
			twoBelow = true;
		// q below, p, r above	
		case 2:
			odd = q;
			even1 = r;
			even2 = p;
			break;

		// p, q below, r above	
		case 3:
			twoBelow = true;
		// r below, p, q above	
		case 4:
			odd = r;
			even1 = p;
			even2 = q;
			break;
			
		default:
			System.err.println("addEdge(): the | function doesn't seem to work...");
		}
		
		// Work out the intersection line segment (e1 -> e2) between the z plane and the triangle
		
		even1.sub((Tuple3d)odd);
		even2.sub((Tuple3d)odd);
		double t = (z - odd.z)/even1.z;	
		Rr2Point e1 = new Rr2Point(odd.x + t*even1.x, odd.y + t*even1.y);	
		Point3d e3_1 = new Point3d(e1.x(), e1.y(), z);
		e1 = new Rr2Point(toGrid(e1.x()), toGrid(e1.y()));
		t = (z - odd.z)/even2.z;
		Rr2Point e2 = new Rr2Point(odd.x + t*even2.x, odd.y + t*even2.y);
		Point3d e3_2 = new Point3d(e2.x(), e2.y(), z);
		e2 = new Rr2Point(toGrid(e2.x()), toGrid(e2.y()));
		
		// Too short?
		if(!Rr2Point.same(e1, e2, Preferences.lessGridSquare()))
		{
			add(e1, e2, att);
			box.expand(e1);
			box.expand(e2);
		}
		
		// Sort out the bits of triangle to add to the shape under the z plane
		
		if(twoBelow)
		{
			even1.add((Tuple3d)odd);
			even2.add((Tuple3d)odd);
			if(generateLowerTriangles)
			{
				triangles.add(new Point3d(even1));
				triangles.add(new Point3d(even2));
				triangles.add(new Point3d(e3_1));
				triangles.add(new Point3d(e3_2));
				triangles.add(new Point3d(e3_1));
				triangles.add(new Point3d(even2));
			}
		} else if(generateLowerTriangles)
		{
			triangles.add(new Point3d(odd));
			triangles.add(new Point3d(e3_1));
			triangles.add(new Point3d(e3_2));
		}
	}
	

	
	/**
	 * Run through a Shape3D and set edges from it at plane z
	 * Apply the transform first
	 * @param shape
	 * @param trans
	 * @param z
	 */
	private void addAllEdges(Shape3D shape, Transform3D trans, double z, Attributes att)
    {
        GeometryArray g = (GeometryArray)shape.getGeometry();
        Point3d p1 = new Point3d();
        Point3d p2 = new Point3d();
        Point3d p3 = new Point3d();
        Point3d q1 = new Point3d();
        Point3d q2 = new Point3d();
        Point3d q3 = new Point3d();
        
        if(g.getVertexCount()%3 != 0)
        {
        	System.err.println("addAllEdges(): shape3D with vertices not a multiple of 3!");
        }
        if(g != null)
        {
            for(int i = 0; i < g.getVertexCount(); i+=3) 
            {
                g.getCoordinate(i, p1);
                g.getCoordinate(i+1, p2);
                g.getCoordinate(i+2, p3);
                trans.transform(p1, q1);
                trans.transform(p2, q2);
                trans.transform(p3, q3);
                addEdge(q1, q2, q3, z, att);
            }
        }
    }
	
	/**
	 * Unpack the Shape3D(s) from value and set edges from them
	 * @param value
	 * @param trans
	 * @param z
	 */
	private void recursiveSetEdges(Object value, Transform3D trans, double z, Attributes att) 
    {
        if(value instanceof SceneGraphObject) 
        {
            SceneGraphObject sg = (SceneGraphObject)value;
            if(sg instanceof Group) 
            {
                Group g = (Group)sg;
                java.util.Enumeration<?> enumKids = g.getAllChildren( );
                while(enumKids.hasMoreElements())
                    recursiveSetEdges(enumKids.nextElement(), trans, z, att);
            } else if (sg instanceof Shape3D) 
            {
                addAllEdges((Shape3D)sg, trans, z, att);
            }
        }
    }
	
	/**
	 * Constructor for building a branch quad in the division
	 * @param pgl
	 * @param b
	 * @param res
	 * @param fac
	 */
	private STLSlice(List<LineSegment> pgl, RrRectangle b)
	{
		edges = pgl;
		box = b;
		q1 = null;
		q2 = null;
		q3 = null;
		q4 = null;
		xCoords = new ArrayList<Double>();
		yCoords = new ArrayList<Double>();
	}
	
	/**
	 * Prune the edge list to the box so that only segments
	 * with endpoints in the box are retained.  
	 */
	private void prune()
	{
		List<LineSegment> result = new ArrayList<LineSegment>();
		
		for(int i = 0; i < edges.size(); i++)
		{
			Rr2Point aa = segment(i).a;
			Rr2Point bb = segment(i).b;
			
			boolean aIn = (box.pointRelative(aa) == 0);
			boolean bIn = (box.pointRelative(bb) == 0);
			
			if(aIn || bIn)
				result.add(segment(i));

			if(aIn)
			{
				xCoords.add(new Double(aa.x()));
				yCoords.add(new Double(aa.y()));
			}
			
			if(bIn)
			{
				xCoords.add(new Double(bb.x()));
				yCoords.add(new Double(bb.y()));
			}
		}
				
		edges = result;
	}
	
	/**
	 * Start near the middle of a sorted list of coordinates, working both
	 * up and down and find the first decent-sized gap
	 * @param coords
	 * @param i
	 * @return
	 */
	private static double findGap(ArrayList<Double> coords, RrInterval i)
	{
		double g, v;
		
		Collections.sort(coords);
		
		int middle = coords.size()/2;
		double vpOld = coords.get(middle).doubleValue();
		double vmOld = vpOld;
		int p = middle + 1;
		int m = middle - 1;
		RrInterval biggest = new RrInterval(0, 0);
		
		while(p < coords.size() || m  >= 0)
		{
			if(p < coords.size())
			{
				v = coords.get(p).doubleValue();
				g = v - vpOld;
				if(g > Preferences.gridRes()*0.1)
				{
					return 0.5*(vpOld + v);
				}
				if(g > biggest.length())
					biggest = new RrInterval(vpOld, v);
				vpOld = v;
				p++;
			}
			
			if(m >= 0)
			{
				v = coords.get(m).doubleValue();
				g = vmOld - v;
				if(g > Preferences.gridRes()*0.1)
				{
					return 0.5*(v + vmOld);
				}
				if(g > biggest.length())
					biggest = new RrInterval(v, vmOld);
				vmOld = v;
				m--;
			}
		}
		
		if(biggest.length() > Preferences.gridRes()*0.1)
			return biggest.low() + 0.5*biggest.length();
		else
		{
			do
				v = coords.get(0) + (rangen.nextDouble() - 0.5)*i.length();
			while(!i.in(v));
			return v;
		}
	}
	
	/**
	 * Find the place to put the quad division.
	 * @return
	 */
	private Rr2Point biggestGap()
	{
		Rr2Point result = new Rr2Point(findGap(xCoords, box.x()), findGap(yCoords, box.y()));
		//System.out.println("Box: " + box.toString() + "   centre: " + result.toString());
		
		// Not needed any more
		
		//xCoords = new ArrayList<Double>();
		//yCoords = new ArrayList<Double>();
		
		// Sanity check
		
		if(box.pointRelative(result) != 0)
			Debug.d("STLSlice.biggestGap(): point outside box! point: " + 
					result.toString() + ", box: " + box.toString());
		
		return result;		
	}
	
	/**
	 * Quad tree division - make the 4 sub quads.
	 */
	private void makeQuads()
	{
//		 Set up the quad-tree division
		
		Rr2Point sw = box.sw();
		Rr2Point nw = box.nw();
		Rr2Point ne = box.ne();
		Rr2Point se = box.se();
		
		Rr2Point cen = biggestGap();
		double w = cen.x() - (cen.x() - sw.x())*(sFactor - 1);
		double n = cen.y() + (nw.y() - cen.y())*(sFactor - 1);
		double e = cen.x() + (se.x() - cen.x())*(sFactor - 1);
		double s = cen.y() - (cen.y() - sw.y())*(sFactor - 1);
		
//		 Put the results in the children
		
		RrRectangle b = new RrRectangle(nw, new Rr2Point(e, s));
		q1 = new STLSlice(edges, b);
		q1.prune();
		
		b = new RrRectangle(ne, new Rr2Point(w, s));
		q2 = new STLSlice(edges, b);
		q2.prune();
		
		b = new RrRectangle(se, new Rr2Point(w, n));
		q3 = new STLSlice(edges, b);
		q3.prune();
		
		b = new RrRectangle(sw, new Rr2Point(e, n));
		q4 = new STLSlice(edges, b);
		q4.prune();
	}
	

	/**
	 * Quad tree division to end up with two (or no) ends in each box.
	 */
	public void divide()
	{
		if(edges.size() <= 0)
			return;
		
		if(box.dSquared() < resolution_2)
		{
			Debug.d("STLSlice.divide(): hit resolution limit! Edge end count: " + edges.size());
			for(int i = 0; i < edges.size(); i++)
				Debug.d(edges.get(i).toString());
			LineSegment.setQuad(this);
			return;
		}
		
		if(edges.size() > 2)
		{
			makeQuads();
			q1.divide();
			q2.divide();
			q3.divide();
			q4.divide();
			return;
		}

		for(int i = 0; i < edges.size(); i++)
		{
			if(box.pointRelative(segment(i).a) == 0 &&  
					box.pointRelative(segment(i).b) == 0)
			{
				makeQuads();
				q1.divide();
				q2.divide();
				q3.divide();
				q4.divide();
				return;
			}		
		}

		if(edges.size() == 1)
		{
			Debug.d("STLSlice.divide(): only one end in box: " + edges.get(0).toString() + " box: " + box.toString());
			edges.remove(0);
		} else
			LineSegment.setQuad(this);

	}
	
	 /**
	 * Recursively walk the tree to find an unvisited corner in a quad
     */
    private STLSlice findCorner()
    {	
    	STLSlice result = null;
    	
    	if(!leaf())
    	{
    		result = q1.findCorner();
    		if(result != null)
    			return result;
    		result = q2.findCorner();
    		if(result != null)
    			return result;
    		result = q3.findCorner();
    		if(result != null)
    			return result;
    		result = q4.findCorner();
    		return result;
    	}

    	if(edges.size() > 0)
    		return this;
    	else
    		return null;
    }

//    /**
//     * Useful for debugging - plot a bit of the quad tree.
//     */
//    private static void quickPlot(STLSlice s)
//    {
//    	s.qp = new RrGraphics(s);
//    }
    
    public void recursiveReport()
    {
    	if(leaf())
    	{
    		System.out.println("Leaf box: " + box.toString() + " contains -");
        	for(int i = 0; i < edges.size(); i++)
        		System.out.println("   " + segment(i).toString());
    	} else
    	{
    		q1.recursiveReport();
    		q2.recursiveReport();
    		q3.recursiveReport();
    		q4.recursiveReport();
    	}
    }
    
    /**
     * Useful for debugging - print statistics
     * TODO: rewrite so log4j can be used
     */
    public void reportStats()
    {
    	int single = 0;
    	int twin = 0;
    	for(int i = 0; i < edges.size(); i++)
    	{
    		LineSegment s = segment(i);
    		if(s.qa == null)
    			single++;
    		if(s.qb == null)
    			single++;
    		if(s.qa != null && s.qb != null)
    			twin++;
    	}
    	System.out.println("STLSlice.reportStats() - lines: " + edges.size()
    			+ " double-ended quads: " + twin +
    			" single ends: " + single);
    }
    

    /**
     * We are working our way round a polygon.  This function takes the
     * edge we're running along from the quad we're in and finds the 
     * next quad and next edge at its other end.
     */
    private trackPolygon processThisQuad(LineSegment edge)
    {
    	trackPolygon result = new trackPolygon();
    	
    	if(edges.size() <= 0)
    		return result;
    	
    	boolean aEnd;
    	if(edge.qa == this)
    		aEnd = true;
    	else
    		aEnd = false;
    	
    	boolean dud = true;
    	for(int i = 0; i < edges.size(); i++)
    	{
    		if(edge == segment(i))
    		{
    			edges.remove(i);
    			dud = false;
    			break;
    		}
    	}
    	
    	if(dud)
    		Debug.d("processThisQuad(): edge not found!");
    	
    	if(edges.size() <= 0)
    	{
    		Debug.d("processThisQuad(): entered quad with no exit!");
    		return result;
    	}
    	
    	result.nextE = segment(0);
    	edges.remove(0);
		
		Rr2Point p0, p1;
		
		if(aEnd)
			p0 = edge.a;
		else
			p0 = edge.b;
		
    	if(result.nextE.qa == this)
    		aEnd = true;
    	else
    		aEnd = false;
    	
		if(aEnd)
			p1 = result.nextE.a;
		else
			p1 = result.nextE.b;
		
		result.here = Rr2Point.mul(0.5, Rr2Point.add(p0, p1));
		
		if(aEnd)
			result.nextQ = result.nextE.qb;
		else
			result.nextQ = result.nextE.qa;
    	
		return result;
    }
	
	/**
	 * Stitch up the line segment ends in the quad tree.
	 * @param fg
	 * @param fs
	 * @return a list of all the resulting polygons.
	 */
	private static RrPolygonList conquer(STLSlice root) //, int fg, int fs)
	{
		RrPolygonList pgl = new RrPolygonList();
		
		STLSlice corner, startCorner;
		LineSegment edge;
		startCorner = root.findCorner();
		
		while(startCorner != null)
		{
			corner = startCorner;
			edge = corner.segment(0);
			RrPolygon pg = new RrPolygon(edge.att, true);
			do
			{
				trackPolygon tp = corner.processThisQuad(edge);
				if(tp.here != null)
					pg.add(tp.here);
				corner = tp.nextQ;
				edge = tp.nextE;
			} while (corner != startCorner && corner != null);
			
			if(pg.size() > 2)  // Throw away "noise"...
			{
				//pg.flag(pg.size() - 1, fs);
				pgl.add(pg);
			}

			startCorner = root.findCorner();
		}
		return pgl;
	}
    

	/**
	 * Find the maximum height of the object(s) to be built
	 * @return that height
	 */
	public double maxZ()
	{
		STLObject stl;
		double result = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < shapeList.size(); i++)
		{
			stl = (STLObject)shapeList.get(i);
			if(stl.size().z > result)
				result = stl.size().z;
		}
		return result;
	}
	
	
	/**
	 * build a 2D polygon list of all edges in the plane z
	 * from all the objects in shapeList then turn it in CSG form.
	 * @param z
	 * @return a CSG representation of all the polygons in the slice
	 */
	public BooleanGridList slice(double z, Extruder es[])
	{
		BooleanGridList rl = new BooleanGridList();
		RrCSG csgp = null;
		
		if(generateLowerTriangles)
			below = new BranchGroup();
		else
			below = null;

		for(int mat = 0; mat < materialList.getExtruderCount(); mat++)
		{
			//destroyLayer();
			ArrayList<AandT> aats = materialList.getAandTs(mat);

			if(aats.size() > 0)
			{
				//Appearance ap = aats.get(0).att.getAppearance();
				
				for(int obj = 0; obj < aats.size(); obj++)
				{
					destroyLayer();
					AandT aat = aats.get(obj);
					Transform3D trans = aat.trans;
					Attributes attr = aat.att;
					recursiveSetEdges(attr.getPart(), trans, z, attr);


					// Make sure nothing falls down the cracks.

					sFactor = Preferences.swell();
					box = box.scale(sFactor);
					resolution_2 = box.dSquared()*Preferences.tiny();

					// Recursively generate the quad tree.  The aim is to have each
					// leaf quad containing either 0 or 2 ends of different line
					// segments.  Then we just run round joining up all the pairs of
					// ends.

					//divide();

					// Run round joining up all the pairs of ends...

					//RrPolygonList pgl = conquer(this); 
					
					RrPolygonList pgl = simpleCull();
					
					// Remove wrinkles

					pgl = pgl.simplify(Preferences.gridRes()*1.5);

					// Fix small radii

					pgl = pgl.arcCompensate();

					// Check for a silly result.

					if(pgl.size() > 0)
					{
						csgp = pgl.toCSG(Preferences.tiny());
						rl.add(new BooleanGrid(csgp), attr);
//						if(picture == null)
//						{
//						  picture = new RrGraphics("STL Slice");
//						  picture.init(ObjectPlanRectangle(), false);
//						}
//
//						picture.cleanPolygons();
//						csgp.divide(0.0001, 1.0);
//						//picture.add(pgl);
//						picture.add(csgp);
					}
				}
			}
		}
//		if(picture == null)
//		{
//		  picture = new RrGraphics("STL Slice");
//		  picture.init(ObjectPlanRectangle(), false);
//		}
//
//		picture.cleanPolygons();
//		if(rl.size() > 0)
//		{
//			csgp.divide(0.01, 1.0);
//			picture.add(csgp);
//			//picture.add(rl.get(0));
//		}
		return rl;
	}
}
