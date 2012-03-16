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
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.border.LineBorder;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;
import edu.virginia.speclab.ui.filetree.FileTreePanel;

public class FileTreeTitle extends PanelTitle implements JuxtaUserInterfaceStyle
{
	private JButton editRootButton;
	private FileTreePanel fileTreePanel;
	private JFrame parentFrame;
	
	private class EditRootAction extends AbstractAction
    {
        public EditRootAction()
        {
            
        	super("", FILE_OPEN);
            putValue( SHORT_DESCRIPTION, "Change the base directory");
        }

        public void actionPerformed(ActionEvent e)
        {
        	editRoot();
        }        
    }
	    	
    public FileTreeTitle( JFrame frame, FileTreePanel fileTreePanel )
    {
    	this.parentFrame = frame;
    	this.fileTreePanel = fileTreePanel;
        initUI();                        
    }
	
	private void initUI()
	{
		editRootButton = new JButton( new EditRootAction() );
		add( editRootButton, BorderLayout.EAST );
        setBorder(new LineBorder(Color.BLACK, 0, false));               		
	}
	
    private void editRoot()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        File myDir = fileTreePanel.getBaseDirectory();
        chooser.setCurrentDirectory(myDir);
        chooser.setDialogTitle("Select Base Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(parentFrame);
        if(option == JFileChooser.APPROVE_OPTION) 
        {
            File selectedDir = chooser.getSelectedFile();
            fileTreePanel.setBaseDirectory(selectedDir);
        }                   
    }
}
