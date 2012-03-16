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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.templates.TemplateConfigTable.Mode;

/**
 * Show a read-only list of templates availble for the currently selected document.
 * Allow the user to select a new parse template or launch the edit templates view
 */
public final class SelectTemplateDialog extends BaseTemplateDialog {

    private JLabel rootLabel;
    private JLabel defaultLabel;
    private JComboBox templateSelect;
    private ComboModel templateSelectModel;
    private TemplateConfigTable configTable;

    public SelectTemplateDialog(JuxtaAuthorFrame juxta) {
        super(juxta);  
        initUI();
    }

    /**
     * Initialize the processing dialog; create the UI and setup starting tables
     */
    private void initUI() {
        setResizable(false);
        setVisible(false);
        setTitle("Select Parse Template");
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        setSize(400, 600);
        setLocationRelativeTo( getParent() );
        getContentPane().setLayout(new BorderLayout(0,10));

        // template select
        JPanel templatePanel = createTemplatePanel(); 
        getContentPane().add(templatePanel, BorderLayout.NORTH);

        // table of template tag behavior
        JPanel tablePanel = createConfigPanel();
        getContentPane().add(tablePanel, BorderLayout.CENTER);

        // template cfg; default, root tag, xpath
        JPanel dataPnl = createDataPanel();
        
        // main ok/cancel buttons
        JPanel buttonPanel = createButtonPanel();

        JPanel bottom = new JPanel();
        bottom.setLayout( new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(dataPnl);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(buttonPanel);
        
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }
    

    /**
     * init the extra template data panel. Root tag, xpath, is default
     * @return
     */
    private JPanel createDataPanel() {
        JPanel labelPnl = new JPanel();
        labelPnl.setLayout(new BoxLayout(labelPnl, BoxLayout.Y_AXIS));
        labelPnl.add( new JLabel("Target XPath : ") );
        labelPnl.add( new JLabel("Schema : ") );
        labelPnl.add( new JLabel("Default for Schema : ") );
        
        JPanel dataPnl = new JPanel();
        dataPnl.setLayout(new BoxLayout(dataPnl, BoxLayout.Y_AXIS));  
        this.defaultLabel = new JLabel(); 
        this.rootLabel = new JLabel();
        dataPnl.add(this.rootLabel);
        dataPnl.add(this.defaultLabel);
        
        JPanel stuff = new JPanel( new BorderLayout(15,5));
        stuff.add(labelPnl, BorderLayout.WEST );
        stuff.add(dataPnl, BorderLayout.CENTER );
        
        return stuff;
    }
    
    private JPanel createConfigPanel() {
        
        JPanel tablePanel = new JPanel( new BorderLayout(0,10) );
        tablePanel.add( new JLabel("Template Configuration:"), BorderLayout.NORTH);
        this.configTable = new TemplateConfigTable( Mode.READ_ONLY );
        tablePanel.add( new JScrollPane(this.configTable), BorderLayout.CENTER );
        return tablePanel;
    }
   
    /**
     * Create the top panel with template select combo
     * @return
     */
    private JPanel createTemplatePanel() {

        JPanel templatePnl = new JPanel(new BorderLayout(5, 0));
        templatePnl.add(new JLabel("XML Parsing Template:"), BorderLayout.WEST);
        this.templateSelectModel = new ComboModel();
        this.templateSelectModel.setTemplates( getConfig() ); 
        this.templateSelect = new JComboBox( this.templateSelectModel );
        this.templateSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTemplateSelect();
            }
        });
        templatePnl.add(this.templateSelect, BorderLayout.CENTER);

        return templatePnl;
    }
    
    /**
     * create the panel at the bottom of the dialog with the main control buttons
     * @return
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder( BorderFactory.createEmptyBorder(15,0,0,0));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        JButton okButton = new JButton("OK");
        getRootPane().setDefaultButton( okButton );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        
        JButton editButton = new JButton("Edit Templates...");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onEdit();
            }
        });
        
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        

        return buttonPanel;
    }
    
    
    @Override
    public void setup( JuxtaDocument doc, String templateGuid ) {
        super.setup(doc, templateGuid);
        String rootEle = doc.getSourceDocument().getXMLRoot().getName();
        this.templateSelectModel.setTemplates( getFilteredTemplateList(rootEle));
        updateUI();
    }

    private void onTemplateSelect() {
        ParseTemplate t = (ParseTemplate)this.templateSelect.getSelectedItem();
        if ( t != null ) {
            this.templateGuid = t.getGuid();
            updateUI();
        }
    }

    private void updateUI() {
        if ( this.templateGuid != null && this.templateGuid.length() > 0 ) {
            ParseTemplate t = getConfig().get(this.templateGuid);
            if  ( t == null ) {
                // this occurs when the current template has
                // been deleted by the edit dialog. in this case,
                // set the template name to the default for this
                // document type
                String root = this.tgtDocument.getSourceDocument().getXMLRoot().toString();
                t = getConfig().getDefaultTemplate(root);
                this.templateGuid = t.getGuid();
            }
            
            if ( this.templateSelectModel.getSize() > 0 ) {
                this.templateSelect.setSelectedItem(t);
            }
            
            this.configTable.setTemplate( t );
            this.rootLabel.setText( t.getRootTagName() );
            this.defaultLabel.setText( Boolean.toString( t.isDefault() ));
            
            if ( this.tgtDocument != null ) {
                String root = this.tgtDocument.getSourceDocument().getXMLRoot().toString();
                List<ParseTemplate> list = getFilteredTemplateList(root);
                this.templateSelectModel.setTemplates( list ); 
            }
            
            this.templateSelectModel.refresh();
            this.templateSelect.setSelectedItem( t );
        }
    }
    
    private void onEdit() {             
        ListTemplatesDialog dlg = new ListTemplatesDialog( this.juxta);
        dlg.setTargetDocument( this.tgtDocument );
        dlg.setModal(true);
        dlg.setVisible(true);
        updateUI(); 
    }

    private void onOK() {        
        this.okClicked = true;
        setVisible(false);
    }

    private void onCancel() {
        setVisible(false);
    }
    
    /**
     * Data model for the template combo box. Just a wrapper around the config
     */
    class ComboModel extends AbstractListModel implements ComboBoxModel {
        private List<ParseTemplate>  templates;
        private Object selected;
        
        public ComboModel( ) {
            this.templates = new ArrayList<ParseTemplate>();
            this.selected = null;
        }
        public void setTemplates( List<ParseTemplate>  templates) {
            this.templates = templates;
            this.fireContentsChanged(this, 0, getSize() );
        }
        public void refresh() {
            this.fireContentsChanged(this, 0, this.templates.size());
        }
        public Object getElementAt(int index) {
            return this.templates.get(index);
        }

        public int getSize() {
            return this.templates.size();
        }
        public Object getSelectedItem() {
            return this.selected;
        }
        public void setSelectedItem(Object anItem) {
            this.selected = anItem;
        }
    }
}
