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

import java.awt.Point;


/**
 * Models an offset range in the text of a MarkableTextPanel.
 * @author Nick
 *
 */
public class TextRange
{
    private int startOffset, endOffset;
    
    public TextRange( MarkableTextPanel panel, Point startPoint, Point endPoint )
    {
        startOffset = panel.getOffset(startPoint);
        endOffset = panel.getOffset(endPoint);

        // if we are backwards, fix it
        if( startOffset > endOffset )
        {
            int swap = startOffset;
            startOffset = endOffset;
            endOffset = swap;
        }            
    }
  
    public int getEndOffset()
    {
        return endOffset;
    }

    public int getStartOffset()
    {
        return startOffset;
    }
}
