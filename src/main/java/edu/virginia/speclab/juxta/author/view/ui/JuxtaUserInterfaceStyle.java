/*
 *  Copyright 2002-2010 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http:/www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 

package edu.virginia.speclab.juxta.author.view.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.ImageIcon;

import edu.virginia.speclab.util.OSDetector;

public interface JuxtaUserInterfaceStyle
{
	// cursors
    public static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
    public static final Cursor HOTSPOT_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    public static final Cursor NORMAL_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
	
	// green
	public static final Color FIRST_COLOR_DARKER = new Color(0,128,0);
    public static final Color FIRST_COLOR_DARKER_WITH_ALPHA  = new Color(0,128,0,128);
	public static final Color FIRST_COLOR = new Color(186,226,186); 
	public static final Color FIRST_COLOR_BRIGHTER = new Color(222,242,222);
	public static final Color FIRST_COLOR_BRIGHTEST = new Color(126,222,126);
	
	// blue
	public static final Color SECOND_COLOR = new Color(0,12,252);
    public static final Color SECOND_COLOR_BRIGHTEST = new Color(239,240,255);
    
    // blue scale
    public static final Color[] SECOND_COLOR_SCALE = new Color[] 
    {
      new Color(239,240,255),
      new Color(213,217,255),
      new Color(187,193,255),
      new Color(161,170,255),
      new Color(135,146,255),
      new Color(109,123,255),
      new Color(83,99,255),
      new Color(57,76,255),
      new Color(51,71,250)
    };

    // beige
	public static final Color THIRD_COLOR = new Color(226,209,186);
    public static final Color THIRD_COLOR_LIGHTER = new Color(241,231,219);
    public static final Color THIRD_COLOR_WHITE = new Color(253,250,247);
    
    public static final Color TITLE_BACKGROUND_COLOR = OSDetector.getOperatingSystem() == OSDetector.MAC ? SECOND_COLOR_BRIGHTEST : THIRD_COLOR ;
	
	// fonts
    public static final Font TITLE_FONT = new Font("Verdana",Font.BOLD,14);
    public static final Font LARGE_FONT = new Font("Verdana",Font.BOLD,12);
    public static final Font NORMAL_FONT = new Font("Verdana",Font.PLAIN,12);
    public static final Font SMALL_FONT = new Font("Verdana",Font.PLAIN,10);
    
    public static final Font TEXT_FONT = NORMAL_FONT;
    
    public static final Color ADD_TEXT_COLOR = Color.BLUE;
    public static final Color DEL_TEXT_COLOR = new Color(128,0,0);
    
    public static final ImageIcon FILE_NEW = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/file.new.gif"));
    public static final ImageIcon FILE_OPEN = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/file.open.gif"));
    public static final ImageIcon FILE_SAVE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/save.gif"));
    public static final ImageIcon WS_EXPORT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/ws-export.png"));
    public static final ImageIcon LEFT_ARROW = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/left.arrow.gif"));
    public static final ImageIcon RIGHT_ARROW = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/right.arrow.gif"));
    public static final ImageIcon WINDOW_MODE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/window.mode.gif"));
    public static final ImageIcon LINE_MODE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/line.mode.gif"));
    public static final ImageIcon WORD_MODE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/word.mode.gif"));
    public static final ImageIcon SEARCH = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/search.gif"));
    public static final ImageIcon ADD_DOCUMENT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/add.document.gif"));
    public static final ImageIcon REMOVE_DOCUMENT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/remove.document.gif"));
    public static final ImageIcon RENAME_DOCUMENT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/rename.document.gif"));
    public static final ImageIcon HISTOGRAM = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/histogram.gif"));
    public static final ImageIcon EXIT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/exit.gif"));
	public static final ImageIcon REFRESH = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/refresh.gif"));
    public static final ImageIcon LOCK_WINDOWS = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/synch.mode.gif"));
    public static final ImageIcon UNLOCK_WINDOWS = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/unsynch_mode.gif"));
	public static final ImageIcon SELECTION_MODE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/selection.mode.gif"));
    public static final ImageIcon ONE_TO_MANY_VIEW = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/collation.view.gif"));
    public static final ImageIcon ONE_TO_ONE_VIEW = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/doc.compare.gif"));
    public static final ImageIcon BIBLIO_DATA = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/open.document.gif"));
	public static final ImageIcon LINE_NUMBER = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/line.number.gif"));
	public static final ImageIcon REMOVE_ANNOTATION = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/trash.can.gif"));
    public static final ImageIcon GLOBE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/globe.gif"));
    public static final ImageIcon ABOUT = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/about.gif"));
    public static final ImageIcon HELP = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/help.gif"));
    public static final ImageIcon REMOVE_MOVE = REMOVE_ANNOTATION;
    public static final ImageIcon EDIT_MOVE = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/trash.can.single.gif"));
    public static final ImageIcon MARK_AS_MOVED = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/mark.as.moved.gif"));
    public static final ImageIcon HIGHLIGHT_ALL_SEARCH_RESULTS = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/highlight.all.search.results.gif"));
    public static final ImageIcon DONT_HIGHLIGHT_ALL_SEARCH_RESULTS = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/dont.highlight.all.search.results.gif"));
    public static final ImageIcon SORT_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/sort.gif"));
    public static final ImageIcon VIEW_SOURCE_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/view_source.gif"));
    public static final ImageIcon TOGGLE_SOURCE_VIEW_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/toggle_source_view.jpg"));
        
    public static final ImageIcon DOCUMENT_TREE_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/document.tree.node.gif"));
    public static final ImageIcon MARKER_TREE_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/marker.tree.node.gif"));
    public static final ImageIcon IMAGE_MARKER_TREE_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/image.marker.tree.node.gif"));
        
    public static final ImageIcon BLANK_BOX = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/blank.box.gif"));
    public static final ImageIcon COLLATING_BOX = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/collating.box.gif"));
    public static final ImageIcon EMPTY_BOX = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/empty.box.gif"));
    
    public static final ImageIcon JUXTA_ICON = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/juxta.gif"));
    
    public static final ImageIcon JUXTA_LOGO = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/juxta_logo.png"));
    
    public static final ImageIcon WORKING_ANIMATION = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/timer_animation.gif"));
    public static final ImageIcon EXPORT_SUCCESS = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/success.png"));
    public static final ImageIcon EXPORT_FAIL = new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/fail.png"));

    
    /// comparison explorer color boxes
    public static final ImageIcon[] BOXES = new ImageIcon[] 
    {
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box1.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box2.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box3.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box4.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box5.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box6.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box7.gif")),
        new ImageIcon(JuxtaUserInterfaceStyle.class.getResource("/icons/box8.gif")),
    };
 
    
}
