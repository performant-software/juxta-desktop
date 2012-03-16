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
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.BadLocationException;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.LoaderCallBack;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.collation.HighlightManager;
import edu.virginia.speclab.juxta.author.view.templates.BaseTemplateDialog;
import edu.virginia.speclab.juxta.author.view.templates.EditTemplateDialog;
import edu.virginia.speclab.juxta.author.view.templates.SelectTemplateDialog;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;
import edu.virginia.speclab.util.SimpleLogger;

class DocumentViewer extends JPanel implements JuxtaUserInterfaceStyle {

    private JTextPane textArea;
    private AddDocumentAction addDocumentAction;
    private JuxtaDocument currentDocument;
    private JuxtaAuthorFrame juxtaFrame;
    private JButton addDocumentButton;
    private FragmentSelectionTracker selectionTracker;
    private JuxtaSession juxtaSession;
    private PanelTitle titlePnl;

    public static int DOCUMENT_SELECT = 0;
    public static int FRAGMENT_SELECT = 1;

    public DocumentViewer(JuxtaAuthorFrame juxtaFrame) {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        this.juxtaFrame = juxtaFrame;
        addDocumentAction = new AddDocumentAction();

        // title
        this.titlePnl = new PanelTitle();
        this.titlePnl.setFont(TITLE_FONT);
        this.titlePnl.setBackground(TITLE_BACKGROUND_COLOR);

        // text
        JScrollPane txtPnl = createTextPanel();

        add(this.titlePnl, BorderLayout.NORTH);
        add(txtPnl, BorderLayout.CENTER);
        add(createToolbarPanel(), BorderLayout.SOUTH);
    }

    public JPanel createToolbarPanel() {
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        addDocumentButton = new JButton(addDocumentAction);
        toolbar.add(addDocumentButton);
        toolbarPanel.add(toolbar, BorderLayout.EAST);
        return toolbarPanel;
    }

    private JScrollPane createTextPanel() {

        this.textArea = new JTextPane();
        this.textArea.setMargin(new Insets(5, 5, 5, 5));

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.validate();

        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                scrollBar.setValue(scrollBar.getValue() + (e.getUnitsToScroll() * 5));
            }
        });

        // highlighting text by clicking and dragging can clobber the view, repaint
        textArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                updateAddDocumentButton(DOCUMENT_SELECT);
                repaint();
            }
        });

        // highlighting text by clicking and dragging can clobber the view, repaint
        textArea.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                updateAddDocumentButton(FRAGMENT_SELECT);
                repaint();
            }
        });

        selectionTracker = new FragmentSelectionTracker(textArea);
        textArea.addMouseListener(selectionTracker);
        textArea.addMouseMotionListener(selectionTracker);

        return scrollPane;
    }

    private void updateAddDocumentButton(int mode) {
        if (addDocumentButton == null)
            return;

        if (mode == FRAGMENT_SELECT) {
            addDocumentButton.setText("Add Selected Fragment");
        } else {
            addDocumentButton.setText("Add Document");
        }
    }

    public void centerOffset(int offset) {
        textArea.setCaretPosition(offset);

        try {
            // scroll to the beginning of the doc 
            Rectangle startOfDoc = textArea.modelToView(0);

            if (startOfDoc != null) {
                scrollRectToVisible(startOfDoc);

                // scroll to the selected position
                Rectangle comparandRect = textArea.modelToView(offset);

                if (comparandRect != null) {
                    Rectangle viewRect = getVisibleRect();
                    comparandRect.y += viewRect.height / 2;
                    scrollRectToVisible(comparandRect);
                }
            }
        } catch (BadLocationException e) {
            SimpleLogger.logError("Unable to scroll to offset: " + offset);
        }
    }

    public void setDocument(JuxtaDocument document) {
        currentDocument = document;
        textArea.setText(document.getDocumentText());
        updateAddDocumentButton(DOCUMENT_SELECT);
        centerOffset(0);
        this.titlePnl.setTitleText(document.getDocumentName());
    }

    public void setSession(JuxtaSession session) {
        this.juxtaSession = session;
    }

    private class AddDocumentAction extends AbstractAction implements LoaderCallBack {

        private ProcessingInProgressDialog processingDlg;
        
        public AddDocumentAction() {
            super("Add Document", ADD_DOCUMENT);
            putValue(SHORT_DESCRIPTION, "Add Document to Comparison Set");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                OffsetRange fragment = null;
                if (selectionTracker.isFragmentSelected()) {
                    fragment = new OffsetRange(currentDocument, selectionTracker.getFragmentStart(),
                        selectionTracker.getFragmentStart() + selectionTracker.getFragmentLength(),
                        OffsetRange.Space.ACTIVE);
                }

                JuxtaDocumentFactory factory = new JuxtaDocumentFactory(currentDocument.getEncoding());
                if (currentDocument != null ) {
                    
                    if ( currentDocument.isXML() == false) {
                        if (fragment != null) {
                            currentDocument.setActiveRange(fragment);
                            processingDlg = new ProcessingInProgressDialog(juxtaFrame, "Adding fragment...");
                        } else {
                            processingDlg = new ProcessingInProgressDialog(juxtaFrame, "Adding document...");
                        }
                        juxtaSession.addExistingDocument(currentDocument, this);
                        processingDlg.showDialog();
                        return;
                    }
                    
                    // see if a template exists for this type of document
                    TemplateConfig templateCfg = TemplateConfigManager.getInstance().getConfig(ConfigType.SESSION);
                    String rootName = currentDocument.getSourceDocument().getXMLRoot().getName();
                    ParseTemplate template = templateCfg.getDefaultTemplate(rootName);
                    boolean newTemplate = false;
                    BaseTemplateDialog dialog = null;

                    // Generate the appropriate dialog to show; edit for new templates
                    // select for pre-existing
                    if (template != null) {
                        dialog = new SelectTemplateDialog(juxtaFrame);
                    } else {
                        // generate a new template and open up the template EDIT popup
                        template = templateCfg.createTemplate(rootName, rootName, currentDocument.getSourceDocument()
                            .getElementsEncountered(), true);
                        dialog = new EditTemplateDialog(juxtaFrame);
                        juxtaFrame.showNewTemplateHelp(currentDocument.getDocumentName());
                        newTemplate = true;
                    }

                    // show the dialog and import document if ok was clicked
                    dialog.setup(currentDocument, template.getGuid());
                    dialog.setModal(true);
                    dialog.setVisible(true);
                    if (dialog.wasOkClicked()) {

                        // Get the final template that was configured by the dialog
                        // and bind it to the document
                        String guid = dialog.getTemplateGuid();
                        template = TemplateConfigManager.getInstance().getTemplate(ConfigType.SESSION, guid);

                        // parse and add it to the session
                        factory.reparseDocument(currentDocument, template);
                        if (fragment != null) {
                            currentDocument.setActiveRange(fragment);
                        }
                        juxtaSession.addExistingDocument(currentDocument, this);
                        processingDlg = new ProcessingInProgressDialog(juxtaFrame, "Adding document...");
                        processingDlg.showDialog();
                    } else {
                        // add was canceled. If there was a new template, remove it too
                        if (newTemplate) {
                            templateCfg.remove(template);
                        }
                    }
                }
            } catch (ReportedException exception) {
                ErrorHandler.handleException(exception);
            }
        }

        public void loadingComplete() {
            processingDlg.setVisible(false); 
        }
    }

    private class FragmentSelectionTracker extends MouseAdapter implements MouseMotionListener {
        private HighlightManager diffHighlighter;
        private boolean dragging;
        private Point dragStart, dragEnd;
        private JTextPane textArea;

        public FragmentSelectionTracker(JTextPane textArea) {
            this.textArea = textArea;
            dragStart = null;
            dragEnd = null;
            diffHighlighter = new HighlightManager(textArea);
        }

        public void mouseClicked(MouseEvent e) {
            if (!dragging) {
                diffHighlighter.clearRangeSelection();
                dragStart = null;
                dragEnd = null;
            }
            dragging = false;
        }

        public void mousePressed(MouseEvent e) {
            dragStart = e.getPoint();
            dragging = false;
        }

        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                dragEnd = e.getPoint();
                int dragStartOffset = textArea.viewToModel(dragStart);
                int dragEndOffset = textArea.viewToModel(dragEnd);
                diffHighlighter.setRangeSelection(dragStartOffset, dragEndOffset);
                dragging = false;
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (dragStart != null) {
                dragging = true;
                int dragStartOffset = textArea.viewToModel(dragStart);
                int dragEndOffset = textArea.viewToModel(e.getPoint());
                diffHighlighter.setRangeSelection(dragStartOffset, dragEndOffset);
            }
        }

        public void mouseMoved(MouseEvent e) {
            dragging = false;
        }

        public boolean isFragmentSelected() {
            return (dragStart != null && dragEnd != null);
        }

        public int getFragmentStart() {
            return textArea.viewToModel(dragStart);
        }

        public int getFragmentLength() {
            int dragEndOffset = textArea.viewToModel(dragEnd);
            return dragEndOffset - getFragmentStart();
        }

    }
}
