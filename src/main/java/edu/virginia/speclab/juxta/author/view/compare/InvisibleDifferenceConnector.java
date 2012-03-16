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

import java.awt.Graphics2D;

public class InvisibleDifferenceConnector extends DifferenceConnector {

	public InvisibleDifferenceConnector(int baseTextOffset, int leftPosition, int leftLength, int rightPosition, int rightLength) 
	{
		super(baseTextOffset, leftPosition, leftLength, rightPosition, rightLength, DifferenceConnector.DIFFERENCE_STYLE);
	}
	
    public void render( Graphics2D g2, int connectorLaneWidth, int viewHeight, int leftViewPosition, int rightViewPosition )
    {
    	//don't actually render -- this is used for aligning fragments
    }

}
