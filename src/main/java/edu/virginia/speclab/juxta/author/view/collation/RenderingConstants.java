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
 
package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

/**
 * Provides constants for rendering differences. Extends <code>JuxtaUserInterfaceStyle</code>.
 * @author Nick
 *
 */
interface RenderingConstants extends JuxtaUserInterfaceStyle
{
    public static final int LINE_EXTENSION_BEYOND_MARGIN = 40;
    public static final int BOX_WIDTH = 180;    
    public static final int BOX_ARC = 12;
    public static final int TITLE_HEIGHT = 20;
    public static final float STROKE_WIDTH = 1.0f;
    public static final float HIGHLIGHT_STROKE_WIDTH = 1.0f;
    public static final float ANCHOR_POINT_SQUARE_WIDTH = 3.0f;
    public static final int BOX_GAP = 3;
    public static final int DIFF_ICON_OFFSET_X = 4;
    public static final int DIFF_ICON_OFFSET_Y = 2;
	public static final int ANNOTATION_ICON_WIDTH = 13;
	public static final int ANNOTATION_ICON_OFFSET_Y = 4;
    public static final int SYMBOL_SIDEBAR_WIDTH = 20;
    
    public static final int MAX_BOX_DISTANCE_FROM_LINE = 550;
    
    public static final int MARGIN_SIZE = LINE_EXTENSION_BEYOND_MARGIN + BOX_WIDTH + (BOX_GAP*2);
    
    public static final int PB_MARGIN_SIZE = 10;
    
	
	public static final float TITLE_TEXT_INSET_X = 16.0f;
	public static final float MARGIN_BOX_TEXT_INSET_LEFT = 5.0f;
    public static final float MARGIN_BOX_TEXT_INSET_RIGHT = 5.0f;
	public static final float MARGIN_BOX_TEXT_INSET_Y = 3.0f;
        
    public static final int MARGIN_BOX_TEXT_MAX_LINES = 3;
        
    public static final Color TRANSPARENT_BACKGROUND = new Color(0,0,0,0); 
    public static final Color DIFF_STROKE_COLOR = FIRST_COLOR_DARKER;
    public static final Color DIFF_FILL_COLOR = FIRST_COLOR;
    public static final Color DIFF_HIGHLIGHT_FILL_COLOR = FIRST_COLOR_BRIGHTEST;
    public static final Color DIFF_SELECTION_FILL_COLOR = FIRST_COLOR_BRIGHTER;
	public static final Color DIFF_TEXT_COLOR = Color.BLACK;
	public static final Color FRAG_FILL_COLOR = FIRST_COLOR_BRIGHTEST;
	public static final Color FRAG_SELECTION_FILL_COLOR = Color.PINK;
	public static final Color DISABLED_TEXT_COLOR = new Color(215,215,215);
	public static final Color BLOCK_FILL_COLOR = new Color(255,255,199);
    
    public static final Color SELECTION_VIEWPORT_COLOR = Color.GRAY;
    public static final Color SELECTION_VIEWPORT_SHADING = new Color(0,0,0,.1f);
	
	public static final Font DIFF_BODY_FONT = SMALL_FONT;
	public static final Font DIFF_TITLE_FONT = LARGE_FONT;

    // controls the refresh rate when loading differences
    public static final int ANIMATION_TIMER_FREQUENCY = 20;
    
    // max milliseconds we can spend painting differences in a single repaint call
    // spreading it out over multiple repaints paints slower but is more responsive
    // to user input.
    public static final long PAINT_TIMEOUT = 50;
    
    public static final Composite DRAW_MODE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	
    // blue scale
    public static final Color[] BLUE_DIFFERENCE_SCALE = SECOND_COLOR_SCALE; 

}
