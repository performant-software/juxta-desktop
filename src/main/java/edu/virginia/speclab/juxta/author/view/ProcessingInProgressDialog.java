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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author ben
 */
class ProcessingInProgressDialog extends JDialog {

    String message;

    public ProcessingInProgressDialog(Frame owner, String message)
    {
        super(owner);
        this.message = message;
        init();
    }

    private void init()
    {

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());


        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JProgressBar progBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progBar.setIndeterminate(true);
        panel.add(progBar, BorderLayout.CENTER);
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Color.black), new EmptyBorder(10,10,10,10)));

        contentPane.add(panel, BorderLayout.CENTER);
        this.setResizable(false);

        this.setUndecorated(true);
    }

    public void showDialog()
    {
        Component parent = this.getParent();
        int width = 200;
        int height = 100;
        setBounds( parent.getX() + (parent.getWidth()-width)/2, parent.getY() + (parent.getHeight()-height)/2, width, height );

        this.setModal(true);
        this.setVisible(true);
    }
}
