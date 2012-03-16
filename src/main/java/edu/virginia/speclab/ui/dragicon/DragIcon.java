/*
 * Created on Jan 12, 2004
 *
 */
/*
 *  Copyright 2002-2010 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 

package edu.virginia.speclab.ui.dragicon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A graphical icon which can be dragged and dropped anywhere within the 
 * associated JFrame. This class uses the glassPane component of 
 * the JFrame during dragging. This class is abstract and requires 
 * implementations for <code>makeCopy()</code> and <code>handleDropEvent()</code>.
 * The derived class is also responsible for setting the "face" of the DragIcon
 * to the component which will visually represent the icon.  
 * @author Nick Laiacona
 */
public abstract class DragIcon extends JPanel {
	
	private JComponent myFace;
	private JFrame applicationFrame;
	boolean beingDragged;
	DragIcon masterCopy;
	private Point attachPoint;
	private Component oldGlassPane;
					
	protected DragIcon() {
		setLayout(null);			
		beingDragged = false;
		
		IconDragger dragger = new IconDragger(this);
		IconDropper dropper = new IconDropper(this);
		
		addMouseMotionListener(dragger);
		addMouseListener(dropper);						
	}
	
	/**
	 * This method is used by master copy icons to replicate themselves
	 * when they are dragged. The copy created is the new master copy.  
	 * @return The new master copy.
	 */
	protected abstract DragIcon makeCopy();
	
	/**
	 * This method is called when the icon is dragged and dropped on the 
	 * component. The icon is removed from the screen and the UI no longer 
	 * retains a copy of the icon when this method is called.  
	 * @param c The target component of the drop. The drop target
	 * must be contained in the application frame <code>rootPane</code>. 
	 */	
	protected abstract void handleDropEvent(Component c);
	
	/**
	 * This method is called when the icon is double clicked.
	 */
	protected abstract void handleDoubleClick();
			
	private void grabIcon(Point p) {
		
		if( beingDragged == true ) { 
			return;
		} else {
			beingDragged = true;
		}
												
		if( masterCopy == this ) {				
			// make a new master copy
			DragIcon copy = makeCopy();
			this.setMasterCopy(copy);
			Dimension size = copy.getPreferredSize();
			Point location = getLocation();		
			copy.setBounds(location.x,location.y,size.width,size.height);
			getParent().add(copy);										 	
		} 
		
		// record attachment point for cursor
		attachPoint = new Point(p);
		
		myFace.setVisible(false);  
		Container parent = getParent();	
		parent.remove(this);
		oldGlassPane = applicationFrame.getGlassPane();
		applicationFrame.setGlassPane(this);			
		setOpaque(false);		
		myFace.validate();
		setVisible(true);
	}

	private void dragIcon(Point p) {
		if( beingDragged == false ) { 
			grabIcon(p);
			return;
		} 
		
		p.translate(-attachPoint.x,-attachPoint.y); 
		myFace.setLocation(p);
		myFace.setVisible(true);						
	}
	
	private void dropIcon(Point p) {
		dragIcon(p);
		beingDragged = false;
		Component targetComponent = applicationFrame.getRootPane().getContentPane().getComponentAt(p);
		handleDropEvent(targetComponent);
		applicationFrame.setGlassPane(oldGlassPane);
		applicationFrame.validate();				 		
	}
	
	private class IconDragger extends MouseMotionAdapter {
	
		private DragIcon myDragIcon; 
			
		IconDragger( DragIcon dragIcon ) {
			myDragIcon = dragIcon;
		}
				
		public void mouseDragged(MouseEvent e) {	
			myDragIcon.dragIcon(e.getPoint());
		}
	}

	private class IconDropper extends MouseAdapter {

		private DragIcon myDragIcon;
			
		IconDropper( DragIcon dragIcon ) { 
			myDragIcon = dragIcon;
		}
		
		public void mousePressed(MouseEvent e) {
			if( e.getClickCount() >= 2 ) {
				handleDoubleClick();
			}		
		}
				
		public void mouseReleased(MouseEvent e) {
			if( myDragIcon.isBeingDragged() == true ) {
				myDragIcon.dropIcon(e.getPoint());
			}
		}
	}
		
	/**
	 * Determine the drag state of the icon. 
	 * @return True if being dragged, false otherwise.
	 */
	public boolean isBeingDragged() {
		return beingDragged;
	}

	/**
	 * Determine if this is a master copy. Master copies are replicated
	 * when they are dragged. This is accomplished with a call to <code>makeCopy()</code>.
	 * @return True if this is a master copy, false otherwise.
	 */
	public boolean isMasterCopy() {
		return (masterCopy == this);
	}

	/**
	 * Set the current application frame. This must be set for the icon
	 * to function properly.
	 * @param frame The <code>JFrame</code> of the application window.
	 */
	public void setFrame(JFrame frame) {
		applicationFrame = frame;
	}

	/**
	 * Retrieve the <code>JComponent</code> that is the visual 
	 * representation of the icon. 
	 * @return The current face component.
	 */
	public JComponent getFace() {
		return myFace;
	}

	/**
	 * Set the current face component for this <code>DragIcon</code>
	 * @param component The new face component.
	 */
	public void setFace(JComponent component) {
		removeAll();
		if( component != null ) {
			myFace = component;
			Dimension size = myFace.getPreferredSize();
			myFace.setBounds(0,0,size.width,size.height);				
			add(myFace);
			setPreferredSize(size);
		}
	}

	/**
	 * Retrieve the current application frame.
	 * @return The application frame for this icon.
	 */
	public JFrame getApplicationFrame() {
		return applicationFrame;
	}

    /**
     * @param masterCopy The masterCopy to set.
     */
    public void setMasterCopy(DragIcon masterCopy)
    {
        this.masterCopy = masterCopy;
    }
    /**
     * @return Returns the masterCopy.
     */
    public DragIcon getMasterCopy()
    {
        return masterCopy;
    }
}		
		
					