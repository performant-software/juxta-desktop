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

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * A JPanel with reasonable defaults for scrolling text.
 * @author Nick
 *
 */
public class ReasonableScroller extends JPanel implements Scrollable
{
    public static final int SCROLL_BLOCK_INCREMENT = 100; 
    public static final int SCROLL_UNIT_INCREMENT = 8;

    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    public boolean getScrollableTracksViewportWidth()
    {
        // always match the width of the viewport
        return true;
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return SCROLL_BLOCK_INCREMENT;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return SCROLL_UNIT_INCREMENT;
    }
}
