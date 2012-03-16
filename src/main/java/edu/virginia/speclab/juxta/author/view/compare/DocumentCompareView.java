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

package edu.virginia.speclab.juxta.author.view.compare;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.collation.DifferenceMap;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionListener;
import edu.virginia.speclab.juxta.author.model.MovesManager;
import edu.virginia.speclab.juxta.author.model.MovesManager.FragmentPair;
import edu.virginia.speclab.juxta.author.model.MovesManagerListener;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import edu.virginia.speclab.juxta.author.model.SearchResults.SearchResult;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.LineMarkController;
import edu.virginia.speclab.juxta.author.view.MovesPanel;
import edu.virginia.speclab.juxta.author.view.ui.DropDownTitlePanel;
import edu.virginia.speclab.ui.DualScrollingTextPanel;
import edu.virginia.speclab.ui.Renderer;
import edu.virginia.speclab.util.IntPair;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class handles the display of the dual document comparison mode. It must be bound with 
 * a <code>JuxtaSession</code> object, using <code>setSession()</code> 
 * @author Nick
 *
 */
public class DocumentCompareView extends JPanel implements Renderer, JuxtaSessionListener,
    DifferenceCompareRenderingConstants, MovesManagerListener {
    private JuxtaSession session;
    private JuxtaDocument witnessDocument;
    private JuxtaDocument baseDocument;

    public DualScrollingTextPanel dualTextPanel;

    private DropDownTitlePanel baseTextSelector;
    private DropDownTitlePanel witnessTextSelector;

    private DocumentCompareHighlighter leftHighlighter;
    private DocumentCompareHighlighter rightHighlighter;
    private List differenceConnectorList;
    private MovesManager.MoveList moveList;
    private DifferenceMap baseDifferenceMap, witnessDifferenceMap;

    private int scrollMode;
    private DualScrollingController dualScrollController;
    private IndependentDualScroller leftScroller, rightScroller;

    private JuxtaAuthorFrame juxtaAuthorFrame;

    private HashSet listeners;

    public static final int SCROLL_MODE_LINKED = 0;
    public static final int SCROLL_MODE_INDEPENDENT = 1;

    // expose these for convenience of users of getConnector()
    public static final int LEFT = DifferenceCompareRenderingConstants.LEFT;
    public static final int RIGHT = DifferenceCompareRenderingConstants.RIGHT;
    private LineMarkController markController;
    private LineMarkController markControllerRight;
    private JButton linkScrollingButton;

    private JButton markAsMovedButton;
    private boolean highLightAllSearchResults = true;
    private SearchResults searchResults;

    private static final String NO_TEXT_SELECTED = "<none>";

    public DocumentCompareView(JuxtaAuthorFrame frame) {
        this.juxtaAuthorFrame = frame;
        listeners = new HashSet();
        initUI();
    }

    private void initUI() {
        // create a dual scrolling text panel and control the scrolling with 
        // the DualScrollController
        dualScrollController = new DualScrollingController(this);
        dualTextPanel = new DualScrollingTextPanel();
        leftScroller = new IndependentDualScroller(this, DualScrollingTextPanel.LEFT);
        rightScroller = new IndependentDualScroller(this, DualScrollingTextPanel.RIGHT);
        this.dualTextPanel.addScroller(dualScrollController, RIGHT);
        this.dualTextPanel.addScroller(leftScroller, LEFT);
        this.dualTextPanel.addScroller(rightScroller, RIGHT);

        setScrollMode(SCROLL_MODE_LINKED);

        dualTextPanel.setFont(NORMAL_FONT);
        dualTextPanel.setLocationMarkStripColor(THIRD_COLOR_WHITE);

        JTextComponent leftText = dualTextPanel.getLeftTextArea();
        JTextComponent rightText = dualTextPanel.getRightTextArea();

        markController = new LineMarkController(dualTextPanel.getLineNumberStrip());
        markControllerRight = new LineMarkController(dualTextPanel.getLineNumberStripRight());

        leftHighlighter = new DocumentCompareHighlighter(leftText);
        rightHighlighter = new DocumentCompareHighlighter(rightText);

        leftText.addMouseMotionListener(new RollOverTracker(leftText, LEFT));
        rightText.addMouseMotionListener(new RollOverTracker(rightText, RIGHT));

        leftText.addMouseListener(new DifferenceSelector(leftText, LEFT));
        rightText.addMouseListener(new DifferenceSelector(rightText, RIGHT));

        dualTextPanel.setRenderer(this);
        dualTextPanel.addComponentListener(new ResizeListener());

        setLayout(new BorderLayout());

        baseTextSelector = new DropDownTitlePanel();
        baseTextSelector.setBackground(TITLE_BACKGROUND_COLOR);

        witnessTextSelector = new DropDownTitlePanel();
        witnessTextSelector.setBackground(TITLE_BACKGROUND_COLOR);;

        baseTextSelector.addActionListener(new SelectionTracker(LEFT));
        witnessTextSelector.addActionListener(new SelectionTracker(RIGHT));

        linkScrollingButton = new JButton(LOCK_WINDOWS);
        linkScrollingButton.setToolTipText("Link documents when scrolling");

        linkScrollingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (DocumentCompareView.this.scrollMode == SCROLL_MODE_LINKED) {
                    DocumentCompareView.this.setScrollMode(SCROLL_MODE_INDEPENDENT);
                    linkScrollingButton.setIcon(UNLOCK_WINDOWS);
                } else {
                    //have to put documents back to the beginning, otherwise
                    //they are linked at the wrong place
                    dualTextPanel.resetScrolling();
                    DocumentCompareView.this.setScrollMode(SCROLL_MODE_LINKED);
                    linkScrollingButton.setIcon(LOCK_WINDOWS);
                }
            }
        });

        markAsMovedButton = new JButton(MARK_AS_MOVED);
        //markAsMovedButton.setToolTipText(MARK_AS_MOVED_DISABLED);

        markAsMovedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    MovesManager.FragmentPair fp = assembleMoveFromSelection();
                    MovesManager movesManager = session.getDocumentManager().getMovesManager();
                    movesManager.createMove(fp.first, fp.second);
                    juxtaAuthorFrame.makeMovesPaneVisible();
                } catch (LoggedException ex) {
                    SimpleLogger.logError("Attempted to highlight bad location: " + ex);
                }
            }
        });

        leftText.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                setMarkAsMovedButton();
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
                JTextComponent leftText = dualTextPanel.getLeftTextArea();
                leftText.setToolTipText(null);
            }
        });

        rightText.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                setMarkAsMovedButton();
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
                JTextComponent rightText = dualTextPanel.getRightTextArea();
                rightText.setToolTipText(null);
            }
        });

        leftText.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
            }

            public void focusLost(FocusEvent e) {
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
            }
        });

        rightText.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
            }

            public void focusLost(FocusEvent e) {
                leftHighlighter.redrawSelection();
                rightHighlighter.redrawSelection();
            }
        });

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        titlePanel.add(baseTextSelector);
        titlePanel.add(linkScrollingButton);
        titlePanel.add(witnessTextSelector);
        titlePanel.add(markAsMovedButton);

        add(titlePanel, BorderLayout.NORTH);
        add(dualTextPanel, BorderLayout.CENTER);
    }

    private void setMarkAsMovedButton() {
        String MARK_AS_MOVED_DISABLED = "Select an area in both texts first, then click this to mark those areas as text that has moved.";
        String MARK_AS_MOVED_ENABLED = "Mark selected text as moved";
        String MARK_AS_MOVED_ERROR = "The selected areas cannot be marked as moved. ";

        JTextComponent leftText = dualTextPanel.getLeftTextArea();
        JTextComponent rightText = dualTextPanel.getRightTextArea();
        boolean leftHasSelection = (leftText.getSelectedText() != null) && (!leftText.getSelectedText().equals(""));
        boolean rightHasSelection = (rightText.getSelectedText() != null) && (!rightText.getSelectedText().equals(""));
        if (leftHasSelection && rightHasSelection) {
            MovesManager.FragmentPair fp = assembleMoveFromSelection();
            MovesManager movesManager = session.getDocumentManager().getMovesManager();
            String err = movesManager.canCreate(fp.first, fp.second);
            if (err.equals("")) {
                markAsMovedButton.setEnabled(true);
                markAsMovedButton.setToolTipText(MARK_AS_MOVED_ENABLED);
            } else {
                markAsMovedButton.setEnabled(false);
                markAsMovedButton.setToolTipText(MARK_AS_MOVED_ERROR + err);
            }
        } else {
            markAsMovedButton.setEnabled(false);
            markAsMovedButton.setToolTipText(MARK_AS_MOVED_DISABLED);
        }
    }

    public void setScrollMode(int mode) {
        if (mode == SCROLL_MODE_LINKED) {
            this.dualTextPanel.leftScrollBarVisible(false);
        } else {
            this.dualTextPanel.leftScrollBarVisible(true);
        }
        scrollMode = mode;
    }

    public void addListener(DocumentCompareViewListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DocumentCompareViewListener listener) {
        listeners.remove(listener);
    }

    private void fireSelectedDocumentsChanged() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            DocumentCompareViewListener listener = (DocumentCompareViewListener) i.next();
            listener.selectedDocumentsChanged(baseDocument, witnessDocument);
        }
    }

    public void setSession(JuxtaSession session) {
        if (this.session != null) {
            this.session.removeListener(this);
        }

        this.session = session;

        session.addListener(this);

        LinkedList documentList = session.getDocumentManager().getDocumentList();

        // try to get the first document on the list
        if (!documentList.isEmpty()) {
            JuxtaDocument firstDocument = (JuxtaDocument) documentList.getFirst();

            // set first document as default 
            selectViews(firstDocument, null);
        }

        updateDocumentLists();
        MovesManager movesManager = session.getDocumentManager().getMovesManager();
        movesManager.addListener(leftHighlighter);
        movesManager.addListener(rightHighlighter);
        movesManager.addListener(this);
        setSearchResults(null);
    }

    public void showLocationMarkStrip(boolean visible) {
        dualTextPanel.showLineNumberStrip(visible);
    }

    public void setLocation(int offset, int length) {
        // highlight the difference
        leftHighlighter.highlightRange(offset, offset + length, true);
        rightHighlighter.highlightRange(0, 0, false);
        
        //get the position of the character in correct document
        int characterPosition = dualTextPanel.getCharacterPosition(offset, true);

        if (this.scrollMode == SCROLL_MODE_LINKED) {
            //calculate how far that is down - from 0 to 1
            float linePosition = (float) characterPosition / dualTextPanel.getTextSize(true);
            //use the preceeding value to scale to the size of the array
            int trueLineNumber = Math.round(linePosition * dualScrollController.getOffsetSize(true));
            //scroll to the correct location
            dualScrollController.scrollToWithOffset(trueLineNumber, true);
        } else {
            setScrollPosition(characterPosition, LEFT);
        }
    }

    public void setLocation(Difference difference, JuxtaDocument baseDocument, JuxtaDocument witnessDocument) {
        setScrollMode(SCROLL_MODE_LINKED);
        linkScrollingButton.setIcon(LOCK_WINDOWS);

        if ((this.baseDocument == null || this.witnessDocument == null)
            || (baseDocument == null || witnessDocument == null)
            || (this.baseDocument.getID() != baseDocument.getID() || this.witnessDocument.getID() != witnessDocument
                .getID())) {
            selectViews(baseDocument, witnessDocument);

            if (baseDocument != null)
                baseTextSelector.setSelection(baseDocument);
            else
                baseTextSelector.setSelection(NO_TEXT_SELECTED);

            if (witnessDocument != null)
                witnessTextSelector.setSelection(witnessDocument);
            else
                witnessTextSelector.setSelection(NO_TEXT_SELECTED);
        }

        // have to wait a little while to scroll for this to work
        if (difference != null && baseDocument != null && witnessDocument != null)
            SwingUtilities.invokeLater(new DelayedScroller(difference, RIGHT));
    }

    public void scrollToPosition(float position, int side) {
        dualTextPanel.scrollToPosition(position, side);
    }

    private class DelayedScroller implements Runnable {
        private Difference difference;
        private int side;

        public DelayedScroller(Difference difference, int side) {
            this.side = side;
            this.difference = difference;
        }

        public void run() {
            // center the scroll target
            int halfWindowHeight = (dualTextPanel.getHeight() / 2) / dualTextPanel.getLineHeight();

            int position = dualScrollController.getScrollPosition(difference);
            int targetOffset = position - halfWindowHeight;
            if (targetOffset < 0)
                targetOffset = 0;
            dualTextPanel.scrollToLine(targetOffset, side);
            leftHighlighter.highlightRange(difference.getOffset(Difference.BASE), difference.getOffset(Difference.BASE)
                + difference.getLength(Difference.BASE), true);
            rightHighlighter.highlightRange(difference.getOffset(Difference.WITNESS),
                difference.getOffset(Difference.WITNESS) + difference.getLength(Difference.WITNESS), true);
        }
    }

    public void finishSelectingViews() {
        updateConnectors();
        movesChanged(session.getDocumentManager().getMovesManager());

        markController.updateLineMarkers(baseDocument, dualTextPanel.getLeftTextArea());
        markControllerRight.updateLineMarkers(witnessDocument, dualTextPanel.getRightTextArea());
        setHighlightAllSearchResults(highLightAllSearchResults); // reselect the results on the text panes

        // schedule a repaint so the differences will render
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                repaint();
            }
        });
    }

    // places the specified documents into the view
    private void selectViews(JuxtaDocument baseDocument, JuxtaDocument witnessDocument) {
        this.baseDocument = baseDocument;
        this.witnessDocument = witnessDocument;

        // check for null
        if (baseDocument == null)
            dualTextPanel.setLeftText(null);
        else
            dualTextPanel.setLeftText(baseDocument.getDocumentText());

        // check for null
        if (witnessDocument == null)
            dualTextPanel.setRightText(null);
        else
            dualTextPanel.setRightText(witnessDocument.getDocumentText());

        fireSelectedDocumentsChanged();

        finishSelectingViews();
    }

    private void updateConnectors() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Collation baseCollation = null, witnessCollation = null;

                if (session == null || baseDocument == null || witnessDocument == null) {
                    differenceConnectorList = new LinkedList();
                    dualScrollController.updateScrollData();
                    leftHighlighter.updateModel(null, 0, 0, 0, null);
                    rightHighlighter.updateModel(null, 0, 0, 0, null);
                    return;
                }

                try {
                    // obtain the collation for the base document
                    baseCollation = session.getComparisonSet().getCollation(baseDocument);
                    witnessCollation = session.getComparisonSet().getCollation(witnessDocument);
                } catch (ReportedException e) {
                    ErrorHandler.handleException(e);
                }

                if (baseCollation == null || witnessCollation == null) {
                    SimpleLogger.logError("Unable to load collation data.");
                    baseDifferenceMap = null;
                    witnessDifferenceMap = null;
                    return;
                }

                // update the intermediate model data used for 
                // creating the connector list and rollover selection
                List diffList = baseCollation.getDifferences(witnessDocument);
                baseDifferenceMap = new DifferenceMap(diffList, baseDocument.getDocumentLength(), Difference.BASE);
                witnessDifferenceMap = new DifferenceMap(diffList, witnessDocument.getDocumentLength(),
                    Difference.WITNESS);

                // update the document highlighting 
                leftHighlighter.updateModel(baseDifferenceMap, Difference.BASE, baseDocument.getID(),
                    witnessDocument.getID(), session.getDocumentManager().getMovesManager());
                rightHighlighter.updateModel(witnessDifferenceMap, Difference.WITNESS, baseDocument.getID(),
                    witnessDocument.getID(), session.getDocumentManager().getMovesManager());

                // calculate shape of difference connectors
                differenceConnectorList = createConnectorList();

                // create the arrays to drive scroll data
                dualScrollController.updateScrollData();

            }
        });
    }

    // translates a text offset into the text area into a y-offset in the window
    private static int obtainScreenPosition(JTextComponent rightTextArea, int offset) {
        int position = -1;
        try {
            position = rightTextArea.modelToView(offset).y;
        } catch (BadLocationException e) {
            SimpleLogger.logError("Bad witness offset while create difference connectors: " + offset);
        }

        return position;
    }

    /**
     * Obtain the connector at the specified line number on the specified panel if there is one.
     * @param lineNumber Access the connector at this line number, zero based.
     * @param which either <code>DocumentCompareView.LEFT</code> or <code>DocumentCompareView.RIGHT</code>
     * @return A <code>DifferenceConnector</code> or <code>null</code> if there isn't one present. 
     */
    public DifferenceConnector getConnector(int lineNumber, int which) {
        for (Iterator j = differenceConnectorList.iterator(); j.hasNext();) {
            DifferenceConnector connector = (DifferenceConnector) j.next();

            int top = 0, bottom = 0;

            // get the connector bounds for the specified side of the connector
            if (which == LEFT) {
                top = connector.getLeftPosition();
                bottom = connector.getLeftPosition() + connector.getLeftLength();
            } else {
                top = connector.getRightPosition();
                bottom = connector.getRightPosition() + connector.getRightLength();
            }

            // convert line number to screen pixels
            int currentPosition = lineNumber * dualTextPanel.getLineHeight();

            // if the position is within this connector, return it
            if (currentPosition >= top && currentPosition <= bottom) {
                return connector;
            }
        }

        return null;
    }

    public void movesChanged(MovesManager movesManager) {
        if ((movesManager == null) || (baseDocument == null) || (witnessDocument == null)) {
            SimpleLogger.logInfo("DocumentCompareView didn't update its move blocks because a pointer was null");
            return;
        }
        moveList = movesManager.getAllMoves(baseDocument.getID(), witnessDocument.getID());
        dualTextPanel.repaint();

        updateConnectors();
        markController.updateLineMarkers(baseDocument, dualTextPanel.getLeftTextArea());
        markControllerRight.updateLineMarkers(witnessDocument, dualTextPanel.getRightTextArea());
    }

    private Rectangle calcMovePosition(Rectangle top, Rectangle bottom, int scrollPosition, boolean isLeftSide) {
        Rectangle rect = new Rectangle();
        int margin = 0;
        rect.width = 1;
        rect.x = (isLeftSide ? margin : dualTextPanel.getCenterWidth() - rect.width - margin);
        rect.y = top.y - scrollPosition;
        rect.height = bottom.height + bottom.y - top.y;
        return rect;
    }

    private void renderMoves(Graphics2D g2) {
        if (moveList == null)
            return;

        JTextComponent leftTextArea = dualTextPanel.getLeftTextArea();
        JTextComponent rightTextArea = dualTextPanel.getRightTextArea();

        for (int i = 0; i < moveList.size(); ++i) {
            FragmentPair fp = moveList.get(i);
            try {
                Rectangle r0 = leftTextArea.modelToView(fp.first.getStartOffset(OffsetRange.Space.ACTIVE));
                Rectangle r1 = leftTextArea.modelToView(fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
                Rectangle rMove = calcMovePosition(r0, r1, 0/*dualTextPanel.getLeftPosition()*/, true);
                //				g2.drawRect(rMove.x, rMove.y, rMove.width, rMove.height);

                Rectangle r2 = rightTextArea.modelToView(fp.second.getStartOffset(OffsetRange.Space.ACTIVE));
                Rectangle r3 = rightTextArea.modelToView(fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
                Rectangle rMove2 = calcMovePosition(r2, r3, 0/*dualTextPanel.getRightPosition()*/, false);
                //				g2.drawRect(rMove2.x, rMove2.y, rMove2.width, rMove2.height);
                //				Rectangle rConnector = calcMoveConnector(rMove, rMove2);
                //				g2.drawLine(rConnector.x, rConnector.y, rConnector.width, rConnector.height);
                // int baseTextOffset, int leftPosition,  int leftLength, int rightPosition, int rightLength
                DifferenceConnector connector = new DifferenceConnector(
                    fp.first.getStartOffset(OffsetRange.Space.ACTIVE), rMove.y, rMove.height, rMove2.y, rMove2.height,
                    DifferenceConnector.MOVE_STYLE);
                connector.render(g2, dualTextPanel.getCenterWidth(), dualTextPanel.getHeight(),
                    dualTextPanel.getLeftPosition(), dualTextPanel.getRightPosition());

            } catch (BadLocationException e) {
                SimpleLogger.logError("Bad move while rendering the center pane.");
            }
        }
    }

    private List createConnectorList() {
        // We're going to fill this list with DifferenceConnector objects.
        LinkedList diffConnectorList = new LinkedList();

        //
        // First, marshal all of the data we will need to create the connector list.
        //

        // need these
        JTextComponent leftTextArea = dualTextPanel.getLeftTextArea();
        JTextComponent rightTextArea = dualTextPanel.getRightTextArea();

        // find the height of a line at the current font size
        // it doesn't matter which one we use, because we are going to
        // use this to look up connectors which all touch both docs. 
        int lineCount = dualTextPanel.getLineCount(LEFT);

        // the set of differences that already have connectors
        HashSet renderedDifferences = new HashSet();

        // no differences found for this witness document, return empty list
        if (baseDifferenceMap == null)
            return diffConnectorList;

        // The basic strategy here is to walk down the base document, line-by-line, 
        // and for each line create a list of the differences encountered on that line.
        // For each difference, we prepare one and only one DifferenceConnector 
        // object, which will later be renderer in renderToSurface().
        //
        // Variables with the suffix "Position" refer to screen coordinates. 
        // Variables with the suffix "Offset" refer to document offsets.
        //
        for (int line = 0; line < lineCount; line++) {
            // screen Y position of this line
            int linePosition = line * dualTextPanel.getLineHeight();

            // calculate offsets for this line
            int lineStartOffset = leftTextArea.viewToModel(new Point(0, linePosition));
            int lineEndOffset = leftTextArea.viewToModel(new Point(leftTextArea.getWidth(), linePosition));

            // the set of differences found on the current line 
            HashSet differencesOnLine = baseDifferenceMap.getDifferences(lineStartOffset, lineEndOffset);

            // skip this line, no differences found
            if (differencesOnLine == null)
                continue;

            // go through each difference on this line and draw a connector to the witness
            for (Iterator i = differencesOnLine.iterator(); i.hasNext();) {
                Difference difference = (Difference) i.next();

                // skip this difference if we already have a connector for it
                if (renderedDifferences.contains(difference))
                    continue;
                else
                    renderedDifferences.add(difference);

                // figure out the screen positions for the top of the difference areas
                int leftPosition = linePosition;
                int rightPosition = obtainScreenPosition(rightTextArea, difference.getOffset(Difference.WITNESS));

                // if we obtained an invalid screen position, skip this difference
                if (rightPosition == -1)
                    continue;

                // zero length renders a point instead of a bracket
                int leftLength = 0;
                int rightLength = 0;

                // figure out the length of the bracket areas

                if (difference.getType() != Difference.INSERT) {
                    int baseEndOffset = difference.getOffset(Difference.BASE) + difference.getLength(Difference.BASE);
                    int leftEndPosition = obtainScreenPosition(leftTextArea, baseEndOffset);
                    if (leftEndPosition == -1)
                        continue;
                    leftLength = (leftEndPosition - leftPosition) + dualTextPanel.getLineHeight();
                } else
                    // this is a dot, so bump it to the middle of the line
                    leftPosition += dualTextPanel.getLineHeight() / 2;

                if (difference.getType() != Difference.DELETE) {
                    int witnessEndOffset = difference.getOffset(Difference.WITNESS)
                        + difference.getLength(Difference.WITNESS);
                    int rightEndPosition = obtainScreenPosition(rightTextArea, witnessEndOffset);
                    if (rightEndPosition == -1)
                        continue;
                    rightLength = (rightEndPosition - rightPosition) + dualTextPanel.getLineHeight();
                } else
                    // this is a dot, so bump it to the middle of the line
                    rightPosition += dualTextPanel.getLineHeight() / 2;

                // create a connector for this difference
                DifferenceConnector connector = new DifferenceConnector(difference.getOffset(Difference.BASE),
                    leftPosition, leftLength, rightPosition, rightLength, DifferenceConnector.DIFFERENCE_STYLE);
                diffConnectorList.add(connector);
            }

        }

        // return the resulting list
        return diffConnectorList;
    }

    public void renderToSurface(Graphics g) {
        // set up graphics
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (differenceConnectorList != null) {
            // iterate through the connectors and render them
            for (Iterator i = differenceConnectorList.iterator(); i.hasNext();) {
                DifferenceConnector connector = (DifferenceConnector) i.next();
                connector.render(g2, dualTextPanel.getCenterWidth(), dualTextPanel.getHeight(),
                    dualTextPanel.getLeftPosition(), dualTextPanel.getRightPosition());
            }
        }
        renderMoves(g2);
    }

    // since word wrapping can reposition differences in the text, we need to recalculate
    // the connector list when the window resizes.
    private class ResizeListener extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            dualTextPanel.resize();
            finishSelectingViews();
        }
    }

    private class RollOverTracker implements MouseMotionListener {
        private JTextComponent textArea;
        private int which;

        public RollOverTracker(JTextComponent leftText, int which) {
            this.which = which;
            this.textArea = leftText;
        }

        public void mouseDragged(MouseEvent e) {
            // do nothing
        }

        public void mouseMoved(MouseEvent e) {
            Difference difference = getDifference(e.getPoint());

            if (difference != null) {
                if (baseDocument == null || witnessDocument == null)
                    return;

                if (which == LEFT) {
                    leftHighlighter.highlightDifference(difference);
                    rightHighlighter.highlightDifference(difference);
                } else {
                    leftHighlighter.highlightDifference(difference);
                    rightHighlighter.highlightDifference(difference);
                }

                textArea.setToolTipText(differenceSummary(difference));
            } else {
                leftHighlighter.clearHighlight();
                rightHighlighter.clearHighlight();
                textArea.setToolTipText(null);
            }
        }

        private Difference getDifference(Point p) {
            int offset = textArea.viewToModel(p);

            HashSet differences = null;
            if (which == LEFT && baseDifferenceMap != null)
                differences = baseDifferenceMap.getDifferences(offset, offset);
            else if (which == RIGHT && witnessDifferenceMap != null)
                differences = witnessDifferenceMap.getDifferences(offset, offset);

            if (differences != null)
                return (Difference) differences.iterator().next();

            return null;
        }

        private String differenceSummary(Difference difference) {
            boolean debug = false;
            if (debug)
                return difference.dump();

            switch (difference.getType()) {
                case Difference.CHANGE:
                    return "Changed";

                case Difference.DELETE:
                    return "Deleted";

                case Difference.INSERT:
                    return "Inserted";

                case Difference.MOVE:
                    return "Moved";
            }

            return null;
        }
    }

    private class SelectionTracker implements ActionListener {
        private int which;

        public SelectionTracker(int which) {
            this.which = which;
        }

        public void actionPerformed(ActionEvent e) {
            if (which == LEFT) {
                Object selected = baseTextSelector.getSelected();
                leftHighlighter.clearHighlightRange();
                
                if (selected instanceof JuxtaDocument) {
                    JuxtaDocument doc = (JuxtaDocument) selected;
                    selectViews(doc, witnessDocument);
                    try {
                        session.setBaseText(doc);
                    } catch (ReportedException e1) {
                        SimpleLogger.logError("Unable to set base document from compare view: " + e.toString());
                    }
                } else {
                    selectViews(null, witnessDocument);
                }

            } else {
                Object selected = witnessTextSelector.getSelected();
               
                rightHighlighter.clearHighlightRange();

                if (selected instanceof JuxtaDocument)
                    selectViews(baseDocument, (JuxtaDocument) selected);
                else
                    selectViews(baseDocument, null);
            }
        }
    }

    private void updateDocumentLists() {
        if (session == null) {
            selectViews(null, null);
            return;
        }

        // populate the drop down lists
        LinkedList documentList = (LinkedList) session.getDocumentManager().getDocumentList().clone();
        documentList.addFirst(NO_TEXT_SELECTED);

        // get the current base & witness
        Object o1 = baseTextSelector.getSelected();
        Object o2 = witnessTextSelector.getSelected();
        JuxtaDocument base = o1 instanceof JuxtaDocument ? (JuxtaDocument) o1 : null;
        JuxtaDocument witness = o2 instanceof JuxtaDocument ? (JuxtaDocument) o2 : null;

        // make sure they are in the new document list
        if (base != null) {
            base = session.getDocumentManager().lookupDocument(base.getID());
        }

        if (witness != null) {
            witness = session.getDocumentManager().lookupDocument(witness.getID());
        }

        // if no base found, select the first document from the list
        if (base == null && documentList.size() > 1) {
            base = (JuxtaDocument) documentList.get(1);
        }

        // update the views
        selectViews(base, witness);

        baseTextSelector.setList(documentList);
        witnessTextSelector.setList(documentList);

        if (base != null) {
            baseTextSelector.setSelection(base);
        }

        if (witness != null) {
            witnessTextSelector.setSelection(witness);
        }
    }

    private class DifferenceSelector extends MouseAdapter {
        private JTextComponent textArea;
        private int which;

        public DifferenceSelector(JTextComponent leftText, int which) {
            this.which = which;
            this.textArea = leftText;
        }

        public void mouseClicked(MouseEvent e) {
            if (witnessDocument != null) {
                MovesManager movesManager = session.getDocumentManager().getMovesManager();
                int id1 = (which == LEFT) ? baseDocument.getID() : witnessDocument.getID();
                int id2 = (which == LEFT) ? witnessDocument.getID() : baseDocument.getID();
                MovesManager.FragmentPair fp = movesManager.findMove(id1, id2, textArea.viewToModel(e.getPoint()));
                if (fp != null) {
                    juxtaAuthorFrame.makeMovesPaneVisible();
                    MovesPanel movesPanel = juxtaAuthorFrame.getMovesPanel();
                    movesPanel.select(fp);
                }
            }

            int offset = this.textArea.viewToModel( e.getPoint() );
            if ( this.which == LEFT) {
                juxtaAuthorFrame.makeSourcePaneVisible( baseDocument, offset );
            } else  {
                juxtaAuthorFrame.makeSourcePaneVisible( witnessDocument, offset );
            }
        }
    }

    public DualScrollingTextPanel getDualTextPanel() {
        return dualTextPanel;
    }

    public void sessionModified() {
        updateDocumentLists();
    }

    public void currentCollationChanged(Collation currentCollation) {
        // do nothing
    }

    public void documentAdded(JuxtaDocument document) {
        updateDocumentLists();
    }

    public void currentCollationFilterChanged(Collation currentCollation) {
        // do nothing

    }

    public JuxtaDocument getBaseDocument() {
        return baseDocument;
    }

    public JuxtaDocument getWitnessDocument() {
        return witnessDocument;
    }

    public void addScrollBarListener(ChangeListener listener) {
        dualTextPanel.addScrollBarListener(listener);
    }

    public float getScrollPosition() {
        return dualTextPanel.getScrollPosition();
    }

    public void searchHighlightRemoval() {
        leftHighlighter.clearHighlightRange();
        rightHighlighter.clearHighlightRange();
    }

    public void setScrollPosition(int position, int side) {
        dualTextPanel.updateScrollBarPosition(position, side);
    }

    public int getScrollMode() {
        return scrollMode;
    }

    private MovesManager.FragmentPair assembleMoveFromSelection() {
        JTextComponent baseText = dualTextPanel.getLeftTextArea();
        JTextComponent witnessText = dualTextPanel.getRightTextArea();
        MovesManager movesManager = session.getDocumentManager().getMovesManager();
        MovesManager.FragmentPair fp = MovesManager.newFragmentPair();
        fp.first = movesManager.new Fragment(baseDocument, baseText.getSelectionStart(), baseText.getSelectionEnd());
        fp.second = movesManager.new Fragment(witnessDocument, witnessText.getSelectionStart(),
            witnessText.getSelectionEnd());
        return fp;
    }

    private IntPair[] toIntPairArray(List<IntPair> arr) {
        IntPair[] ip = new IntPair[arr.size()];
        for (int i = 0; i < arr.size(); ++i)
            ip[i] = (IntPair) arr.get(i);
        return ip;
    }

    public void setHighlightAllSearchResults(boolean highlight) {
        this.highLightAllSearchResults = highlight;
        if ((searchResults != null) && highlight) {
            List<IntPair> leftSearches = new ArrayList<IntPair>();
            List<IntPair> rightSearches = new ArrayList<IntPair>();
            int leftId = (baseDocument == null) ? 0 : baseDocument.getID();
            int rightId = (witnessDocument == null) ? 0 : witnessDocument.getID();
            for (SearchResult result : searchResults.getSearchResults()) {
                if (leftId == result.getDocumentID())
                    leftSearches.add(new IntPair(result.getOffset(), result.getOffset() + result.getLength()));
                if (rightId == result.getDocumentID())
                    rightSearches.add(new IntPair(result.getOffset(), result.getOffset() + result.getLength()));
            }

            leftHighlighter.setSearchHighlights( toIntPairArray(leftSearches) );
            rightHighlighter.setSearchHighlights( toIntPairArray(rightSearches) );
        } else {
            leftHighlighter.setSearchHighlights(null);
            rightHighlighter.setSearchHighlights(null);
        }
        leftHighlighter.redrawSelection();
        rightHighlighter.redrawSelection();
    }

    public boolean getHighlightAllSearchResults() {
        return highLightAllSearchResults;
    }

    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
        setHighlightAllSearchResults(highLightAllSearchResults); // reselect the results on the text panes
    }
    
    public void clearSearchResults() {

        this.searchResults = null;
        searchHighlightRemoval();
        leftHighlighter.setSearchHighlights(null);
        rightHighlighter.setSearchHighlights(null);
        leftHighlighter.redrawSelection();
        rightHighlighter.redrawSelection();
    }

}
