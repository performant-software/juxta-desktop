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
 
package edu.virginia.speclab.juxta.author.view;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.util.ImageLoader;

/**
 * Displays the Juxta About dialog.
 * @author Nick
 *
 */
public class AboutDialog extends JDialog implements JuxtaUserInterfaceStyle
{
    private static ImageLoader imageLoader = new ImageLoader(null);
    private static BufferedImage SPLASH = imageLoader.loadImage("icons/splash.gif");
    
    public AboutDialog( JFrame parent )
    {
        super(parent);
        
        setModal(true);
        setTitle("About Juxta");
        
        setBounds(parent.getX()+(parent.getWidth()/4), 
                parent.getY()+(parent.getHeight()/4), 365, 146);
        setResizable(false);
        
        DrawPanel drawPanel = new DrawPanel();
                
        getContentPane().add(drawPanel,BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        drawPanel.setLayout(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton,BorderLayout.EAST);
        buttonPanel.setBorder( new EmptyBorder(3,3,3,3));
        
        drawPanel.add(buttonPanel,BorderLayout.SOUTH);

        // add an escape key handler 
        drawPanel.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"),"escape");
        drawPanel.getActionMap().put("escape",new EscapeAction());
    }

    private class DrawPanel extends JPanel
    {
        public void paint( Graphics g )
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(SPLASH,0,0,SPLASH.getWidth(),SPLASH.getHeight(),null);
            super.paintChildren(g);
        }
    }

    private class EscapeAction extends AbstractAction
    {        
        public void actionPerformed(ActionEvent e)
        {
            dispose();            
        }        
    }
}
