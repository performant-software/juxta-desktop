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
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;

import org.apache.commons.lang.StringUtils;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

/**
 *
 * @author ben
 */
public class DocumentSourceCard extends JPanel {

    private DocumentModel document;
    private DocumentSourceTextArea dsTextArea;
    private JScrollPane textScroller;

    public DocumentSourceCard() {
        setLayout(new BorderLayout());
        this.dsTextArea = new DocumentSourceTextArea();
        this.textScroller = new JScrollPane(this.dsTextArea);
        add(this.textScroller, BorderLayout.CENTER);
    }

    public void setEditable(boolean editable) {
        this.dsTextArea.setEditable(editable);
        if (editable) {
            this.textScroller.setBorder(BorderFactory.createLineBorder(Color.RED));
        } else {
            this.textScroller.setBorder(null);
        }
    }

    public boolean hasPendingEdits() {
        return this.dsTextArea.hasEdits();
    }

    public void undoAllEdits() {
        this.dsTextArea.undoAllEdits();
    }

    public boolean isEditable() {
        return this.dsTextArea.isEditable();
    }

    public String getRawXmlText() {
        return this.dsTextArea.getText();
    }

    public DocumentModel getDocument() {
        return this.document;
    }

    public void setDocument(DocumentModel document) {
        this.document = document;

        if (document != null && document.getSourceDocument() != null) {
            dsTextArea.setText(document.getSourceDocument().getRawXMLContent());
            dsTextArea.setCaretPosition(0);
        } else {
            dsTextArea.setText("");
        }
    }

    /**
     * Scroll the text area to center on the specified offset and
     * highlight the xml tag found there.
     * 
     * @param origOffset
     */
    public void highlightText(int origOffset, boolean fullTag) {
        try {
            this.dsTextArea.setSelectedTextColor(JuxtaUserInterfaceStyle.SECOND_COLOR);
            this.dsTextArea.grabFocus();
            this.dsTextArea.getHighlighter().removeAllHighlights();
            boolean foundStart = false;
            boolean foundEnd = false;
            int s = origOffset;
            int e = origOffset;
            int len = this.getDocument().getSourceDocument().getRawXMLContent().length();
            while (!(foundEnd == true && foundStart == true)) {
                if (foundStart == false) {
                    if (s <= 0) {
                        foundStart = true;
                    } else {
                        char sc = this.getDocument().getSourceDocument().getRawXMLContent().charAt(--s);
                        if ( fullTag == false ) {
                            if (StringUtils.isAlphanumeric(String.valueOf(sc)) == false) {
                                foundStart = true;
                            }
                        } else {
                            foundStart = ( sc == '>');
                        }
                    }
                }

                if (foundEnd == false) {
                    if (e >= len - 1) {
                        foundEnd = true;
                    } else {
                        char ec = this.getDocument().getSourceDocument().getRawXMLContent().charAt(++e);
                        if ( fullTag == false ) {
                            if (StringUtils.isAlphanumeric(String.valueOf(ec)) == false) {
                                foundEnd = true;
                            }
                        } else {
                            foundEnd = ( ec == '<');
                        }
                    }
                }
            }
            this.dsTextArea.setSelectionStart(s + 1);
            this.dsTextArea.setSelectionEnd(e);
            int txtCenter = s + (e - s) / 2;

            // first scroll to start, then to specified offset.
            // this allows back scrolling to work
            this.dsTextArea.scrollRectToVisible(this.dsTextArea.modelToView(0));
            Rectangle comparandRect = this.dsTextArea.modelToView(txtCenter);

            if (comparandRect != null) {
                Rectangle viewRect = this.dsTextArea.getVisibleRect();
                comparandRect.y += viewRect.height / 2;
                this.dsTextArea.scrollRectToVisible(comparandRect);
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

    }
}
