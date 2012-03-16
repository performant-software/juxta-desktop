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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;

public class TestTextLabel extends JFrame
{
    public TestTextLabel()
    {
        super("Text Label Test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300,300);
    }
    
    public void paint( Graphics g )
    {
        super.paint(g);
        
        String test = "this is a test of a longer string.";
        
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.BLACK);
        WrappingTextLabel label = new WrappingTextLabel( test, new Font("Verdana",Font.BOLD,12), Color.BLACK, 50, 3, g2.getFontRenderContext() );
        label.setLocation(100,100); 
        label.paint(g2);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        TestTextLabel test = new TestTextLabel();
        test.setVisible(true);
    }

}
