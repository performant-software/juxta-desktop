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

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;

public class PanelTitle extends JPanel 
{
    protected JLabel title;
    
    public PanelTitle()
    {
        super();
        setSize(500, 37);

        initUI();
                        
        setBorder(new LineBorder(Color.BLACK, 0, false));               
    }
    
    public void setFont( Font font )
    {
        super.setFont(font);
        
        if( font != null && title != null ) 
            title.setFont(font);
    }
    
    public void setTitleText( String text )
    {
        title.setText(text);
    }

    private void initUI()
    {
        // Designer code
        title = new JLabel();
        title.setText(" ");
        title.setBorder(new EmptyBorder(2, 3, 2, 3));
        setLayout(new BorderLayout());
        add(title,BorderLayout.CENTER);        
        //
    }

}
