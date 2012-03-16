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
 

package edu.virginia.speclab.juxta.author.view.ui;

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.border.EmptyBorder;

public class StatusBar extends JPanel implements JuxtaUserInterfaceStyle
{
    private JLabel statusBar;
    
    public StatusBar()
    {
        super();
        initUI();
    }
    
    public void setText( String text )
    {
        statusBar.setText(text);
    }

    private void initUI()
    {
        // Designer code
        statusBar = new JLabel();
        setLayout(new BorderLayout());
        add(statusBar,BorderLayout.CENTER);        
        setBorder(new EmptyBorder(3, 2, 2, 3));
        statusBar.setText("Status Bar");
        //
        statusBar.setFont(SMALL_FONT);
    }

}
