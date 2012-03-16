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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.SpringLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import javax.swing.WindowConstants;

public class CollationDialog extends JDialog implements JuxtaUserInterfaceStyle
{
    private TokenizerSettings settings;
    
    private JCheckBox caseCheckBox;
    private JCheckBox punctuationCheckBox;
    private JCheckBox whiteSpaceCheckBox;
    private SpringLayout springLayout;
    
    private boolean ok;

    public CollationDialog( TokenizerSettings settings, JuxtaAuthorFrame frame )
    {
        this.settings = settings;
        initUI(frame);
    }
    
    private void initUI( JuxtaAuthorFrame frame )
    {
        setBounds(frame.getX()+(frame.getWidth()/4), 
                frame.getY()+(frame.getHeight()/4), 371, 237);
        setResizable(false);

        setTitle("Collate Documents");
        springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        // Designer code
        final JButton collateButton = new JButton();
        getContentPane().add(collateButton);
        springLayout.putConstraint(SpringLayout.SOUTH, collateButton, -25, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, collateButton, 15, SpringLayout.WEST, getContentPane());
        collateButton.setText("Collate");
        collateButton.setFont(LARGE_FONT);
        // Designer code
        final JButton cancelButton = new JButton();
        getContentPane().add(cancelButton);
        springLayout.putConstraint(SpringLayout.SOUTH, cancelButton, -25, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, cancelButton, -19, SpringLayout.EAST, getContentPane());
        cancelButton.setText("Cancel");
        cancelButton.setFont(NORMAL_FONT);
        // Designer code
        whiteSpaceCheckBox = new JCheckBox();
        getContentPane().add(whiteSpaceCheckBox);
        springLayout.putConstraint(SpringLayout.SOUTH, whiteSpaceCheckBox, 60, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, whiteSpaceCheckBox, 0, SpringLayout.EAST, cancelButton);
        springLayout.putConstraint(SpringLayout.WEST, whiteSpaceCheckBox, 0, SpringLayout.WEST, collateButton);
        whiteSpaceCheckBox.setSelected(settings.filterWhitespace());
        whiteSpaceCheckBox.setText("Ignore whitespace");
        whiteSpaceCheckBox.setFont(NORMAL_FONT);
        // Designer code
        punctuationCheckBox = new JCheckBox();
        getContentPane().add(punctuationCheckBox);
        springLayout.putConstraint(SpringLayout.SOUTH, punctuationCheckBox, 90, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, punctuationCheckBox, 0, SpringLayout.EAST, whiteSpaceCheckBox);
        springLayout.putConstraint(SpringLayout.WEST, punctuationCheckBox, 0, SpringLayout.WEST, collateButton);
        punctuationCheckBox.setSelected(settings.filterPunctuation());
        punctuationCheckBox.setText("Ignore punctuation");
        punctuationCheckBox.setFont(NORMAL_FONT);
        // Designer code
        caseCheckBox = new JCheckBox();
        getContentPane().add(caseCheckBox);
        springLayout.putConstraint(SpringLayout.SOUTH, caseCheckBox, 28, SpringLayout.SOUTH, punctuationCheckBox);
        springLayout.putConstraint(SpringLayout.EAST, caseCheckBox, 0, SpringLayout.EAST, punctuationCheckBox);
        springLayout.putConstraint(SpringLayout.WEST, caseCheckBox, 0, SpringLayout.WEST, collateButton);
        springLayout.putConstraint(SpringLayout.NORTH, caseCheckBox, 5, SpringLayout.SOUTH, punctuationCheckBox);
        caseCheckBox.setSelected(settings.filterCase());
        caseCheckBox.setText("Ignore case");
        caseCheckBox.setFont(NORMAL_FONT);
        // Designer code
        final JLabel instructions = new JLabel();
        getContentPane().add(instructions);
        springLayout.putConstraint(SpringLayout.EAST, instructions, 0, SpringLayout.EAST, whiteSpaceCheckBox);
        springLayout.putConstraint(SpringLayout.NORTH, instructions, 5, SpringLayout.NORTH, getContentPane());
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        springLayout.putConstraint(SpringLayout.WEST, instructions, 0, SpringLayout.WEST, collateButton);
        instructions.setText("Select the filters to apply and then press Collate.");
        instructions.setFont(NORMAL_FONT);
        //

        cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleCancel();                
            }        
        });

        collateButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleCollate();                
            }        
        });
    }
    
    private void handleCancel()
    {
        dispose();
    }
    
    private void handleCollate()
    {        
        boolean filterCase = this.caseCheckBox.isSelected();
        boolean filterPunctuation = this.punctuationCheckBox.isSelected();
        boolean filterWhitespace = this.whiteSpaceCheckBox.isSelected();
        
        settings = new TokenizerSettings( filterCase,
                                          filterPunctuation, 
                                          filterWhitespace );
        
        ok = true;
        
        dispose();
    }

    public TokenizerSettings getSettings()
    {
        return settings;
    }

    public boolean isOk()
    {
        return ok;
    }

}
