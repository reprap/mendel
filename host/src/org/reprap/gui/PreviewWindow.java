
// Obsolete - to be removed at some point.  - AB

//package org.reprap.gui;
//import java.awt.BorderLayout;
//
//import javax.media.j3d.BranchGroup;
//import javax.swing.WindowConstants;
//import org.reprap.Printer;
//
//public class PreviewWindow extends javax.swing.JFrame implements Previewer {
//	private static final long serialVersionUID = 1L;
//	private PreviewPanel panel;
//	
//	public PreviewWindow(Printer p) {
//		super();
//		initGUI(p);
//	}
//	
//	private void initGUI(Printer p) {
//		try {
//			panel = new PreviewPanel();
//			panel.setMachine(p);
//			getContentPane().add(panel, BorderLayout.CENTER);
//			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//			pack();
//			setSize(500, 350);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void setMachine(Printer p) {
//		panel.setMachine(p);
//	}
//	
//	public void addSegment(double x1, double y1, double z1,
//			double x2, double y2, double z2) {
//		panel.addSegment(x1, y1, z1, x2, y2, z2);
//	}
//	
////	public void setMessage(String message) {
////		panel.setMessage(message);
////	}
////
////	public boolean isCancelled() {
////		return panel.isCancelled();
////	}
//	
//	public void reset() {
//		panel.reset();
//	}
//
////	public void setCancelled(boolean isCancelled) {
////		panel.setCancelled(isCancelled);
////	}
//	
//	public void setLowerShell(BranchGroup ls)
//	{
//		panel.setLowerShell(ls);
//	}
//}
