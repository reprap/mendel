//
//package org.reprap.geometry.polygons;
//
//import org.reprap.Attributes;
//import org.reprap.utilities.RrGraphics;
//
///**
// * Testing class
// */
//public class TestMain
//{
//	public static void rrpTest()
//	{
//		Rr2Point p = new Rr2Point(0.1, 0.15);
//		Rr2Point q = new Rr2Point(0.2, 0.85);
//		Rr2Point r = new Rr2Point(1, 0.89);
//		
//		Rr2Point s = new Rr2Point(0.95, 0.03);
//		Rr2Point pp = new Rr2Point(0.35, 0.62);
//		Rr2Point qq = new Rr2Point(0.55, 0.65);
//		Rr2Point rr = new Rr2Point(0.45, 0.5);
//		Rr2Point ss = new Rr2Point(0.4, 0.3);    
//		
//		RrPolygon a = new RrPolygon(new Attributes(null, null, null, null), true);
//		a.add(p);
//		a.add(q);
//		a.add(r);
//		a.add(s);
//		
//		RrPolygonList c = new RrPolygonList();
//		c.add(a);
//		
//		a = new RrPolygon(new Attributes(null, null, null, null), true);
//		a.add(rr);
//		a.add(qq);
//		a.add(pp);
//		a.add(ss);
//		c.add(a);
//		
//		//RrPolygonList d = c.offset(0.03);
//		
//		//RrPolygon  e = c.hatch(x, 0.03, 3, 4);
//		//d = d.offset(0.003);
//		//e = e.join_up(d);
//		//c.add(d); 
//		//c.add(e);
//		
//		new RrGraphics(c);
//	}
//	
//	public static RrCSGPolygon hex()
//	{
//		double hexSize = 10;
//		double hexX = 35, hexY = 15;
//		
//		RrCSG r = RrCSG.universe();
//		Rr2Point pold = new Rr2Point(hexX + hexSize/2, hexY);
//		Rr2Point p;
//		double theta = 0; 
//		for(int i = 0; i < 6; i++)
//		{
//			theta += Math.PI * 60. / 180.0;
//			p = new Rr2Point(hexX + Math.cos(theta)*hexSize/2, hexY + Math.sin(theta)*hexSize/2);
//			r = RrCSG.intersection(r, new RrCSG(new RrHalfPlane(p, pold)));
//			pold = p;
//		}
//		
//		return new RrCSGPolygon(r, new RrRectangle(new Rr2Point(hexX - hexSize, hexY - hexSize), 
//				new Rr2Point(hexX + hexSize, hexY + hexSize)),
//				new Attributes(null, null, null, null));
//	}
//	
//	public static RrCSGPolygon testPol()
//	{
//		Rr2Point p = new Rr2Point(10, 15);
//		Rr2Point peps = new Rr2Point(10.0000001, 15);
//		Rr2Point q = new Rr2Point(20, 85);
//		Rr2Point r = new Rr2Point(97, 89);
//		Rr2Point s = new Rr2Point(95, 3);
//		
//		Rr2Point pp = new Rr2Point(30, 62);
//		//Rr2Point qq = new Rr2Point(0.55, 0.95);
//		Rr2Point qq = new Rr2Point(35, 95);
//		Rr2Point rr = new Rr2Point(35, 20);    
//		
//		RrHalfPlane ph = new RrHalfPlane(p, q);
//		RrHalfPlane pheps = new RrHalfPlane(peps, q);
//		RrHalfPlane qh = new RrHalfPlane(q, r);
//		RrHalfPlane rh = new RrHalfPlane(r, s);
//		RrHalfPlane sh = new RrHalfPlane(s, p);
//		RrHalfPlane sheps = new RrHalfPlane(s, peps);		
//		
//		/* RrHalfPlane pph = */ new RrHalfPlane(pp, qq);
//		RrHalfPlane qqh = new RrHalfPlane(qq, rr);
//		RrHalfPlane rrh = new RrHalfPlane(rr, pp);
//		
//		RrCSG pc = new RrCSG(ph);
//		RrCSG pceps = new RrCSG(pheps);
//		RrCSG qc = new RrCSG(qh);
//		RrCSG rc = new RrCSG(rh);
//		RrCSG sc = new RrCSG(sh);
//		RrCSG sceps = new RrCSG(sheps);		
//		
//		pc = RrCSG.intersection(pc, qc);
//		rc = RrCSG.intersection(sc, rc);		
//		pc = RrCSG.intersection(pc, rc);
//		
//		//RrCSG ppc = new RrCSG(pph);
//		RrCSG qqc = new RrCSG(qqh);
//		RrCSG rrc = new RrCSG(rrh);
//		
//		// ppc = RrCSG.intersection(ppc, qqc);
//		RrCSG ppc = RrCSG.intersection(qqc, rrc);
//		ppc = RrCSG.difference(pc, ppc);
//		
//		pc = ppc.offset(-8);
//		ppc = RrCSG.difference(ppc, pc);
//		RrCSG ppceps = ppc.offset(-0.001);
//		//ppc = RrCSG.intersection(ppc, ppceps);
//		//ppc = RrCSG.intersection(ppc, pceps);
//		//ppc = RrCSG.intersection(ppc, sceps);
//		
//		return new RrCSGPolygon(ppc, new 
//				RrRectangle(new Rr2Point(-3.2,-1.76), new Rr2Point(131.7,112)),
//				new Attributes(null, null, null, null));
//	}
//	
//	public static void rrCSGTest()
//	{
//		RrCSGPolygon cp = testPol();
//
//		//RrCSGPolygon cp = hex();
//		cp.divide(1.0e-6, 1.03);
//		
//		//RrPolygonList pl = cp.megList(2, 3);
//		//RrGraphics g = new RrGraphics(pl, false);
//		//System.out.println(cp.toString());
//		/* RrGraphics g1 = */ new RrGraphics(cp);
//		//RrHalfPlane hatch = new RrHalfPlane(new Rr2Point(1, -1), new Rr2Point(-1, 1));
//		//RrHalfPlane hatch = new RrHalfPlane(new Rr2Point(1, 1), new Rr2Point(-1, -1));
//		//RrPolygonList h = cp.hatch(hatch, 0.05, 3, 0);
//		//System.out.println(h.toString());
//		//g1.addPol(h);
//
//		
////		RrLine hatch = new RrLine(new Rr2Point(-1, -1), new Rr2Point(1, 1));
////		RrPolygon  h = cp.hatch_join(x, 0.005, 1, 3);
////		RrPolygonList hp;
////		hp = cp.megList(1, 0);
////		hp.add(h);
////		g.addPol(hp);  
//	}
//	
//	public static void rrCHTest()
//	{
//		RrCSGPolygon cp = testPol();
//		
//		cp.divide(1.0e-6, 1);
//		//RrGraphics g = new RrGraphics(cp, true);
//		RrPolygonList hp = cp.megList();
//		System.out.println("polygons: " + hp.size());
////		RrPolygonList hpl0 = new RrPolygonList();
////		hpl0.add(hp.polygon(0));
////		hpl0.add(hp.polygon(1));
////		hpl0.add(hp.polygon(2));
////		hpl0.add(hp.polygon(3));
////		RrCSGPolygon restored = hp.toCSG();
////		restored.divide(1.0e-6, 1);
////		System.out.println(restored.toString());
//		/* RrGraphics g = */ new RrGraphics(hp);
//
//		//g.addCSG(cp);
//		//g.addCSG(restored);
//		
////		List chl = hp.convexHull();
////		RrPolygonList ch = new RrPolygonList();		
////		ch.add(hp.toRrPolygonHull(chl, 1));
////		g.addPol(ch);  
//	}
//	
//	
//	
//	
//	
//	public static void main(String args[])
//	{
//		rrCSGTest();
//		//rrCHTest();
//		//rrpTest();
//	}
//}
