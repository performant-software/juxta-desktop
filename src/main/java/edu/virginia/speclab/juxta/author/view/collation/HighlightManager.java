/*
 *  Copyright 2002-2011 The Rector and Visitors of the
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.document.NoteData;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.MovesManager;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * Manages all types of highlighting for a document rendered on a text panel
 * @author loufoster
 *
 */
public class HighlightManager {
    private JTextPane txtPane;
    private JuxtaDocument document;
    private Collation collation;
    private MovesManager movesManager;
    private int maxHighlightValue;
    private List<DefaultHighlighter.DefaultHighlightPainter> diffHighlighters;
    private Map<String, DefaultHighlighter.DefaultHighlightPainter> highlighters;
    private Set<HighlightRange> hits;
    private HighlightRange focus;
    private boolean enabled = true;
    
    private boolean selection = false;
    private int selectionStart;
    private int selectionEnd;
    
    public HighlightManager( final JTextPane txtPnl) {
        this.txtPane = txtPnl;
        this.txtPane.setSelectedTextColor(RenderingConstants.DIFF_TEXT_COLOR);
        this.txtPane.setSelectionColor(Color.WHITE);
        this.maxHighlightValue = RenderingConstants.BLUE_DIFFERENCE_SCALE.length - 1;
        
        this.hits = new HashSet<HighlightManager.HighlightRange>();
        
        // create array of the heatmap diff highlighters
        int length = RenderingConstants.BLUE_DIFFERENCE_SCALE.length;
        this.diffHighlighters = new ArrayList<DefaultHighlighter.DefaultHighlightPainter>();
        for (int i = 0; i < length; i++) {
            DefaultHighlighter.DefaultHighlightPainter painter = 
                newHighlighter( RenderingConstants.BLUE_DIFFERENCE_SCALE[i] );
            this.diffHighlighters.add(painter);
        }
        
        // create a map of all other highlighters
        this.highlighters = new HashMap<String, DefaultHighlighter.DefaultHighlightPainter>();
        this.highlighters.put("note-hi", newHighlighter( new Color(254,254,56)) );
        this.highlighters.put("note", newHighlighter( new Color(255,252,200)) );
        this.highlighters.put("hit", newHighlighter( Color.YELLOW) );
        this.highlighters.put("focus", newHighlighter( Color.ORANGE) );
        this.highlighters.put("diff-hi", newHighlighter( RenderingConstants.DIFF_HIGHLIGHT_FILL_COLOR) );
        this.highlighters.put("selected", newHighlighter( RenderingConstants.DIFF_FILL_COLOR) );
    }
    
    // nicer syntax for creating a highlighter
    private DefaultHighlighter.DefaultHighlightPainter newHighlighter(Color c) {
        return new DefaultHighlighter.DefaultHighlightPainter(c);
    }
    

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Clear all highlights from the document
     */
    public void clear() {
        Highlighter highlighter = this.txtPane.getHighlighter();
        highlighter.removeAllHighlights();
    }
    
    public void setMaxHighlightValue(int maxHighlightValue) {
        this.maxHighlightValue = maxHighlightValue;
    }
    
    /**
     * Highlight the specified difference in a brighter color
     * @param difference
     * @param textType
     */
    public void highlightDifference(Difference difference, int textType) {
        if ( this.enabled == false ) {
            return;
        }
        
        clear();
    
        if (difference != null) {
            try {
                // get the offsets for the appropriate text
                int start = difference.getOffset(textType);
                int end = start + difference.getLength(textType);

                // Highlight the selected postion                
                Highlighter highlighter = this.txtPane.getHighlighter();
                highlighter.addHighlight(start, end, this.highlighters.get("diff-hi"));
            } catch (BadLocationException e) {
                SimpleLogger.logError("Attempted to highlight bad location: " + e);
            }
        }

        highlightSelection();
        renderHeatMap();
        highlightNotes();
    }

    public void refreshHeatMap() {
        if ( this.enabled == false ) {
            return;
        }
        clear();
        highlightSelection();
        highlightNotes();
        renderHeatMap();
    }
    
    private void highlightSelection() {
        if (this.selection == false ) {
            return;
        }

        try {
            Highlighter highlighter = this.txtPane.getHighlighter();
            highlighter.addHighlight( this.selectionStart, this.selectionEnd, this.highlighters.get("selected"));
        } catch (BadLocationException e) {
            SimpleLogger.logError("Attempted to highlight bad location: " + e);
        }
    }

    private void renderHeatMap() {
        if (this.collation == null || this.movesManager == null || this.document == null) {
            return;
        }
        
        highlightSearches();

        int start = 0;
        int end = 0;
        int currentFrequency = -1;
        int docLen = this.document.getDocumentText().length();

        // run length encode frequency data to generate highlighting        
        for (int i = 0; i <= docLen; i++) {
            int frequency = this.collation.getDifferenceFrequency(i);
            frequency += this.movesManager.countMoves(this.document.getID(), i);

            // there is something to highlight here
            if (frequency > 0) {
                // check to see if we are in the midst of a run
                if (currentFrequency > 0) {
                    // if we are, is this the same frequency?
                    if (currentFrequency == frequency) {
                        // if it is, then extend this run
                        end++;
                    } else {
                        // if it isn't, then end this run and start a new one
                        addFrequencyHighlight(start, end, currentFrequency);
                        start = i;
                        end = i + 1;
                        currentFrequency = frequency;
                    }
                } else {
                    // if we aren't in the middle of a run, start a new one
                    start = i;
                    end = i + 1;
                    currentFrequency = frequency;
                }
            } else {
                // if we are in the midst of a run, end it
                if (currentFrequency > 0) {
                    addFrequencyHighlight(start, end, currentFrequency);
                    start = end = -1;
                    currentFrequency = -1;
                }
            }
        }
        if (currentFrequency > 0) {
            addFrequencyHighlight(start, end, currentFrequency);
        }
    }
    
    private void highlightSearches() {
        Highlighter highlighter = this.txtPane.getHighlighter();
        if ( this.focus != null ) {
            try {
                highlighter.addHighlight(this.focus.start, this.focus.end, this.highlighters.get("focus"));
            } catch (BadLocationException e) {
                SimpleLogger.logError("Attempted to highlight search at bad location: " + e);
            }
        }
        
        for (HighlightRange r : this.hits) {
            try {
                highlighter.addHighlight(r.start, r.end, this.highlighters.get("hit"));
            } catch (BadLocationException e) {
                SimpleLogger.logError("Attempted to highlight search at bad location: " + e);
            }
        }
    }

    public void higlightFocusRange(int start, int end) {
        if ( this.enabled == false ) {
            return;
        }
        this.focus = new HighlightRange(start, end);
    }
    
    public void addSearchResult(int start, int end) {
        this.hits.add( new HighlightRange(start, end));
    }
    
    public void clearSearchResults() {
        this.hits.clear();
        this.focus = null;
    }

    public void updateModel(Collation collation, JuxtaDocument document, MovesManager movesManager) {
        this.collation = collation;
        this.document = document;
        this.movesManager = movesManager;

        clear();
        if (collation != null) {
            renderHeatMap();
            highlightNotes();
        }
    }
    
    private void highlightNotes() {
        if ( this.document == null ) {
            return;
        }
        
        Highlighter highlighter = this.txtPane.getHighlighter();
        for (NoteData note : this.document.getNotes()) {
            if (note.getTargetID() != null) {
                OffsetRange range = note.getAnchorRange();
                if (range != null) {
                    int start = range.getStartOffset(Space.ACTIVE);
                    int end = range.getEndOffset(Space.ACTIVE) + 1;
                    try {
                        highlighter.addHighlight(start, end, this.highlighters.get("note"));
                    } catch (BadLocationException e) {
                        SimpleLogger.logError("Attempted to highlight note at bad location: " + e);
                    }
                }
            }
        }
    }
    
    private void addFrequencyHighlight(int start, int end, int frequency) {
        // scale frequency value to color scale
        int diffHighlightLen = (this.diffHighlighters.size() - 1);
        double value = ((double) frequency / (double) maxHighlightValue) * diffHighlightLen;
        int idx = (int) Math.floor(value);
        idx = Math.max(0, idx);
        idx = Math.min(idx, diffHighlightLen);

        try {
            // Highlight the selected position
            Highlighter highlighter = this.txtPane.getHighlighter();
            highlighter.addHighlight(start, end, this.diffHighlighters.get(idx));
        } catch (BadLocationException e) {
            SimpleLogger.logError("Attempted to highlight bad location: " + e);
        }
    }

    public void setNoteHighlight(int start, int end, boolean highlight) {
        if ( this.enabled == false ) {
            return;
        }
        clear();
        String name = "note";
        if ( highlight ) {
            name = "note-hi";
        } else {
            // when not note highlighting, render map first so it
            // takes precedence over normal notes
            highlightSelection();
            renderHeatMap();
        }
        try {
            // Highlight the selected position
            Highlighter highlighter = this.txtPane.getHighlighter();
            highlighter.addHighlight(start, end, this.highlighters.get(name));
        } catch (BadLocationException e) {
            SimpleLogger.logError("Attempted to highlight note bad location: " + e);
        }
        
        if (highlight ) {
            highlightSelection();
            renderHeatMap();
        }
        highlightNotes();  
    }
   
    public void clearRangeSelection() {
        this.selection = false;
        refreshHeatMap();
    }
    
    public void setRangeSelection(int start, int end) {
        if ( this.enabled == false ) {
            return;
        }
        this.selection = true;
        this.selectionStart = start;
        this.selectionEnd = end;
        refreshHeatMap();
    }
    
    /**
     * helper class to bind together info about a highlight range
     * @author loufoster
     *
     */
    private static class HighlightRange {
        final int start;
        final int end;
        HighlightRange(int s, int e) {
            this.start = s;
            this.end = e;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HighlightRange other = (HighlightRange) obj;
            if (end != other.end) {
                return false;
            }
            if (start != other.start) {
                return false;
            }
            return true;
        }
    }
}
