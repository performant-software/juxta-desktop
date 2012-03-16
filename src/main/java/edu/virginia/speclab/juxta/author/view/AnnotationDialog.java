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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.CriticalApparatus;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.Lemma;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.DataForm;

/**
 * Displays the dialog for editing the information attached to a particular annotation. 
 * @author Nick
 *
 */
public class AnnotationDialog extends JDialog implements JuxtaUserInterfaceStyle
{
    private AnnotationDataForm formPanel;
    
    private boolean ok;

    public AnnotationDialog( Annotation annotation, JuxtaDocument baseDocument, JuxtaDocument witnessDocument, JFrame parent )
    {
        super(parent);
        
        setModal(true);
        setTitle("Edit Note");
        
        setBounds(parent.getX()+(parent.getWidth()/4), 
                parent.getY()+(parent.getHeight()/4), 300, 300);
        
        formPanel = new AnnotationDataForm(annotation,baseDocument,witnessDocument);
        
        getContentPane().add(formPanel,BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ));
        
        buttonPanel.add(Box.createGlue());
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        
        buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
        buttonPanel.add(cancelButton);

        buttonPanel.add(Box.createGlue());

        buttonPanel.setBorder( new EmptyBorder(5,5,5,5));
        
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        // jiggle the dialog into the correct shape
        pack();        
        Dimension size = getSize();
        setSize(size.width+150,size.height);
    }
    
    public boolean isOk()
    {
        return ok;
    }
    
    private void onCancel()
    {
        ok = false;
        dispose();
    }
    
    private void onOK()
    {
        ok = true;
        dispose();        
    }

    public String getNotes()
    {
        return formPanel.getNotes();
    }
    
    public boolean includeImage()
    {
        return formPanel.includeImage();
    }
       
    private class AnnotationDataForm extends DataForm implements JuxtaUserInterfaceStyle
    {
        public AnnotationDataForm( Annotation annotation, JuxtaDocument baseDocument, JuxtaDocument witnessDocument )
        {
            String lemmaText;
            if (!annotation.isFromOldVersion())
            {
                Lemma lemma = CriticalApparatus.generateLemma(annotation.getDifference(),baseDocument,witnessDocument);
                lemmaText = lemma.getLemmaText();
            }
            else
                lemmaText = "";

            String baseText = "Base: "+baseDocument.getDocumentName();
            String witnessText = "Witness: "+witnessDocument.getDocumentName();
            
            setFont(LARGE_FONT);
            addLabel("Lemma",lemmaText);
            setFont( SMALL_FONT );
            addLabel("Base",baseText);
            addLabel("Witness",witnessText);
            addSpacer(3);
            setShowLabel(false);
            addMemoField("Notes", annotation.getNotes(), 10 );
            
            Difference difference = annotation.getDifference();
            
            if( !hasImage( baseDocument, difference ) )
                setEnableFields(false);                
            
            addCheckBox("Include image",annotation.includeImage());
        }
        
        private boolean hasImage( JuxtaDocument document, Difference difference )
        {            
            if( document.getImageAt(difference.getOffset(Difference.BASE)) != null )
                return true;
            else
                return false;
        }
        
        public boolean includeImage()
        {
            String result = getFieldContent("Include image");
            if( result.equals("true") ) return true;
            else return false;
        }
        
        public String getNotes()
        {
            return getFieldContent("Notes");            
        }
    }
}
