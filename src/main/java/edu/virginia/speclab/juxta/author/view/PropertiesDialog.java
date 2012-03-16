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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.templates.EditTemplateDialog;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

public class PropertiesDialog extends JDialog implements JuxtaUserInterfaceStyle
{
    private BiblioDataForm formPanel;
    private JComboBox templateCombo;
    private JuxtaDocument document;
    private boolean ok;
    private boolean templateChanged = false;
    private JuxtaAuthorFrame frame;

    public PropertiesDialog( JuxtaDocument juxtaDocument, JuxtaAuthorFrame parent )
    {
        super(parent);
        this.frame = parent;
        this.document = juxtaDocument;
        
        setModal(true);
        setTitle("Document Properties");
        
        setBounds(parent.getX()+(parent.getWidth()/4), 
                  parent.getY()+(parent.getHeight()/4), 
                  350, 390);
        
        JPanel content = new JPanel( new BorderLayout() );
        formPanel = new BiblioDataForm(juxtaDocument.getBiblioData(),true);
        JPanel templatePanel = createTemplatePanel();
        content.add(formPanel, BorderLayout.CENTER);
        content.add(templatePanel, BorderLayout.SOUTH);
                
        getContentPane().add(content, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ));
        buttonPanel.add(Box.createHorizontalGlue());
        
        JButton okButton = new JButton("OK");
        okButton.setFont(JuxtaUserInterfaceStyle.SMALL_FONT);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(JuxtaUserInterfaceStyle.SMALL_FONT);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        buttonPanel.add(cancelButton);
   
        buttonPanel.setBorder( new EmptyBorder(5,5,5,5));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createTemplatePanel() {
        JPanel p = new JPanel( new BorderLayout(10,5) );
        p.setBorder( BorderFactory.createEmptyBorder(5,7,5,5));
        JLabel l = new JLabel("<html>Parse<br/>Template:</html>");
        l.setFont(JuxtaUserInterfaceStyle.SMALL_FONT);
        p.add(l, BorderLayout.WEST);
        TemplateConfig cfg = TemplateConfigManager.getInstance().getConfig(ConfigType.SESSION);
        this.templateCombo = new JComboBox( cfg.toArray() );
        this.templateCombo.setFont(JuxtaUserInterfaceStyle.SMALL_FONT);
        p.add( this.templateCombo, BorderLayout.CENTER );
        
        // select the current document template in the dropdown
        ParseTemplate template = TemplateConfigManager.getInstance().getTemplate(
            ConfigType.SESSION, document.getParseTemplateGuid());
        this.templateCombo.setSelectedItem(template);
        
        JButton btn = new JButton("Edit");
        btn.setFont(JuxtaUserInterfaceStyle.SMALL_FONT);
        btn.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editTemplateClicked();
            }
        });
        p.add(btn, BorderLayout.EAST);
        return p;
    }

    protected void editTemplateClicked() {
        ParseTemplate currTemplate = (ParseTemplate)this.templateCombo.getSelectedItem(); 
        EditTemplateDialog dlg = new EditTemplateDialog( this.frame, false );
        dlg.setConfigType( ConfigType.SESSION );
        dlg.setup( this.document, currTemplate.getGuid() );
        dlg.setModal(true);
        dlg.setVisible(true);
        if ( dlg.wasOkClicked() ) {
            this.templateChanged = true;
        }
    }

    public boolean isOk() {
        return ok;
    }

    private void onCancel() {
        ok = false;
        dispose();
    }

    private void onOK() {
        ParseTemplate template = (ParseTemplate) this.templateCombo.getSelectedItem();
        if ( this.templateChanged == false ) {
            this.templateChanged = !(template.getGuid().equals(this.document.getParseTemplateGuid()));
        }
        this.document.setParseTemplateGuid(template.getGuid());
        ok = true;
        dispose();
    }
    
    public boolean wasTemplateChanged() {
        return this.templateChanged;
    }

    public BiblioData getBiblioData() {
        return formPanel.getData();
    }
}
