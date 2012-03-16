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
 

package edu.virginia.speclab.juxta.author.view.compare;

import java.awt.BasicStroke;
import java.awt.Color;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.DualScrollingTextPanel;

interface DifferenceCompareRenderingConstants extends JuxtaUserInterfaceStyle
{
	public static final Color DISABLED_TEXT_COLOR = new Color(215,215,215);
	
    public static final int LEFT = DualScrollingTextPanel.LEFT;
    public static final int RIGHT = DualScrollingTextPanel.RIGHT;
    
    public  static final int HORIZONTAL_BAR_LENGTH = 3;
    public  static final int HORIZONTAL_BAR_LENGTH_MOVE = 13;
    public  static final int DIFFERENCE_MARK_MARGIN = 4;
    
    public static final float DASH[] = {2.0f}; 

    public static final BasicStroke DASH_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT,  
            BasicStroke.JOIN_MITER, 10.0f, DASH, 0.0f); 

    public static final BasicStroke THICK_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT,  
            BasicStroke.JOIN_MITER); 
}
