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

import java.awt.Dimension;
import java.io.File;

import javax.swing.JFileChooser;

import edu.virginia.speclab.juxta.author.Juxta;
import edu.virginia.speclab.util.OSDetector;

/**
 * Displays the Add Document Dialog, which is an JFileChooser with a custom data pane.
 * @author Nick
 *
 */
public class AddDocumentDialog extends JFileChooser
{
    private BiblioDataForm dataForm;
    
    public AddDocumentDialog()
    {
        setPreferredSize( new Dimension(600,300) );
        
        if( OSDetector.getOperatingSystem() == OSDetector.MAC ) {
            setPreferredSize( new Dimension(700,500) );        	
        }
        
        File currentDirectory = Juxta.selectStartDirectory(false);
        setCurrentDirectory(currentDirectory);

        // Since we want to open the DocumentProcessingSettingsDialog
        // for each document that is added, we need to limit this dialog
        // to only allowing a single selection
        setMultiSelectionEnabled(false);

        dataForm = new BiblioDataForm(this);
		dataForm.addSpacer(10);
        setAccessory( dataForm );        
    }     
}
