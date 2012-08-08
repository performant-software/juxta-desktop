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
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionFile;
import edu.virginia.speclab.juxta.author.model.LoaderCallBack;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.collation.DifferenceViewerListener;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;

/**
 * 
 * @author ben
 */
public class ViewSourcePanel extends JPanel implements JuxtaUserInterfaceStyle, DifferenceViewerListener {

    protected DocumentSourceCard sourceCard;
    protected PanelTitle titlePanel;
    protected JButton editButton;
    protected JButton cancelButton;
    protected JButton saveButton;
    private boolean incorporatingChanges= false;
    private JuxtaSession session;
    private JuxtaAuthorFrame juxtaFrame;

    public ViewSourcePanel(JuxtaAuthorFrame juxtaAuthorFrame) {
        super();
        this.juxtaFrame = juxtaAuthorFrame;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        this.titlePanel = new PanelTitle();
        this.titlePanel.setBorder( BorderFactory.createEmptyBorder(0,3,3,3));
        this.titlePanel.setFont(JuxtaUserInterfaceStyle.TITLE_FONT);
        this.titlePanel.setBackground(JuxtaUserInterfaceStyle.TITLE_BACKGROUND_COLOR);
        this.titlePanel.setTitleText("Document Source");
        add(titlePanel, BorderLayout.NORTH);

        this.sourceCard = new DocumentSourceCard();
        add(this.sourceCard, BorderLayout.CENTER);

        add(createToolbar(), BorderLayout.SOUTH);
    }
    
    @SuppressWarnings("serial")
    private JToolBar createToolbar() {
        JToolBar bar = new JToolBar();
        bar.setLayout( new BoxLayout(bar, BoxLayout.X_AXIS) );
        bar.setFloatable(false);
        
        this.editButton = new JButton("Edit");
        this.editButton.setEnabled(false);
        this.editButton.addActionListener( new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                enableEditing();
            }
        });
        
        this.cancelButton = new JButton("Cancel");
        this.cancelButton.setVisible(false);
        this.cancelButton.addActionListener( new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                cancelEdits();
            }
        });
        
        this.saveButton = new JButton("Update");
        this.saveButton.setVisible(false);
        this.saveButton.addActionListener( new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                incorporateChanges(true);
            }
        });
        
        bar.add( Box.createGlue());
        bar.add(this.cancelButton);
        bar.add( Box.createHorizontalStrut(3));
        bar.add(this.saveButton);
        bar.add(this.editButton);
        return bar;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.editButton.setEnabled(enabled);
    }

    public void setSession(JuxtaSession session) {
        this.session = session;
    }

    public void showDialog() {
        Container parent = this.getParent();
        setBounds(parent.getX() + 35, parent.getY() + 35, 600, 400);
        setVisible(true);
    }
    
    // Called when user clicks on text in the collation view
    // this offset is in ACTUAL space. Convert it to ORIGINAL
    // space and use it to scroll XML to the correct spot
    public void textClicked( final int offset ) {
        textClicked(offset, Space.ACTIVE, false);
    }
    public void textClicked( final int offset, Space space, final boolean highlightFullTag ) {
        
        int tgtOffset = offset;
        if ( space.equals(Space.ACTIVE)) {
            OffsetRange rng = new OffsetRange(this.sourceCard.getDocument(), offset, offset, Space.ACTIVE);
            tgtOffset = rng.getStartOffset(Space.ORIGINAL);
        }
        final int finalOffset = tgtOffset;
        SwingUtilities.invokeLater( new Runnable() {
            
            public void run() {
                sourceCard.highlightText(finalOffset, highlightFullTag);
            }
        });
    }
    
    public DocumentModel getDocument() {
        return this.sourceCard.getDocument();
    }

    public void documentChanged(JuxtaDocument document) {
        if ( hasPendingEdits() && this.incorporatingChanges == false) {
            int resp = JOptionPane.showConfirmDialog(this, 
                    "You have spot edits in \"" 
                    + this.sourceCard.getDocument().getDocumentName()
                    + "\" that have not incorporated into the collation.\nWould you like to do this now?", 
                    "Incorporate Changes", 
                    JOptionPane.YES_NO_OPTION);
            if ( resp == JOptionPane.YES_OPTION ) {
                incorporateChanges(false);
            }
        }
        
        // make sure toolbar is always in its
        // default state (showing edit only) when doc changes
        resetToolbar();   
        
        this.editButton.setEnabled( document != null );
        this.sourceCard.setDocument(document);
        if ( document != null ) {
            this.titlePanel.setTitleText(document.getDocumentName()+" - Source");
        } else {
            this.titlePanel.setTitleText("Document Source");
        }
    }
    
    private boolean hasPendingEdits() {
        if ( this.saveButton.isVisible()) {
            return this.sourceCard.hasPendingEdits();
        }
        return false;
    }

    /**
     * Cancel all outstanding edits on the active view
     */
    private void cancelEdits() {
        this.sourceCard.undoAllEdits();
        resetToolbar();
    }

    private void resetToolbar() {
        this.sourceCard.setEditable(false);
        this.saveButton.setVisible(false);
        this.cancelButton.setVisible(false);
        this.editButton.setVisible(true);

        this.juxtaFrame.lockdownUI( this, false );
    }
    
    /**
     * Enable edits on the active view panel
     */
    private void enableEditing() {
        this.sourceCard.setEditable(true);
        this.saveButton.setVisible(true);
        this.cancelButton.setVisible(true);
        this.editButton.setVisible(false);
        this.juxtaFrame.lockdownUI( this, true );
    }

    /**
     * Save any changes made in spot editing. Pass the reloadSession as true
     * to reload the current session with the newly edited document as the base.
     * 
     * @param reloadSession
     */
    private void incorporateChanges(boolean reloadSession) {
        
        // get the original doc based on the activeID
        int origDocID = this.sourceCard.getDocument().getID();
        JuxtaDocument origDoc = this.session.getDocumentManager().lookupDocument(origDocID);
        String templateGuid = origDoc.getParseTemplateGuid();
        
        if ( origDoc.hasAcceptedRevisions() ) {
            int resp = JOptionPane.showConfirmDialog(this, "This document has previously accepted revisions." +
            		"\nSaving these edits will revert all previously accepted revsions." +
            		"\nAre you sure you want to save?", 
                "Revisions Warning", JOptionPane.YES_NO_OPTION);
            if ( resp == JOptionPane.NO_OPTION ) {
                return;
            } else {
                origDoc.setAcceptedRevisions("");
            }
        }
        String name = origDoc.getDocumentName();
        int dotPos = name.lastIndexOf('.');
        if ( dotPos > -1 ) {
            name = name.substring(0, dotPos)+"-edit";
        } else {
            name = name + "-edit";
        }
        String newName = JOptionPane.showInputDialog(
            "Enter name for the revised document:", name);
        if ( newName == null) {
            return;
        }
        
        if ( origDoc.getSourceDocument().isXml() ) {
            newName = newName + ".xml";
        } else {
            newName = newName + ".txt";
        }
        
        // flag a save in progress to the session reload
        // doesn't incorrectly trigger a prompt to save again
        this.incorporatingChanges = true;
        
        try {
            // dump the raw, edited XML to a temp file
            File tmp = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + newName);
            tmp.deleteOnExit();
            Writer out = new OutputStreamWriter(new FileOutputStream(tmp) );
            out.write( this.sourceCard.getRawXmlText());
            out.flush();
            out.close();

            // reconstruct the doc based on the edited content file
            JuxtaDocument edited = session.getDocumentManager().constructDocument(newName, tmp.getAbsolutePath());
    
            // lastly, reparse it against the prior template to keep results the same
            JuxtaDocumentFactory factory = new JuxtaDocumentFactory(edited.getEncoding());
            factory.reparseDocument(edited, 
                TemplateConfigManager.getInstance().getTemplate(ConfigType.SESSION, templateGuid));
            BiblioData newBib = new BiblioData(origDoc.getBiblioData());
            newBib.setShortTitle(newName);
            edited.setBiblioData(newBib);
            this.session.addExistingDocument(edited, new LoaderCallBack() {
                public void loadingComplete() {
                }
            });
            
            this.saveButton.setVisible(false);
            this.cancelButton.setVisible(false);
            this.editButton.setVisible(true);
            this.sourceCard.setEditable(false);

        } catch (Exception ex) {

            String msg = "These changes not been incorporated due to the following problem:\n\n"
                + "     "+ ex.toString() +"\n\n"
                + "Please correct this and try updating again.";
            
            JOptionPane.showMessageDialog(null, msg, "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
       
        this.incorporatingChanges = false;
    }
}
