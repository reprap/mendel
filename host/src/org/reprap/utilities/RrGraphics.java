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
 
 
 RrGraphics: Simple 2D graphics
 
 First version 20 May 2005
 This version: 1 May 2006 (Now in CVS - no more comments here)
 
 */

package org.reprap.utilities;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import org.reprap.geometry.polygons.BooleanGrid;
import org.reprap.geometry.polygons.Rr2Point;
import org.reprap.geometry.polygons.RrRectangle;
import org.reprap.geometry.polygons.RrCSGOp;
//import org.reprap.geometry.polygons.RrCSGPolygon;
import org.reprap.geometry.polygons.RrHalfPlane;
import org.reprap.geometry.polygons.RrInterval;
import org.reprap.geometry.polygons.RrLine;
import org.reprap.geometry.polygons.RrPolygon;
import org.reprap.geometry.polygons.RrPolygonList;
import org.reprap.geometry.polygons.STLSlice;
import org.reprap.gui.StatusMessage;
import org.reprap.Attributes;
import javax.media.j3d.Appearance;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.vecmath.Color3f;
import javax.media.j3d.Material;

/**
 * Class to plot images of geometrical structures for debugging.
 * 
 * @author ensab
 *
 */
public class RrGraphics 
{
	static final Color background = Color.white;
	static final Color boxes = Color.blue;
	static final Color polygon1 = Color.red;
	static final Color polygon0 = Color.black;	
	static final Color infill = Color.pink;
	static final Color hatch1 = Color.magenta;
	static final Color hatch0 = Color.orange;
	
	/**
	 * Pixels 
	 */
	private final int frame = 600;
	
	/**
	 * 
	 */
	private int frameWidth;
	
	/**
	 * 
	 */
	private int frameHeight;
	
	/**
	 * 
	 */
	private RrPolygonList p_list = null;
	
	/**
	 * 
	 */
	//private RrCSGPolygon csg_p = null;
	
	/**
	 * 
	 */
	private BooleanGrid bg = null;
	
	/**
	 * 
	 */
	private boolean csgSolid = true;
	
	/**
	 * 
	 */
	private STLSlice stlc = null;
	
	/**
	 * 
	 */
	private List<RrHalfPlane> hp = null;
	
	/**
	 * 
	 */
	private double scale;
	
	/**
	 * 
	 */
	private Rr2Point p_0;
	
	/**
	 * 
	 */
	private Rr2Point pos;
	
	private RrRectangle scaledBox, originalBox;
	
	/**
	 * 
	 */
	private static Graphics2D g2d;
	private static JFrame jframe;
	/**
	 * 
	 */
	private boolean plot_box = false;
	
	private String title = "RepRap diagnostics";
	
	private boolean initialised = false;
	
	/**
	 * Constructor for just a box - add stuff later
	 * @param b
	 * @param pb
	 */
	public RrGraphics(RrRectangle b, String t) 
	{
		p_list = null;
		//csg_p = null;
		stlc = null;
		hp = null;
		title = t;
		init(b, false);
	}
	
	/**
	 * Constructor for nothing - add stuff later
	 * @param b
	 * @param pb
	 */
	public RrGraphics(String t) 
	{
		p_list = null;
		//csg_p = null;
		stlc = null;
		hp = null;
		title = t;
		initialised = false;
	}
	
	public void cleanPolygons()
	{
		p_list = null;
		//csg_p = null;
		stlc = null;
		hp = null;
	}
//	/**
//	 * Constructor for point-list polygon
//	 * @param pl
//	 * @param pb
//	 */
//	public RrGraphics(RrPolygonList pl) 
//	{
//		if(pl.size() <= 0)
//		{
//			System.err.println("Attempt to plot a null polygon list!");
//			return;
//		}
//		
//		p_list = pl;
//		hp = null;
//		csg_p = null;
//		stlc = null;
//		
//		init(pl.getBox(), true);
//	}
//	
//	/**
//	 * Constructor for CSG polygon
//	 * @param cp
//	 */
//	public RrGraphics(RrCSGPolygon cp) 
//	{
//		p_list = null;
//		hp = null;
//		csg_p = cp;
//		stlc = null;
//		
//		init(csg_p.box(), true);
//	}
//	
//	/**
//	 * Constructor for CSG polygon and crossing lines
//	 * @param cp
//	 * @param pb
//	 */
//	public RrGraphics(RrCSGPolygon cp, List<RrHalfPlane> h) 
//	{
//		p_list = null;
//		csg_p = cp;
//		hp = h;
//		stlc = null;
//		
//		init(csg_p.box(), true);
//	}
//	
//	/**
//	 * Constructor for STL polygons
//	 * @param s
//	 * @param pb
//	 */
//	public RrGraphics(STLSlice s) 
//	{
//		p_list = null;
//		csg_p = null;
//		hp = null;
//		stlc = s;
//		
//		init(stlc.box(), true);
//	}
//	

	
	private void setScales(RrRectangle b)
	{
		scaledBox = b.scale(1.2);
		
		double width = scaledBox.x().length();
		double height = scaledBox.y().length();
		if(width > height)
		{
			frameWidth = frame;
			frameHeight = (int)(0.5 + (frameWidth*height)/width);
		} else
		{
			frameHeight = frame;
			frameWidth = (int)(0.5 + (frameHeight*width)/height);
		}
		double xs = (double)frameWidth/width;
		double ys = (double)frameHeight/height;
		
		if (xs < ys)
			scale = xs;
		else
			scale = ys;	
		
		// God alone knows why the 5 and 20 are needed next...
		
		p_0 = new Rr2Point((frameWidth - (width + 2*scaledBox.x().low())*scale)*0.5,
				(frameHeight - (height + 2*scaledBox.y().low())*scale)*0.5);
		
		pos = new Rr2Point(width*0.5, height*0.5);
	}
	
	/**
	 * @param b
	 */
	public void init(RrRectangle b, boolean waitTillDone)
	{
		originalBox = b;
		setScales(b);
		
		jframe = new JFrame();
		jframe.setSize(frameWidth, frameHeight);
		jframe.getContentPane().add(new MyComponent());
		jframe.setTitle(title);
		jframe.setVisible(true);
		jframe.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		jframe.addMouseListener(new myMouse());
		jframe.addKeyListener(new myKB());
		jframe.setIgnoreRepaint(false);
		
		initialised = true;
		
		if(waitTillDone)
		{
			StatusMessage statusWindow = new StatusMessage(new JFrame());
			//statusWindow.setButton("Continue");
			statusWindow.setMessage("Left mouse - magnify\n" +
					"Middle mouse - evaluate\n" +
					"Right mouse - full image\n" +
					"b - toggle boxes\n" + 
					"s - toggle solid shading\n\n" 
			);
			statusWindow.setLocation(new Point(frameWidth + 20, 0));
			statusWindow.setVisible(true);


			boolean loop = true;
			while(loop)
			{
				try {
					Thread.sleep(100);
					loop = !statusWindow.isCancelled();
				} catch (InterruptedException e) 
				{

				}
			}
			jframe.dispose();
		}
	}
	
	
	public boolean isInitialised()
	{
		return initialised;
	}
	

	
	/**
	 * @param pl
	 */
	public void add(RrPolygonList pl)
	{
		if(p_list == null)
			p_list = new RrPolygonList(pl);
		else
			p_list.add(pl);
		jframe.repaint();
	}
	
	public void add(BooleanGrid b)
	{
		bg = b;
		jframe.repaint();
	}
	
//	/**
//	 * @param cp
//	 */
//	public void add(RrCSGPolygon cp)
//	{
//		csg_p = cp;
//		jframe.repaint();
//	}
//	
//	/**
//	 * @param s
//	 */
//	public void add(STLSlice s)
//	{
//		stlc = s;
//		jframe.repaint();
//	}
//	
//	/**
//	 * @param h
//	 */
//	public void add(List<RrHalfPlane>h)
//	{
//		hp = h;
//		jframe.repaint();
//	}
	
	/**
	 * Real-world coordinates to pixels
	 * @param p
	 * @return
	 */
	private Rr2Point transform(Rr2Point p)
	{
		return new Rr2Point(p_0.x() + scale*p.x(), (double)frameHeight - 
				(p_0.y() + scale*p.y()));
	}
	
	/**
	 * Pixels to real-world coordinates
	 * @param p
	 * @return
	 */
	private Rr2Point iTransform(int x, int y)
	{
		return new Rr2Point(((double)x - p_0.x())/scale, ((double)(frameHeight - y)
				- p_0.y())/scale);
	}
	
	/**
	 * Move invisibly to a point
	 * @param p
	 */
	private void move(Rr2Point p)
	{
		pos = transform(p);
	}
		
	/**
	 * Draw a straight line to a point
	 * @param p
	 */
	private void plot(Rr2Point p)
	{
		Rr2Point a = transform(p);
		g2d.drawLine((int)Math.round(pos.x()), (int)Math.round(pos.y()), 
				(int)Math.round(a.x()), (int)Math.round(a.y()));
		pos = a;
	}
	
	
	/**
	 * Plot a box
	 * @param b
	 */
	private void plot(RrRectangle b)
	{
		if(RrRectangle.intersection(b, scaledBox).empty())
			return;
		
		g2d.setColor(boxes);
		move(b.sw());
		plot(b.nw());
		plot(b.ne());
		plot(b.se());
		plot(b.sw());
	}
	
	/**
	 * Plot the half-plane lust
	 * @param b
	 */
	private void plot(List<RrHalfPlane> hl)
	{
		for(int i = 0; i < hl.size(); i++)
		{
			RrHalfPlane h = hl.get(i);
			if(!scaledBox.wipe(h.pLine(), RrInterval.bigInterval()).empty())
			{
				if(h.size() > 0)
				{
					move(h.getPoint(0));
					boolean even = false;
					for(int j = 1; j < h.size(); j++)
					{
						even = !even;
						if(even)
							g2d.setColor(hatch1);
						else
							g2d.setColor(hatch0);
						plot(h.getPoint(j));
					}
				}
			}
		}
	}
	
	/**
	 * Set the colour from a RepRap attribute
	 * @param at
	 */
	private void setColour(Attributes at)
	{
		Appearance ap = at.getAppearance();
		Material mt = ap.getMaterial();
		Color3f col = new Color3f();
		mt.getDiffuseColor(col);
		g2d.setColor(col.get());		
	}
	
	/**
	 * Plot a polygon
	 * @param p
	 */
	private void plot(RrPolygon p)
	{
		if(p.size() <= 0)
			return;
		if(RrRectangle.intersection(p.getBox(), scaledBox).empty())
			return;
	    if(p.getAttributes().getAppearance() == null)
	    {
	    	System.err.println("RrGraphics: polygon with size > 0 has null appearance.");
	    	return;
	    }
	    
	    setColour(p.getAttributes());
		move(p.point(0));
		for(int i = 1; i < p.size(); i++)	
				plot(p.point(i));
		if(p.isClosed())
			plot(p.point(0));
	}
	
	/**
	 * Plot a section of parametric line
	 * @param a
	 * @param i
	 */
	private void plot(RrLine a, RrInterval i)
	{
		if(i.empty()) return;
		move(a.point(i.low()));
		plot(a.point(i.high()));
	}
	
//	/**
//	 * Recursively fill a CSG quad where it's solid.
//	 * @param q
//	 */
//	private void fillCSG(RrCSGPolygon q)
//	{
//		if(RrRectangle.intersection(q.box(), scaledBox).empty())
//			return;
//		
//		if(q.c_1() != null)
//		{
//			fillCSG(q.c_1());
//			fillCSG(q.c_2());
//			fillCSG(q.c_3());
//			fillCSG(q.c_4());
//			return;
//		}
//		
//		if(q.csg().operator() == RrCSGOp.NULL)
//			return;
//			
//		g2d.setColor(infill);
//		Rr2Point sw = transform(q.box().sw());
//		Rr2Point ne = transform(q.box().ne());
//
//		int x0 = (int)Math.round(sw.x());
//		int y0 = (int)Math.round(sw.y());
//		int x1 = (int)Math.round(ne.x());
//		int y1 = (int)Math.round(ne.y());
//
//		if(q.csg().operator() == RrCSGOp.UNIVERSE)
//		{
//			g2d.fillRect(x0, y1, x1 - x0 + 1, y0 - y1 + 1);
//			return;
//		}
//
//		for(int x = x0; x <= x1; x++)
//		{
//			for(int y = y1; y <= y0; y++)  // Bloody backwards coordinates...
//			{
//				Rr2Point p = iTransform(x, y);
//				double v = q.csg().value(p);
//				if(v <= 0)
//					g2d.fillRect(x, y, 1, 1);
//			}
//		}
//
//	}
//	
//	private void boxCSG(RrCSGPolygon q)
//	{
//		if(RrRectangle.intersection(q.box(), scaledBox).empty())
//			return;
//		
//		if(q.c_1() != null)
//		{
//			boxCSG(q.c_1());
//			boxCSG(q.c_2());
//			boxCSG(q.c_3());
//			boxCSG(q.c_4());
//			return;
//		}
//		plot(q.box());
//	}
//	
//	/**
//	 * Plot a divided CSG polygon recursively
//	 * @param p
//	 */
//	private void plot(RrCSGPolygon q)
//	{
//		if(RrRectangle.intersection(q.box(), scaledBox).empty())
//			return;		
//		
//		if(q.c_1() != null)
//		{
//			plot(q.c_1());
//			plot(q.c_2());
//			plot(q.c_3());
//			plot(q.c_4());
//			return;
//		}
//		
//		g2d.setColor(polygon1);
//		if(q.csg().complexity() == 1)
//			plot(q.csg().plane().pLine(), q.interval1());
//		else if (q.csg().complexity() == 2)
//		{
//			plot(q.csg().c_1().plane().pLine(), q.interval1());
//			plot(q.csg().c_2().plane().pLine(), q.interval2());
//		}
//	}
	
	/**
	 * Recursively fill a Boolean Grid where it's solid.
	 * @param q
	 */
	private void fillBG(BooleanGrid b)
	{
		if(RrRectangle.intersection(b.box(), scaledBox).empty())
			return;
		
		if(!b.leaf())
		{
			fillBG(b.northEast());
			fillBG(b.northWest());
			fillBG(b.southEast());
			fillBG(b.southWest());
			return;
		}
		
		if(!b.value())
			return;
			
		g2d.setColor(infill);
		Rr2Point sw = transform(b.box().sw());
		Rr2Point ne = transform(b.box().ne());

		int x0 = (int)Math.round(sw.x());
		int y0 = (int)Math.round(sw.y());
		int x1 = (int)Math.round(ne.x());
		int y1 = (int)Math.round(ne.y());


		g2d.fillRect(x0, y1, x1 - x0 + 1, y0 - y1 + 1);
	}
	
	/**
	 * Recursively plot the boxes for an STL object
	 * @param s
	 */
	private void boxSTL(STLSlice s)
	{
		if(RrRectangle.intersection(s.box(), scaledBox).empty())
			return;
		
		if(s.leaf())
		{
			g2d.setColor(boxes);
			plot(s.box());
		} else
		{
			boxSTL(s.c_1());
			boxSTL(s.c_2());
			boxSTL(s.c_3());
			boxSTL(s.c_4());
		}
	}
	
	/**
	 * Plot a divided STL recursively
	 * @param s
	 */
	private void plot(STLSlice s)
	{
		if(RrRectangle.intersection(s.box(), scaledBox).empty())
			return;
		
		if(s.leaf())
		{
			g2d.setColor(polygon1);
			for(int i = 0; i < s.edges().size(); i++)
			{
				move(s.segmentA(i));
				plot(s.segmentB(i));
			}
		} else
		{
			plot(s.c_1());
			plot(s.c_2());
			plot(s.c_3());
			plot(s.c_4());
		}
	}
	
	/**
	 * Master plot function - draw everything
	 */
	private void plot()
	{
		
		if(bg != null)
		{
			fillBG(bg);
		}
		
//		if(csg_p != null)
//		{
//			if(csgSolid)
//				fillCSG(csg_p);
//			
//			if(plot_box)
//				boxCSG(csg_p);
//			//else
//				//plot(csg_p.box());
//			
//			plot(csg_p);
//		}
		
		if(p_list != null)
		{
			int leng = p_list.size();
			for(int i = 0; i < leng; i++)
				plot(p_list.polygon(i));
			if(plot_box)
			{
				for(int i = 0; i < leng; i++)
					plot(p_list.polygon(i).getBox());
			} //else
				//plot(p_list.getBox());
		}
		
		if(stlc != null)
		{
			if(plot_box)
				boxSTL(stlc);
			//else
				//plot(stlc.box());
			
			plot(stlc);
		}

		if(hp != null)
		{
			plot(hp);
		}
		

	}
	
	class myKB implements KeyListener
	{
		public void keyTyped(KeyEvent k)
		{
			switch(k.getKeyChar())
			{
			case 'b':
			case 'B':
				plot_box = !plot_box;
				break;
				
			case 's':
			case 'S':
				csgSolid = !csgSolid;
				
			default:
			}
			jframe.repaint();
		}
		
		public void keyPressed(KeyEvent k)
		{	
		}
		
		public void keyReleased(KeyEvent k)
		{	
		}
	}
	
	/**
	 * Clicking the mouse magnifies
	 * @author ensab
	 *
	 */
	class myMouse implements MouseListener
	{
		private RrRectangle magBox(RrRectangle b, int ix, int iy)
		{
			Rr2Point cen = iTransform(ix, iy);
			//System.out.println("Mouse: " + cen.toString() + "; box: " +  scaledBox.toString());
			Rr2Point off = new Rr2Point(b.x().length()*0.05, b.y().length()*0.05);
			return new RrRectangle(Rr2Point.sub(cen, off), Rr2Point.add(cen, off));
		}
		
		public void mousePressed(MouseEvent e) {
		}
	    public void mouseReleased(MouseEvent e) {
	    }
	    public void mouseEntered(MouseEvent e) {
	    }
	    public void mouseExited(MouseEvent e) {
	    }
	    
	    public void mouseClicked(MouseEvent e) 
	    {
			int ix = e.getX() - 5;  // Why needed??
			int iy = e.getY() - 25; //  "     "
			
			switch(e.getButton())
			{
			case MouseEvent.BUTTON1:
				setScales(magBox(scaledBox, ix, iy));
				break;

			case MouseEvent.BUTTON2:
//				if(csg_p != null)
//				{
//					Rr2Point pc = iTransform(ix, iy);
//					JOptionPane.showMessageDialog(null, "Potential at " + pc.toString() + " is " + csg_p.value(pc) +
//							"\nQuad: " + csg_p.quad(pc).toString());
//				}
				break;
				
			case MouseEvent.BUTTON3:

			default:
				setScales(originalBox);
			}
			jframe.repaint();
	    } 
	}
	
	/**
	 * Canvas to paint on 
	 */
	class MyComponent extends JComponent 
	{
		private static final long serialVersionUID = 1L;
		public MyComponent()
		{
			super();
		}
		// This method is called whenever the contents needs to be painted
		public void paint(Graphics g) 
		{
			// Retrieve the graphics context; this object is used to paint shapes
			g2d = (Graphics2D)g;
			// Draw everything
			plot();
		}
	}
}
