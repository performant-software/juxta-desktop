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

import java.awt.image.BufferedImage;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.util.ImageLoader;

class DifferenceIconSet
{
    private static ImageLoader imageLoader = new ImageLoader(null);
    private static BufferedImage addIcon = imageLoader.loadImage("icons/diff.add.gif");
    private static BufferedImage delIcon = imageLoader.loadImage("icons/diff.del.gif");
    private static BufferedImage moveIcon = imageLoader.loadImage("icons/diff.move.gif");
    private static BufferedImage changeIcon = imageLoader.loadImage("icons/diff.delta.gif");
	
	private static BufferedImage starFilledIcon = imageLoader.loadImage("icons/star.filled.jpg");
	private static BufferedImage brightStarFilledIcon = imageLoader.loadImage("icons/bright.star.filled.jpg");

	private static BufferedImage starUnfilledIcon = imageLoader.loadImage("icons/star.unfilled.jpg");
	private static BufferedImage brightStarUnfilledIcon = imageLoader.loadImage("icons/bright.star.unfilled.jpg");

    private static BufferedImage downArrow = imageLoader.loadImage("icons/diff.down.arrow.gif");
    private static BufferedImage upArrow = imageLoader.loadImage("icons/diff.up.arrow.gif");
    
    public static BufferedImage getDownArrow()
    {
        return downArrow;
    }
    
    public static BufferedImage getUpArrow()
    {
        return upArrow;
    }
    
    public static BufferedImage getDifferenceIcon( int iconType )
    {
        BufferedImage iconImage;
        
        switch( iconType )
        {
            case Difference.INSERT:
                iconImage = addIcon;
                break;
            case Difference.DELETE:
                iconImage = delIcon;
                break;
            case Difference.MOVE:
                iconImage = moveIcon;
                break;
            case Difference.CHANGE:
                iconImage = changeIcon;
                break;
            default:
                iconImage = null;
                break;
        }
        
        return iconImage;
    }

	public static BufferedImage getStarIcon( boolean highlighted, boolean solid ) 
	{
		if( solid )
		{
			if( highlighted ) return brightStarFilledIcon;
			else return starFilledIcon;
		}
		else
		{
			if( highlighted ) return brightStarUnfilledIcon;
			else return starUnfilledIcon;
		}
	}
}
