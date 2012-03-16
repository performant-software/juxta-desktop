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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;

import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentListener;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionListener;
import edu.virginia.speclab.juxta.author.model.ProgressListener;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.util.SimpleLogger;

public class ComparisonExplorer extends JPanel implements JuxtaUserInterfaceStyle, JuxtaSessionListener,
    ProgressListener, Scrollable {
    private JuxtaSession session;
    private JuxtaAuthorFrame frame;

    private LinkedList rowList;
    private OmitDocumentList omitList;

    private SelectionTracker selectionTracker;
    private SpringLayout layout;

    private FloatingEditBox nameEditBox;

    private DirectionalComparator listSortComparator;

    public static final int ROW_SPACING = 5;
    public static final Color SELECTED_COLOR = FIRST_COLOR_BRIGHTEST;
    public static final Color SELECTED_OUTLINE_COLOR = FIRST_COLOR_DARKER;
    public static final Color NORMAL_COLOR = Color.WHITE;

    private static final int SCROLL_BLOCK_INCREMENT = 100;
    private static final int SCROLL_UNIT_INCREMENT = 10;
    private static final int ROW_SIZE = 30;

    public static final String SORT_BY_DATE = "SORT_BY_DATE";
    public static final String SORT_BY_NAME = "SORT_BY_NAME";
    public static final String SORT_DIRECTION_ASCENDING = "SORT_DIRECTION_ASCENDING";
    public static final String SORT_DIRECTION_DESCENDING = "SORT_DIRECTION_DESCENDING";

    private final DirectionalComparator NAME_COMPARATOR = new NameComparator();
    private final DirectionalComparator DATE_COMPARATOR = new DateComparator();

    public ComparisonExplorer(JuxtaAuthorFrame frame) {
        this.frame = frame;

        // create a new layout 
        SpringLayout layout = new SpringLayout();
        setLayout(layout);

        rowList = new LinkedList();
        omitList = new OmitDocumentList();
        setBackground(Color.WHITE);

        nameEditBox = new FloatingEditBox();

        selectionTracker = new SelectionTracker();
        addMouseListener(selectionTracker);
        addMouseMotionListener(selectionTracker);
        setToolTipText("");

        listSortComparator = DATE_COMPARATOR;
        listSortComparator.setAscending(false);
    }

    public void setSort(String sortType, String direction) {
        if (sortType.equals(SORT_BY_DATE))
            listSortComparator = DATE_COMPARATOR;
        else if (sortType.equals(SORT_BY_NAME))
            listSortComparator = NAME_COMPARATOR;

        if (direction.equals(SORT_DIRECTION_ASCENDING))
            listSortComparator.setAscending(true);
        else if (direction.equals(SORT_DIRECTION_DESCENDING))
            listSortComparator.setAscending(false);

        maybeReorderTable();
    }

    public void maybeReorderTable() {
        generateTable();
        revalidate();
    }

    public String getToolTipText(MouseEvent e) {
        Component c = getComponentAt(e.getPoint());

        if (c instanceof DocumentRow) {
            DocumentRow row = (DocumentRow) c;
            return row.getToolTipText();
        }

        return null;
    }

    private void generateTable() {
        // clear out old list
        removeAll();
        repaint();

        layout = new SpringLayout();
        setLayout(layout);

        Collections.sort(rowList, listSortComparator);

        // populate new list
        DocumentRow lastRow = null;
        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();

            // the first item on the list is positioned relative to the parent panel
            if (lastRow == null) {
                layout.putConstraint(SpringLayout.NORTH, row, ROW_SPACING, SpringLayout.NORTH, this);
            }
            // the rest are positioned relative to the previous item
            else {
                layout.putConstraint(SpringLayout.NORTH, row, ROW_SPACING, SpringLayout.SOUTH, lastRow);
            }

            // resize with the panel
            layout.putConstraint(SpringLayout.EAST, row, 0, SpringLayout.EAST, this);
            layout.putConstraint(SpringLayout.WEST, row, 0, SpringLayout.WEST, this);

            add(row);
            row.addActionListener(omitList);
            lastRow = row;
        }
    }

    private void createRowList(List documentList) {
        rowList.clear();

        // populate new list
        for (Iterator i = documentList.iterator(); i.hasNext();) {
            JuxtaDocument document = (JuxtaDocument) i.next();
            addDocument(document);
        }
    }

    private void updateBaseDocument() {
        if (session == null)
            return;

        Collation collation = session.getCurrentCollation();
        if (collation == null)
            return;

        JuxtaDocument baseDocument = session.getDocumentManager().lookupDocument(collation.getBaseDocumentID());
        if (baseDocument == null)
            return;

        //base doc length = length of the fragment if it is one
        float baseDocumentLength = baseDocument.getDocumentLength();

        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            JuxtaDocument document = row.getDocument();
            row.nameChanged(document.getDocumentName());

            if (document.getID() == baseDocument.getID()) {
                row.setDifferenceLevel(0);
                row.setBaseText(true);
                row.setSelected(true);
            } else {
                float charCount = collation.getCharacterCount(document);
                float differenceLevel = (baseDocumentLength > 0) ? charCount / baseDocumentLength : 0.0f;

                row.setDifferenceLevel(differenceLevel);
                row.setBaseText(false);
                row.setSelected(false);
            }
        }
    }

    public void updateBaseDocumentFragment(JuxtaDocument baseDocument) {
        if (session == null)
            return;

        Collation collation = session.getCurrentCollation();
        if (collation == null)
            return;

        if (baseDocument == null)
            baseDocument = session.getDocumentManager().lookupDocument(collation.getBaseDocumentID());

        if (baseDocument == null)
            return;

        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            JuxtaDocument document = row.getDocument();

            if (document.getID() == baseDocument.getID()) {
                row.setDifferenceLevel(0);
                row.setBaseText(true);
                row.setSelected(true);
            } else {
                row.setDifferenceLevel(0);
                row.setBaseText(false);
                row.setSelected(false);
            }
        }
    }

    private void selectRow(DocumentRow selectedRow) {
        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            if (row == selectedRow) {
                row.setSelected(true);
            } else {
                row.setSelected(false);
            }
        }
    }

    private void rollOverRow(DocumentRow selectedRow) {
        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            if (row == selectedRow) {
                row.setRollOver(true);
            } else {
                row.setRollOver(false);
            }
        }
    }

    public JuxtaDocument getSelectedDocument() {
        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            if (row.isSelected()) {
                return row.getDocument();
            }
        }

        return null;
    }

    public void setJuxtaAuthorSession(JuxtaSession session) {
        if (this.session != null) {
            this.session.removeListener(this);
            this.session.getComparisonSet().removeProgressListener(this);
        }

        this.session = session;

        if (this.session != null) {
            createRowList(session.getDocumentManager().getDocumentList());
            generateTable();
            session.addListener(this);
            session.getComparisonSet().addProgressListener(this);
        }
    }

    private class OmitDocumentList implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            HashSet documentSet = new HashSet();

            for (Iterator i = rowList.iterator(); i.hasNext();) {
                DocumentRow row = (DocumentRow) i.next();

                if (row.isCheckSelected() == false) {
                    documentSet.add(row.getDocument());
                }
            }

            session.setCurrentCollationFilter(documentSet);
        }
    }

    interface DirectionalComparator extends Comparator {
        public void setAscending(Boolean value);
    }

    private class NameComparator implements DirectionalComparator {
        private Boolean ascending;

        public void setAscending(Boolean value) {
            ascending = value;
        }

        public int compare(Object o1, Object o2) {
            BiblioData row1 = ((DocumentRow) (ascending ? o1 : o2)).document.getBiblioData();
            BiblioData row2 = ((DocumentRow) (ascending ? o2 : o1)).document.getBiblioData();

            return (row1.getShortTitle().compareTo(row2.getShortTitle()));
        }
    }

    private class DateComparator implements DirectionalComparator {
        private Boolean ascending;
        private NameComparator nameComparator;

        public DateComparator() {
            nameComparator = new NameComparator();
        }

        public void setAscending(Boolean value) {
            ascending = value;
            nameComparator.setAscending(value);
        }

        public int compare(Object o1, Object o2) {
            // Sort such that objects without established dates are 
            // last in the sort regardless of sort ordering.
            Date date1 = ((DocumentRow) (o1)).document.getBiblioData().getSortDate();
            Date date2 = ((DocumentRow) (o2)).document.getBiblioData().getSortDate();

            int result;
            if (date1 == null) {
                if (date2 == null) {
                    result = 0;
                } else
                    result = 1;
            } else if (date2 == null)
                result = -1;
            else {
                result = date1.compareTo(date2);
                // reverse the order if requested
                if (!ascending)
                    result *= -1;
            }

            // secondary sort by name
            if (result == 0) {
                result = nameComparator.compare(o1, o2);
            }
            return result;
        }
    }

    private class DocumentRow extends JPanel implements JuxtaDocumentListener {

        private boolean selected, base, collated, editing;
        private float level;
        private JuxtaDocument document;
        private JCheckBox check;
        private JLabel label;
        private JLabel boxList[];
        private Box diffBoxes;

        private String toolTipText;

        private JPanel diffBoxesBackground;

        public DocumentRow(JuxtaDocument document) {
            this.document = document;
            document.addJuxtaDocumentListener(this);

            setPreferredSize(new Dimension(280, 23));
            setCursor(HOTSPOT_CURSOR);
            setBorder(new LineBorder(NORMAL_COLOR, 1));

            SpringLayout layout = new SpringLayout();
            setLayout(layout);

            Box leftBox = Box.createHorizontalBox();
            diffBoxes = Box.createHorizontalBox();

            diffBoxesBackground = new JPanel();
            diffBoxesBackground.setBackground(new Color(255, 255, 255));
            diffBoxesBackground.setOpaque(true);

            check = new JCheckBox();
            check.setSelected(true);
            check.setOpaque(false);
            leftBox.add(check);

            label = new JLabel(document.getDocumentName());
            nameChanged(document.getDocumentName());
            label.setOpaque(false);
            label.setFont(NORMAL_FONT);
            leftBox.add(label);

            boxList = new JLabel[BOXES.length];

            for (int i = 0; i < BOXES.length; i++) {
                JLabel box = new JLabel(BLANK_BOX);
                diffBoxes.add(box);
                boxList[i] = box;
            }

            layout.putConstraint(SpringLayout.WEST, leftBox, 5, SpringLayout.WEST, this);
            layout.putConstraint(SpringLayout.EAST, diffBoxes, -5, SpringLayout.EAST, this);
            layout.putConstraint(SpringLayout.SOUTH, diffBoxes, -5, SpringLayout.SOUTH, this);
            layout.putConstraint(SpringLayout.NORTH, diffBoxesBackground, -6, SpringLayout.NORTH, diffBoxes);
            layout.putConstraint(SpringLayout.SOUTH, diffBoxesBackground, 0, SpringLayout.SOUTH, diffBoxes);
            layout.putConstraint(SpringLayout.EAST, diffBoxesBackground, 0, SpringLayout.EAST, diffBoxes);
            layout.putConstraint(SpringLayout.WEST, diffBoxesBackground, 0, SpringLayout.WEST, diffBoxes);

            add(diffBoxes);
            add(diffBoxesBackground);
            add(leftBox);

            if (session.getComparisonSet().isLoadComplete()) {
                check.setSelected(true);
                check.setEnabled(true);
                diffBoxes.setVisible(true);
                diffBoxesBackground.setVisible(true);
                setPercentComplete(1.0f);
                markAsCollated();
            } else {
                check.setSelected(false);
                check.setEnabled(false);
                diffBoxes.setVisible(false);
                diffBoxesBackground.setVisible(false);
            }

            setBaseText(false);
            setRollOver(false);
            setSelected(false);
        }

        public void setDifferenceLevel(float level) {
            if (level < 0.0f) {
                SimpleLogger.logError("Invalid difference level: " + level);
                return;
            }

            // note this value is not filtered below
            this.level = level;

            if (collated) {
                // if then amount of changed text is greated than the total amount of text,
                // set to max.
                if (level > 1.0f)
                    level = 1.0f;

                SimpleLogger.logInfo("setting difference level: " + level);

                for (int i = 0; i < BOXES.length; i++) {
                    float currentLevel = (float) i / (float) BOXES.length;

                    JLabel box = boxList[i];
                    if (currentLevel < level) {
                        box.setIcon(BOXES[i]);
                    } else {
                        box.setIcon(BLANK_BOX);
                    }
                }

                updateToolTipText();
                if (!base) {
                    diffBoxes.setVisible(true);
                    diffBoxesBackground.setVisible(true);
                }
            }
        }

        public void markAsCollated() {
            //System.out.println(">markAsCollated()   " + document.getDocumentName());
            setPercentComplete(1.0f);
            check.setSelected(!session.isFiltered(document));
            check.setEnabled(true);
            collated = true;
        }

        public void markAsUncollated() {
            //System.out.println("<markAsUncollated() " + document.getDocumentName() );
            collated = false;
            setPercentComplete(0f);
            check.setSelected(false);
            check.setEnabled(false);
        }

        public void setPercentComplete(float completeness) {
            if (completeness < 0.0f || completeness > 1.0f) {
                SimpleLogger.logError("Invalid loading %: " + completeness);
                return;
            }

            if (!collated) {
                JLabel prevBox = null;

                for (int i = 0; i < BOXES.length; i++) {
                    float currentPercent = (float) i / (float) BOXES.length;

                    JLabel box = boxList[i];
                    if (currentPercent < completeness) {
                        box.setIcon(BLANK_BOX);
                    } else {
                        if (prevBox != null && prevBox.getIcon() == BLANK_BOX) {
                            prevBox.setIcon(COLLATING_BOX);
                        }

                        box.setIcon(EMPTY_BOX);
                    }

                    prevBox = box;
                }

                updateToolTipText();
                diffBoxes.setVisible(true);
                diffBoxesBackground.setVisible(true);
            }
        }

        private void updateToolTipText() {
            if (collated == false) {
                toolTipText = "Collating...";
            } else {
                if (base) {
                    toolTipText = "base text";
                } else {
                    // update tooltip to show % change
                    float displayLevel = (Math.round(level * 100f)) / 100f;
                    toolTipText = displayLevel + " change index from base text";
                }
            }
            toolTipText = "<html>" + this.label.getText() + "<br>" + toolTipText + "</html>";
        }

        public void addActionListener(ActionListener listener) {
            check.addActionListener(listener);
        }

        public void setBaseText(boolean isBase) {
            if (!collated)
                return;

            this.base = isBase;

            if (isBase) {
                updateToolTipText();
                check.setSelected(true);
                check.setEnabled(false);
                diffBoxes.setVisible(false);
                diffBoxesBackground.setVisible(false);
            } else {
                updateToolTipText();
                check.setEnabled(true);
                diffBoxes.setVisible(true);
                diffBoxesBackground.setVisible(true);
            }
        }

        public void setRollOver(boolean rollOver) {
            // don't repaint while editing document name
            if (editing)
                return;

            if (rollOver) {
                setBorder(new LineBorder(SELECTED_OUTLINE_COLOR, 1));
            } else if (!selected) {
                setBorder(new LineBorder(NORMAL_COLOR, 1));
            }
        }

        public void setEditing(boolean state) {
            editing = state;
            label.setVisible(!state);
        }

        public JuxtaDocument getDocument() {
            return document;
        }

        public boolean isCheckSelected() {
            return check.isSelected();
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;

            if (selected) {
                setRollOver(false);
                setBorder(new LineBorder(SELECTED_OUTLINE_COLOR, 1));
                setBackground(SELECTED_COLOR);
            } else {
                setBorder(new LineBorder(NORMAL_COLOR, 1));
                setBackground(NORMAL_COLOR);
            }

        }

        public String getToolTipText() {
            return toolTipText;
        }

        public JLabel getLabel() {
            return label;
        }

        public void nameChanged(String name) {
            if (label != null) {
                label.setForeground(Color.BLACK);
                label.setText(name);
            }
        }

    }

    private class FloatingEditBox implements FocusListener {
        private JTextField nameEditField;

        public void beginEditing() {
            if (layout == null)
                return;

            JuxtaDocument document = getSelectedDocument();
            if (document == null)
                return;

            DocumentRow row = getRowForDocument(document);
            if (row == null)
                return;

            if (nameEditField != null) {
                stopEditing();
                return;
            }

            JLabel label = row.getLabel();

            nameEditField = new JTextField();
            nameEditField.setBackground(JuxtaUserInterfaceStyle.FIRST_COLOR_BRIGHTER);
            nameEditField.setFont(JuxtaUserInterfaceStyle.NORMAL_FONT);
            nameEditField.setBorder(null);
            nameEditField.addFocusListener(this);
            nameEditField.setText(label.getText());

            Rectangle labelBounds = label.getBounds();
            int northConstraint = labelBounds.y + 2;
            int eastConstraint = row.getWidth() - (labelBounds.x + labelBounds.width);
            int westConstraint = labelBounds.x + 5;

            layout.putConstraint(SpringLayout.NORTH, nameEditField, northConstraint, SpringLayout.NORTH, row);
            layout.putConstraint(SpringLayout.EAST, nameEditField, eastConstraint, SpringLayout.EAST, row);
            layout.putConstraint(SpringLayout.WEST, nameEditField, westConstraint, SpringLayout.WEST, row);

            row.setEditing(true);

            add(nameEditField);

            nameEditField.setVisible(true);
            nameEditField.requestFocus();
            nameEditField.setCaretPosition(0);
        }

        public void stopEditing() {
            if (nameEditField != null) {
                nameEditField.removeFocusListener(this);
                layout.removeLayoutComponent(nameEditField);
                remove(nameEditField);

                JuxtaDocument document = (JuxtaDocument) getSelectedDocument();
                if (document == null)
                    return;

                DocumentRow row = getRowForDocument(document);
                if (row == null)
                    return;

                row.setEditing(false);

                row.getLabel().setText(nameEditField.getText());
                document.setDocumentName(nameEditField.getText());
                session.markAsModified();
                nameEditField = null;
                row.repaint();

                revalidate();
            }
        }

        public void focusGained(FocusEvent e) {

        }

        public void focusLost(FocusEvent e) {
            stopEditing();
        }
    }

    private class SelectionTracker extends MouseAdapter implements MouseMotionListener {
        private boolean enabled;

        public void mouseClicked(MouseEvent e) {
            if (!enabled)
                return;

            Component c = getComponentAt(e.getPoint());

            if (c instanceof DocumentRow) {
                DocumentRow row = (DocumentRow) c;

                if (session != null) {
                    // if we are editing a name, stop
                    nameEditBox.stopEditing();

                    JuxtaDocument document = row.getDocument();

                    try {
                        if (session.setBaseText(document)) {
                            selectRow(row);
                            frame.setViewmode(JuxtaAuthorFrame.VIEW_MODE_COLLATION);
                            frame.setBaseDocument(document);
                        }
                    } catch (ReportedException e1) {
                        ErrorHandler.handleException(e1);
                    }
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            rollOverRow(null);
        }

        public void mouseDragged(MouseEvent e) {
            // do nothing
        }

        public void mouseMoved(MouseEvent e) {
            if (!enabled)
                return;

            Component c = getComponentAt(e.getPoint());

            if (c instanceof DocumentRow) {
                DocumentRow row = (DocumentRow) c;
                rollOverRow(row);
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

    private DocumentRow getRowForDocument(JuxtaDocument document) {
        for (Iterator i = rowList.iterator(); i.hasNext();) {
            DocumentRow row = (DocumentRow) i.next();
            if (row.getDocument() == document) {
                return row;
            }
        }

        return null;
    }

    public void removeDocument(JuxtaDocument document) {
        DocumentRow row = getRowForDocument(document);

        if (row != null) {
            rowList.remove(row);
            generateTable();
            revalidate();
        }
    }

    public void sessionModified() {

        Collation collation = session.getCurrentCollation();
        if (collation == null) {
            return;
        }

        try {
            JuxtaDocument baseDocument = session.getDocumentManager().lookupDocument(collation.getBaseDocumentID());
            if (baseDocument == null) {
                LinkedList documentList = session.getDocumentManager().getDocumentList();
                if (documentList.isEmpty() == false) {
                    baseDocument = (JuxtaDocument) documentList.getFirst();
                    session.setBaseText(baseDocument);
                    float baseDocumentLength = baseDocument.getDocumentLength();
                    for (Iterator i = rowList.iterator(); i.hasNext();) {
                        DocumentRow row = (DocumentRow) i.next();
                        JuxtaDocument document = row.getDocument();
                        row.nameChanged(document.getDocumentName());
    
                        if (document.getID() == baseDocument.getID()) {
                            row.setDifferenceLevel(0);
                            row.setBaseText(true);
                            row.setSelected(true);
                        } else {
                            float charCount = collation.getCharacterCount(document);
                            float differenceLevel = (baseDocumentLength > 0) ? charCount / baseDocumentLength : 0.0f;
    
                            row.setDifferenceLevel(differenceLevel);
                            row.setBaseText(false);
                            row.setSelected(false);
                        }
                    }
                }
            }   
        } catch (ReportedException e) {
            // not much to do here... just swallow it and leave the 
            // ui with no doc selected. 
        }
    }

    public void currentCollationChanged(Collation currentCollation) {
        if (currentCollation != null) {
            updateBaseDocument();
        }
    }

    public void documentAdded(JuxtaDocument document) {
        if (document != null) {
            addDocument(document);

            generateTable();
            revalidate();
        }
    }

    private void addDocument(JuxtaDocument document) {
        DocumentRow row = new DocumentRow((JuxtaDocument) document);
        rowList.add(row);
        setPreferredSize(new Dimension(0, rowList.size() * ROW_SIZE));
    }

    public void updateProgress(JuxtaDocument document, float completeness) {
        SimpleLogger.logInfo(document.getDocumentName() + ": " + completeness);
        DocumentRow row = getRowForDocument(document);

        if (row != null) {
            row.setPercentComplete(completeness);
        }
    }

    public void collationCompleted(JuxtaDocument document) {
        SimpleLogger.logInfo(document.getDocumentName() + ": done!");
        DocumentRow row = getRowForDocument(document);

        if (row != null) {
            row.markAsCollated();
            updateBaseDocument();
        }
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        // always match the width of the viewport
        return true;
    }

    public void setSelectionEnabled(boolean enabled) {
        selectionTracker.setEnabled(enabled);
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_BLOCK_INCREMENT;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_UNIT_INCREMENT;
    }

    public void collationStarted(JuxtaDocument document) {
        SimpleLogger.logInfo(document.getDocumentName() + ": started collation");
        DocumentRow row = getRowForDocument(document);

        if (row != null) {
            row.markAsUncollated();
            updateBaseDocument();
        }
    }

    public void editDocumentName() {
        nameEditBox.beginEditing();
        session.markAsModified();
    }

    public void currentCollationFilterChanged(Collation currentCollation) {
        // do nothing

    }
}
