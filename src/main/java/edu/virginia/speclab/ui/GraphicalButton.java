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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class GraphicalButton
{
    private Point location; 
    private boolean rolledOver, depressed;
    private ActionListener action;
    private BufferedImage upImage,downImage,rolloverImage;
    private Rectangle hitRectangle;
    
    public GraphicalButton( Point location, BufferedImage downImage, BufferedImage upImage, BufferedImage rollImage )
    {
        this.location = location;
        this.upImage = upImage;
        this.downImage = downImage;
        this.rolloverImage = rollImage;
        this.rolledOver = false;
        this.depressed = false;
        
        this.hitRectangle = new Rectangle( location.x, 
                                        location.y, 
                                        upImage.getWidth(),
                                        upImage.getHeight() );
    }
    
    public void onPress()
    {
        depressed = true;    
        if( action != null ) action.actionPerformed(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,""));
    }
    
    public void onRelease()
    {
        depressed = false;
    }
    
    public void onRollOver()
    {
        rolledOver = true;
    }
    
    public void onRollOut()
    {
        rolledOver = false;
    }
    
    public void paint( Graphics2D g2 )
    {
        if( depressed )
        {
            g2.drawImage(downImage,location.x,location.y,null);    
        }
        else if( rolledOver )
        {
            g2.drawImage(rolloverImage,location.x,location.y,null);
        }
        else
        {
            g2.drawImage(upImage,location.x,location.y,null);
        }
    }

    public boolean testHit( Point p )
    {
        return hitRectangle.contains(p);
    }

    public void setActionListener(ActionListener action)
    {
        this.action = action;
    }
    
    
}
