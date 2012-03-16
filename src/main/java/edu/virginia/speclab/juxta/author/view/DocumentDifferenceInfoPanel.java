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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author ben
 */
public class DocumentDifferenceInfoPanel extends JPanel
{
    private JLabel documentNameLabel, documentNameValueLabel;
    private JLabel xpathLabel, xpathValueLabel;
    private JLabel documentTypeLabel, documentTypeValueLabel;
    private Font font;

    public DocumentDifferenceInfoPanel() {
        initUI();
    }

    public void setDocumentName(String name)
    {
        documentNameValueLabel.setText(name);
    }

    public void setXPath(String xpath)
    {
        xpathValueLabel.setText(xpath);
    }

    public void setDocumentType(String type)
    {
        documentTypeValueLabel.setText(type);
    }

    private void initUI()
    {
        setBounds(0,0,300,300);

        this.font = new Font("Verdana",Font.PLAIN,10);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.ipadx = 20;
        documentNameLabel = new JLabel("Document name:");
        documentNameLabel.setFont(font);
        documentNameLabel.setBorder( new EmptyBorder(2,2,2,2));
        add(documentNameLabel, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        documentNameValueLabel = new JLabel();
        documentNameValueLabel.setFont(font);

        add(documentNameValueLabel, c);

        c.gridwidth = 1;
        xpathLabel = new JLabel("Current Difference XPath:");
        xpathLabel.setFont(font);
        xpathLabel.setBorder( new EmptyBorder(2,2,2,2));
        add(xpathLabel, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        xpathValueLabel = new JLabel();
        xpathValueLabel.setFont(font);
        add(xpathValueLabel, c);

        c.gridwidth = 1;
        documentTypeLabel = new JLabel("Document type:");
        documentTypeLabel.setFont(font);
        documentTypeLabel.setBorder( new EmptyBorder(2,2,2,2));
        add(documentTypeLabel, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        documentTypeValueLabel = new JLabel();
        documentTypeValueLabel.setFont(font);
        add(documentTypeValueLabel, c);
    }

}
