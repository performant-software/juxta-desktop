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
 

package edu.virginia.speclab.ui;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JComponent;

/**
 * Renders and manages a set of <code>GraphicButton</code> objects on a given 
 * <code>JComponent</code> surface. Call repaint() on the component, which 
 * somewhere should override paint() to call paintButtons().
 * @author Nick
 *
 */
public class GraphicalButtonSet
{
    private JComponent parentComponent;
    private HashSet graphicalButtons;
            
    public GraphicalButtonSet( JComponent parentComponent )
    {
        this.parentComponent = parentComponent;
        graphicalButtons = new HashSet();        
        MouseTracker tracker = new MouseTracker();
        parentComponent.addMouseListener(tracker);
        parentComponent.addMouseMotionListener(tracker);
    }
    
    public void paintButtons( Graphics2D g2 )
    {
        synchronized( graphicalButtons )
        {
            for( Iterator i = graphicalButtons.iterator(); i.hasNext(); )
            {
                GraphicalButton button = (GraphicalButton) i.next();
                button.paint(g2);
            }        
        }
    }
       
    public void addButton( GraphicalButton button )
    {      
        graphicalButtons.add(button);
        parentComponent.repaint();
    }
    
    public void removeButton( GraphicalButton button )
    {
        graphicalButtons.remove(button);
        parentComponent.repaint();
    }
    
    private GraphicalButton getButton( Point p )
    {
        for( Iterator i = graphicalButtons.iterator(); i.hasNext(); )
        {
            GraphicalButton button = (GraphicalButton) i.next();
            if( button.testHit(p) ) return button;
        }        
        
        return null;
    }
    
    private void clearRollOvers()
    {
        for( Iterator i = graphicalButtons.iterator(); i.hasNext(); )
        {
            GraphicalButton button = (GraphicalButton) i.next();
            button.onRollOut();
        }        
    }
    
    private class MouseTracker extends MouseAdapter implements MouseMotionListener
    {
        public void mousePressed(MouseEvent e)
        {
            GraphicalButton button = getButton( e.getPoint() );
            if( button != null )
            {
                button.onPress();
                parentComponent.repaint();
            }
        }
        
        public void mouseReleased(MouseEvent e)
        {
            GraphicalButton button = getButton( e.getPoint() );
            if( button != null )
            {                
                button.onRelease();
                parentComponent.repaint();
            }
        }

        public void mouseDragged(MouseEvent e)
        {
            // do nothing
        }

        public void mouseMoved(MouseEvent e)
        {            
            clearRollOvers();
            
            GraphicalButton button = getButton( e.getPoint() );
            if( button != null )
            {
                button.onRollOver();                
            }
            
            parentComponent.repaint();
        }        
    }
}
