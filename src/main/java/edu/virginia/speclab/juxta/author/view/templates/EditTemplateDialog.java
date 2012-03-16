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

package edu.virginia.speclab.juxta.author.view.templates;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.templates.TemplateConfigTable.Mode;

/**
 * 
 * @author ben
 */
public final class EditTemplateDialog extends BaseTemplateDialog {
    private JLabel nameLabel;
    private JCheckBox defaultTemplateCkBox;
    private TemplateConfigTable configTable;
    private int originalIndex;

    public EditTemplateDialog(JuxtaAuthorFrame juxta, boolean canRename) {
        super(juxta);
        initUI(canRename);
    } 
    
    public EditTemplateDialog(JuxtaAuthorFrame juxta) {
        this(juxta, true);
    }

    /**
     * Initialize the processing dialog; create the UI and setup starting tables
     */
    private void initUI(boolean canRename) {
        setResizable(false);
        setVisible(false);
        setTitle("Edit Parse Template");
        setSize(400, 560);
        setLocationRelativeTo( getParent() );
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));

        // create al of the dlg components
        JPanel namePanel = createNamePanel(canRename);
        this.configTable = new TemplateConfigTable(Mode.EDIT);
        JPanel settingsPanel = createSettingsPanel();   
        JPanel buttonPanel = createButtonPanel();

        // add them to the UI
        getContentPane().add( namePanel );
        getContentPane().add( Box.createVerticalStrut(5));
        getContentPane().add(new JScrollPane(this.configTable ));
        getContentPane().add( Box.createVerticalStrut(5));
        getContentPane().add(settingsPanel);
        getContentPane().add( Box.createVerticalStrut(10));
        getContentPane().add(buttonPanel);         
    }

    private JPanel createNamePanel(boolean canRename) {
        JPanel namePnl = new JPanel( new BorderLayout(10,10));
        namePnl.add( new JLabel("Template Name:"), BorderLayout.WEST);
        this.nameLabel = new JLabel();
        namePnl.add(this.nameLabel, BorderLayout.CENTER);
        
        if ( canRename ) {
            JButton rename = new JButton("Rename");
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onRenameTemplate();
                }
            });
            namePnl.add(rename, BorderLayout.EAST);
        }
        return namePnl;
    }

    private JPanel createSettingsPanel() {
        JPanel browsePanel = new JPanel();
        browsePanel.setLayout(new BoxLayout(browsePanel, BoxLayout.LINE_AXIS));        
        this.defaultTemplateCkBox = new JCheckBox("Default Template");        
        JPanel settingsPanel = new JPanel( new BorderLayout(10,10) );
        settingsPanel.add(browsePanel, BorderLayout.CENTER);
        settingsPanel.add(this.defaultTemplateCkBox, BorderLayout.SOUTH );
        return settingsPanel;
    }
    
    /**
     * create the panel at the bottom of the dialog with the main control buttons
     * @return
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createGlue());
        
        JButton saveButton = new JButton("Save");
        getRootPane().setDefaultButton(saveButton);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSaveTemplate();
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        return buttonPanel;
    }
    
    @Override
    public void setup(final JuxtaDocument doc, final String guid) {
        super.setup(doc,guid);
  
        if ( guid != null && guid.length() >0 ) {
                   
            // always work with a clone of the template
            // so canceled changes don't persist
            ParseTemplate template = getConfig().get(guid).clone();
            
            // also save the origninal index of the template
            // so it can be found and updated when saving
            // index is necessary because the editing process here
            // can change everything about the template and we
            // wont be able to find it any other way.
            this.originalIndex = getConfig().indexOf(template);
    
            this.nameLabel.setText( template.getName() );
            this.defaultTemplateCkBox.setText("Default Template for <"+template.getRootTagName()+">");
            this.defaultTemplateCkBox.setSelected( template.isDefault() );
            this.configTable.setTemplate( template );
        }
    }
    
    private void onSaveTemplate() {
        try {
    
            // grab the copy of the template back from the edit table
            // it will contain all of the changes made there
            ParseTemplate template = this.configTable.getTemplate();
            template.rename( this.nameLabel.getText() );
            boolean isDefault = this.defaultTemplateCkBox.isSelected();
            getConfig().updateTemplate( this.originalIndex, template, isDefault);
            getConfig().save();
            
            this.okClicked = true;
            setVisible(false);
            
        } catch (ReportedException e ) {
            ErrorHandler.handleException(e);
        }
    }  
    
    private void onRenameTemplate() {
        String val = JOptionPane.showInputDialog(this, 
                "Enter the new template name:", 
                "Rename Template", 
                JOptionPane.OK_CANCEL_OPTION);
        if ( val != null) {
            this.nameLabel.setText(val);
        }
    }
    
    private void onCancel() {
        setVisible(false);
    }
}
