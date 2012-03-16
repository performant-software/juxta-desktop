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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import edu.virginia.speclab.util.ImageLoader;
import edu.virginia.speclab.util.SimpleLogger;

public class GraphicalButtonTest extends JFrame 
{
    private static ImageLoader imageLoader = new ImageLoader(null);
    private static BufferedImage downImage = imageLoader.loadImage("icons/star.filled.jpg");
    private static BufferedImage upImage = imageLoader.loadImage("icons/star.unfilled.jpg");
    private static BufferedImage rollImage = downImage;
    
    private GraphicalButton button;
    private GraphicalButtonSet controller; 

    public static void main( String args[] )
    {        
        SimpleLogger.initConsoleLogging();
        new GraphicalButtonTest();
    }
    
    public GraphicalButtonTest()
    {
        setTitle("Graphical Button Test");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300,300);
        getContentPane().setLayout( new BorderLayout() );
        TestPanel testPanel = new TestPanel();
        getContentPane().add(testPanel,BorderLayout.CENTER);

        button = new GraphicalButton( new Point(150,150),  
                                      downImage, upImage, rollImage );
        
        button.setActionListener( new TestListener() );
        
        controller = new GraphicalButtonSet(testPanel);
        controller.addButton(button);
        
        setVisible(true);
    }
    
    private class TestPanel extends JPanel
    {
        public void paint( Graphics g )
        {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g;
            controller.paintButtons(g2);            
        }        
    }
    
    private class TestListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            SimpleLogger.logInfo("clicked!");
            controller.removeButton(button);    
        }               
    }
    
}
