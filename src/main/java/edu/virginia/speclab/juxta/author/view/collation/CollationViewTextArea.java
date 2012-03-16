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

package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.document.PageBreakData;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.AnnotationListener;
import edu.virginia.speclab.juxta.author.model.AnnotationManager;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.Revision;
import edu.virginia.speclab.juxta.author.model.Revision.Type;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import edu.virginia.speclab.juxta.author.model.SearchResults.SearchResult;
import edu.virginia.speclab.juxta.author.view.AnnotationDialog;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.LineMarkController;
import edu.virginia.speclab.ui.LocationMarkStrip;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * The top level view object for document display. Displays documents with highlighted 
 * collation results. Houses display and control of difference popups and document area 
 * highlighting.
 * 
 * @author Nick
 *
 */
public class CollationViewTextArea extends JTextPane implements AnnotationListener {

    private JuxtaSession session;
    private JuxtaDocument document;
    private List<Difference> differenceList;
    private int selectedOffset;
    private DifferencePainter painter;
    private HighlightManager highlightMgr;
    private TextSelectionTracker selectionTracker;
    private LocationMarkStrip locationMarkStrip;
    private LinkedList<DifferenceViewerListener> listeners;
    private LineMarkController markController;
    private JuxtaAuthorFrame juxtaAuthorFrame;
    private boolean highLightAllSearchResults;
    private SearchResults searchResults;
    private NotesManager notesManager; 
    private PageBreakManager breakManager;
    
    public CollationViewTextArea(JuxtaAuthorFrame juxtaAuthorFrame) {
        
        super();
        setMargin( new Insets(5,5,5,RenderingConstants.MARGIN_SIZE));
        setContentType("text/plain; charset=UTF-8");
        setEditable( false );
        
        this.juxtaAuthorFrame = juxtaAuthorFrame;
        this.listeners = new LinkedList<DifferenceViewerListener>();
        this.differenceList = new LinkedList<Difference>();

        this.locationMarkStrip = new LocationMarkStrip(this, false, LocationMarkStrip.Position.RIGHT);
        this.locationMarkStrip.setBackground(RenderingConstants.THIRD_COLOR_WHITE);
        showLocationMarkStrip(false);

        this.highlightMgr = new HighlightManager(this);
        this.markController = new LineMarkController(locationMarkStrip);
        this.selectionTracker = new TextSelectionTracker(this);
        
        // default plain text style
        Style plain = addStyle("plain", null);
        StyleConstants.setForeground(plain, Color.BLACK);
        StyleConstants.setBackground(plain, Color.WHITE);
        StyleConstants.setStrikeThrough(plain, false);
        StyleConstants.setUnderline(plain, false);
        
        Style revision = addStyle("revision", null);
        StyleConstants.setForeground(revision, new Color(128,0,0));
        StyleConstants.setBold(revision, true);
        
        Style pb = addStyle("pb", null);
        StyleConstants.setForeground(pb, new Color(140,140,140));
        StyleConstants.setItalic(pb, true);
    }

    public void setTextFont(Font newFont) {
        this.setFont(newFont);
    }

    public void addListener(DifferenceViewerListener listener) {
        listeners.add(listener);
    }

    private void fireDocumentChanged(JuxtaDocument document) {
        for (DifferenceViewerListener listener : this.listeners) {
            listener.documentChanged(document);
        }
    }

    private void initTextArea(String text) {

        // adjust margins to handle page break markers
        if ( this.document != null ) {
            if ( this.document.getPageBreaks().size() >0 ) {
                setMargin( new Insets(5,RenderingConstants.PB_MARGIN_SIZE,5,RenderingConstants.MARGIN_SIZE));
            } else {
                setMargin( new Insets(5,5,5,RenderingConstants.MARGIN_SIZE));
            }
        }
        
        setText(text);
        setSelectionStart(-1);
        setSelectionEnd(-1);
        setCharacterAttributes(getStyle("plain"), true);
                
        this.locationMarkStrip.recalculateLineHeight();
        this.markController.updateLineMarkers(this.document, this);
        
        if ( this.document != null) {
            for ( Revision rev : this.document.getAcceptedRevisions()) {
                if (rev.getType().equals(Type.ADD)) {
                    int start = rev.getStartOffset(Space.ACTIVE);
                    int end = rev.getEndOffset(Space.ACTIVE)+1;
                    setSelectionStart(start);
                    setSelectionEnd(end);
                    setCharacterAttributes(getStyle("revision"), false);
                }
            }
        }
        
        setCaretPosition(0);
        updateSize();
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
               
        if (painter == null) {
            FontRenderContext fontRenderContext = g2.getFontRenderContext();
            painter = new DifferencePainter(this, selectedOffset, fontRenderContext,
                session.getDocumentManager(), session.getAnnotationManager());
            painter.initialize( differenceList );
        } 
        this.painter.paint(g2);
        
        if ( this.notesManager == null ) {
            this.notesManager = new NotesManager(this, g2.getFontRenderContext());
            this.juxtaAuthorFrame.addComponentListener( this.notesManager );
            this.notesManager.initNotes( this.document );
        } else {
            this.notesManager.paint(g2);
        }
        
        if ( this.breakManager == null ) {
            this.breakManager = new PageBreakManager(this );
            this.juxtaAuthorFrame.addComponentListener( this.breakManager );
            this.breakManager.initialize( this.document );
        } else {
            this.breakManager.paint(g2);
        }
    }

    public void centerOnDifference(Difference difference) {
        // if there are differences on the list, scroll to the first one.
        int offset = difference.getOffset(Difference.BASE);
        centerOffset(offset);
    }

    /**
     * Recalculates and redraws surface.
     *
     */
    public void redraw() {
        // if there is already a painter going, terminate it
        if (painter != null) {
            painter = null;
        }

        // render the diff panel with the new info
        repaint();
        
    }

    public boolean selectDifference(Point p) {
        if (this.painter != null) {
            
            // see if a note was clicked
            NoteBox box = this.notesManager.getNoteBox(p);
            if ( box != null ) {
                int offset = box.getNoteTextStartOffset();
                setSelectedOffset(offset);
                juxtaAuthorFrame.makeSourcePaneVisible( this.document, offset, Space.ORIGINAL );
                redraw();
                return true;
            } else {
                // get the selected difference
                Difference difference = painter.getDifference(p.x, p.y);
    
                if (difference != null) {
                    if (painter.isAnnotationSelected(p)) {
                        selectAnnotation(difference);
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Determine which xml tag contains the point clicked and notify
     * listeners of its selection 
     * @param p
     */
    public void handleClick(int offset) {
        setSelectedOffset(offset);
        juxtaAuthorFrame.makeSourcePaneVisible( this.document, offset );
        redraw();
    }
    
    public boolean isMarginBoxAnnotationLink(Point p) {
        if ( this.painter != null ) {
            return this.painter.isAnnotationSelected(p);
        }
        return false;
    }
    
    public JuxtaAuthorFrame getFrame() {
        return this.juxtaAuthorFrame;
    }

    private void selectAnnotation(Difference difference) {
        boolean newAnnotation = false;
        AnnotationManager annotationManager = session.getAnnotationManager();

        Annotation annotation = annotationManager.getAnnotation(difference);

        // if this difference is not already annotated, create
        // a new annotation for it        
        if (annotation == null) {
            annotation = annotationManager.addAnnotation(difference);
            newAnnotation = true;
        }

        JuxtaDocument witnessDocument = (JuxtaDocument) session.getDocumentManager().lookupDocument(
            difference.getWitnessDocumentID());

        AnnotationDialog dialog = new AnnotationDialog(annotation, (JuxtaDocument) document, witnessDocument,
            this.juxtaAuthorFrame);
        dialog.setVisible(true);

        if (dialog.isOk()) {
            annotation.setIncludeImage(dialog.includeImage());
            annotationManager.markAnnotation(annotation, dialog.getNotes());
        } else if (newAnnotation) {
            annotationManager.removeAnnotation(annotation);
        }
    }

    private void setCurrentText(JuxtaDocument document) {
        this.document = document;

        if (document != null) {
            initTextArea(document.getDocumentText());
        } else {
            initTextArea("");
        }

        // if we have a listener, inform it of the change
        fireDocumentChanged(document);

        // reset the highlighter
        Collation currentCollation = this.session.getCurrentCollation();

        this.highlightMgr.updateModel(currentCollation, document, this.session.getDocumentManager().getMovesManager());
        int docId = (document == null) ? 0 : document.getID();
        this.selectionTracker.setCollation(currentCollation, this.session.getDocumentManager().getMovesManager(), docId);

        // clear the list of selected differences        
        this.differenceList.clear();

        // reselect the results on the text panes
        setHighlightAllSearchResults(this.highLightAllSearchResults); 
        
        //  if the not manager exists, reset it and a new one will
        // be created and initialized on the next paint
        if ( this.notesManager != null ) {
            this.juxtaAuthorFrame.removeComponentListener( this.notesManager );
            this.notesManager = null;
        }
        if ( this.breakManager != null ) {
            this.juxtaAuthorFrame.removeComponentListener( this.breakManager );
            this.breakManager = null;
        }

        // reset the painter
        redraw();
    }

    public void refreshHeatMap() {
        this.highlightMgr.refreshHeatMap();
    }

    private void updateHighlighterScale() {
        if ( this.session != null) {
            int numberOfDocuments = this.session.getDocumentManager().getDocumentList().size();
            this.highlightMgr.setMaxHighlightValue(numberOfDocuments);
        }
    }

    public void setJuxtaDocument(JuxtaDocument currentDocument) {
        updateHighlighterScale();
        setCurrentText(currentDocument);
        centerOffset(0);
    }
    
    public JuxtaDocument getJuxtaDocument() {
        return this.document;
    }

    public void setLocation(Difference difference) {
        if (difference != null) {
            // select offset should be calculated 
            setSelectedOffset(-1);

            // remove other differences from the list
            differenceList.clear();

            // center the viewport on this difference
            centerOnDifference(difference);

            // display the selected difference
            if (difference.getType() != Difference.NONE) {
                differenceList.add(difference);
            }

            // highlight the difference   
            int startOffset = difference.getOffset(Difference.BASE);
            int endOffset = startOffset + difference.getLength(Difference.BASE);
            this.highlightMgr.clearRangeSelection();
            this.highlightMgr.higlightFocusRange(startOffset, endOffset);
            redraw();
        }
    }

    public void clearRangeSelection() {
        this.highlightMgr.clearRangeSelection();
    }

    public void highlightRangeSelection(int start, int end) {
        this.highlightMgr.setRangeSelection(start, end);
    }
    
    public void setNoteHighlight(OffsetRange range, boolean highlight) {
        int start = range.getStartOffset(Space.ACTIVE);
        int end = range.getEndOffset(Space.ACTIVE)+1;
        this.highlightMgr.setNoteHighlight(start, end, highlight);
        repaint();
    }

    public void setDifferenceList(List<Difference> differenceList) {
        if (differenceList == null) {
            this.differenceList.clear();
        } else {
            this.differenceList = differenceList;
        }
        
        if ( this.notesManager != null) {
            this.notesManager.setEnabled( (differenceList==null) );
        }
    }

    public void setSelectedOffset(int selectedOffset) {
        this.selectedOffset = selectedOffset;
    }

    public void setSession(JuxtaSession session) {
        if (this.session != null) {
            session.getAnnotationManager().removeListener(this);
        }

        this.session = session;

        if (this.session != null) {
            session.getAnnotationManager().addListener(this);
        }
        setSearchResults(null);
    }

    public JuxtaDocument getCurrentText() {
        return document;
    }

    public void clearShading() {
        repaint();
    }

    public void showLocationMarkStrip(boolean visible) {
        locationMarkStrip.setVisible(visible);
    }

    public boolean isLocationMarkStripVisible() {
        return locationMarkStrip.isVisible();
    }

    public LocationMarkStrip getLocationMarkStrip() {
        return locationMarkStrip;
    }

    public void annotationAdded(Annotation annotation) {
        Difference difference = annotation.getDifference();
        if (painter != null)
            painter.setAnnotationMark(difference, true);
        repaint();
    }

    public void annotationRemoved(Annotation annotation) {
        Difference difference = annotation.getDifference();
        if (painter != null)
            painter.setAnnotationMark(difference, false);
        repaint();
    }

    public void annotationMarked(Annotation annotation) {
        Difference difference = annotation.getDifference();
        if (painter != null)
            painter.setAnnotationMark(difference, true);
        repaint();
    }

    /**
     * called when mouse is moved over this text area. check if pointer is
     * over a difference or an highlight and modify the display accordingly
     * @param point
     */
    public void rollOver(Point point) {    
        if ( this.painter != null && this.painter.isEnabled() ) {
            Difference difference = painter.getDifference(point.x, point.y);
           
            // if a highlight changed state, then repaint
            if (painter.highlightDifference(difference)) {
                this.highlightMgr.highlightDifference(difference, Difference.BASE);
                repaint();
            }
        }
        
        // only handle mouseover on notes if the notes
        // are currently viewed!
        if ( this.notesManager != null && this.notesManager.isEnabled() ) {
            this.notesManager.rollover(point);
        }
        if ( this.breakManager != null ) {
            this.breakManager.rollover(point);
        }
    }

    public void setHighlightAllSearchResults(boolean highlight) {
        this.highLightAllSearchResults = highlight;
        this.highlightMgr.clearSearchResults();
        if ((searchResults != null) && highlight && (document != null)) {
            int id = document.getID();
            
            for ( SearchResult result : this.searchResults.getSearchResults() ) {
                if (id == result.getDocumentID()) {
                    this.highlightMgr.addSearchResult(result.getOffset(), result.getOffset() + result.getLength());
                }
            }
        } 
        this.highlightMgr.refreshHeatMap();
    }
    
    public void clearSearchResults() {
        this.searchResults = null;
        this.highlightMgr.clearSearchResults();
        this.highlightMgr.refreshHeatMap();
    }

    public boolean getHighlightAllSearchResults() {
        return highLightAllSearchResults;
    }

    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
        setHighlightAllSearchResults(highLightAllSearchResults); // reselect the results on the text panes
    }
    
    /**
     * At a minimum, the drawing surface is always the height of the text area plus the height of 
     * the top and bottom margins combined. The height of the drawing surface can be further 
     * extended below the bottom margin with this method. 
     * 
     * @param extension The height of the extension. If less than or equal to zero, the existing
     * extension is removed.
     */
    public void extendDrawingSurfaceHeight( final int extendedHeight ) { 
         SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                View v = getUI().getRootView(CollationViewTextArea.this);
                Insets margin = getMargin();
                int pw = getParent().getWidth() - RenderingConstants.MARGIN_SIZE - margin.left;
                v.setSize(pw, Integer.MAX_VALUE);
                int preferredHeight = (int)v.getPreferredSpan(View.Y_AXIS) + margin.top + margin.bottom;  
                if ( extendedHeight > preferredHeight ) {
                    Dimension prefferedSize = new Dimension(pw, extendedHeight);         
                    setPreferredSize(prefferedSize);
                    revalidate();
                }
            }
        });
    }
    
    /**
     * Scroll to a position that is <code>docPercent</code> percent
     * in the current document
     * @param docPercent
     */
    public void scrollToPercent(float position) {
        if (getText() == null){
            return;
        }
        int textLength = getText().length();
        int targetPosition = Math.round((float) textLength * position);

        try {
            Rectangle comparandRect = modelToView(targetPosition);
            Rectangle viewRect = getVisibleRect();

            if (viewRect != null && comparandRect != null) {
                comparandRect.height = viewRect.height;
                if (comparandRect.y < 0) {
                    comparandRect.y = 0;
                }
                scrollRectToVisible(comparandRect);
            }
        } catch (BadLocationException e) {
            SimpleLogger.logError("Unable to scroll to position: " + position);
        }
    }
    
    /**
     * Center the display on the specified character offset position
     * @param offset Character offset position
     */
    public void centerOffset(final int offset) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    scrollRectToVisible(modelToView(0));
                    setCaretPosition(offset);
                    Rectangle comparandRect = modelToView(offset);
                    if (comparandRect != null) {
                        Rectangle viewRect = getVisibleRect();
                        comparandRect.y += viewRect.height / 2;
                        scrollRectToVisible(comparandRect);
                    }
                } catch (Exception e) {
                    SimpleLogger.logError("Unable to scroll CollationViewTextArea to offset: " + offset);
                }
            }
        });
    }

    /**
     * Enable/disable the heatmap view of the text. When diabled, the text
     * will be renderd plain and all mouse interactivity for margin boxes
     * and notes will also be disabled
     */
    public void setHeatmapEnabled( boolean enabled ) {
        if ( enabled == false ) {
            if (this.notesManager != null) {
                this.notesManager.setEnabled(false);
            }
            if ( this.painter != null ) {
                this.painter.setEnabled(false);
            }
            this.highlightMgr.clear();
            this.highlightMgr.setEnabled(false);
            this.selectionTracker.setEnabled(false);
        } else {
            this.highlightMgr.setEnabled(true);
            this.highlightMgr.refreshHeatMap();
            if ( this.notesManager != null) {
                this.notesManager.setEnabled(true);
            }
            if ( this.painter != null ) {
                this.painter.setEnabled(true);
            }
            this.selectionTracker.setEnabled(true);
            
            setSelectionStart(0);
            setSelectionEnd(0);
            setCharacterAttributes(getStyle("plain"), true);
            setJuxtaDocument( this.document );
        }   
    }

    public void updateSize() {    
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                Insets margin = getMargin();
                View v = getUI().getRootView(CollationViewTextArea.this);
                int pw = getParent().getWidth() - RenderingConstants.MARGIN_SIZE - margin.left;
                v.setSize(pw, Integer.MAX_VALUE);  
                int preferredHeight = (int)v.getPreferredSpan(View.Y_AXIS) + margin.top + margin.bottom;  
                Dimension prefferedSize = new Dimension(pw, preferredHeight);         
                setPreferredSize(prefferedSize);
                revalidate();
            }
        });
    }

    @Override
    public void setCursor(Cursor csr) {
        if ( this.juxtaAuthorFrame != null ) {
            this.juxtaAuthorFrame.setCursor(csr);
        }
    }

    public void selectText(int offset, int length) {
        this.highlightMgr.setRangeSelection(offset, offset+length);
        centerOffset(offset);        
    }

    public void previousPage() {
        int currPageIndex = findCurrentPageIndex();
        int len = this.document.getPageBreaks().size();
        currPageIndex--;
        if ( currPageIndex < 0) {
            currPageIndex = len -1;
        }
        System.err.println("  NEW PAGE "+currPageIndex);
        PageBreakData pbd = this.document.getPageBreaks().get(currPageIndex);
        int pos = pbd.getRange().getStartOffset(Space.ACTIVE);
        centerOffset(pos);
        
    }
    
    private int findCurrentPageIndex() {
        Rectangle r = getVisibleRect();
        Point pt = new Point( r.x, r.y+r.height);
        int scrollPos = viewToModel(pt);
        int currPage = 0;
        for ( PageBreakData pbd : this.document.getPageBreaks()) {
            int pagePos = pbd.getRange().getStartOffset(Space.ACTIVE);
            if (pagePos > scrollPos ) {
                currPage--;
                break;
            } else {
                currPage++;
            }
        }
        currPage = Math.max(0, currPage);
        currPage = Math.min(this.document.getPageBreaks().size()-1, currPage);
        //System.err.println("CURR PAGE "+currPage);
        return currPage;
    }
    public void nextPage() {
        int currPageIndex = findCurrentPageIndex();
        int len = this.document.getPageBreaks().size();
        currPageIndex++;
        if ( currPageIndex >= len ) {
            currPageIndex = 0;
        }
       // System.err.println("  NEW PAGE "+currPageIndex);
        PageBreakData pbd = this.document.getPageBreaks().get(currPageIndex);
        int pos = pbd.getRange().getStartOffset(Space.ACTIVE);
        centerOffset(pos);
    }
}
