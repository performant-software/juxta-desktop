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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ToolTipManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.View;

import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.Revision;
import edu.virginia.speclab.juxta.author.model.Revision.Type;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This is the manager/controller used to navigate, accept
 * and decline revisions in a document
 * 
 * @author loufoster
 *
 */
public class RevisionManager implements MouseListener, MouseMotionListener {
    private CollationViewTextArea view;
    private Integer revisionIndex;
    private Map<Integer, RevisionOffset> revisionOffsetMap;
    private List<Integer> workingRevisionIndexes;
    private int totalRevisions;
    private StyleContext styles;
    
    public RevisionManager(CollationViewTextArea view) {
        this.view = view;
        this.revisionOffsetMap = new LinkedHashMap<Integer, RevisionManager.RevisionOffset>();
        this.workingRevisionIndexes = new ArrayList<Integer>();
        
        // setup markup attributes
        this.styles = StyleContext.getDefaultStyleContext();
        Style add = this.styles.addStyle("add", null);
        StyleConstants.setForeground(add, JuxtaUserInterfaceStyle.ADD_TEXT_COLOR);
        StyleConstants.setBackground(add, Color.WHITE);
        StyleConstants.setUnderline(add, true);
        StyleConstants.setStrikeThrough(add, false);
        
        Style del = this.styles.addStyle("delete", null);
        StyleConstants.setForeground(del, JuxtaUserInterfaceStyle.DEL_TEXT_COLOR);
        StyleConstants.setBackground(del, Color.WHITE);
        StyleConstants.setUnderline(del, false);
        StyleConstants.setStrikeThrough(del, true);
        
        Style reject = this.styles.addStyle("reject", null);
        StyleConstants.setBackground(reject, new Color(209,209,209));
        StyleConstants.setBold(reject, false);
        
        Style accept = this.styles.addStyle("accept", null);
        StyleConstants.setBackground(accept, new Color(150,228,133));
        StyleConstants.setBold(accept, false);
        
        Style focusAdd = this.styles.addStyle("focus-add", null);
        StyleConstants.setBackground(focusAdd, new Color(96,96,96));
        StyleConstants.setForeground(focusAdd, Color.WHITE);
        StyleConstants.setUnderline(focusAdd, true);
        StyleConstants.setStrikeThrough(focusAdd, false);
        
        Style focusDel = this.styles.addStyle("focus-del",null);
        StyleConstants.setForeground(focusDel, new Color(220,90,90) );
        StyleConstants.setBackground(focusDel, new Color(96,96,96));
        StyleConstants.setForeground(focusDel, Color.WHITE);
        StyleConstants.setUnderline(focusDel, false);
        StyleConstants.setStrikeThrough(focusDel, true);
        
        Style focusAccept = this.styles.addStyle("focus-accepted", null);
        StyleConstants.setForeground(focusAccept, new Color(130,225,130) );
        StyleConstants.setBackground(focusAccept, new Color(96,96,96));
    }
    
    /**
     * Enable/disable the revsion manager. When enabled, the mouse
     * controls are activated. This include hover for revision tooltip
     * and clicking to accept/reject revisions/
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        if ( enabled ) {
            this.view.addMouseListener( this );
            this.view.addMouseMotionListener( this );
        } else {
            this.view.removeMouseListener( this );
            this.view.removeMouseMotionListener( this );
        }
    }
    
    /**
     * Enabled/Disable the revision mouse hovers only. 
     *
     * @param enabled
     */
    public void enableRevisionHover(boolean hoverEnabled) {
        if ( hoverEnabled ) {
            this.view.addMouseMotionListener( this );
        } else {

            this.view.removeMouseMotionListener( this );
        }
    }
    
    public void showRevisions() {
        JuxtaDocument doc = this.view.getJuxtaDocument();
        this.totalRevisions = doc.getRevisions().size();
        this.revisionIndex = 0;
        this.workingRevisionIndexes.clear();
        this.workingRevisionIndexes.addAll( doc.getAcceptedRevisionIndexes() );
        Integer index = 0;
        int positionOffset = 0;
        
        // generate a map of revsion offsets that will be
        // used to navigate and select them from this manager
        for ( Revision revision : doc.getRevisions() ) {
            
            // get all position data for this revision as well as the text
            int startActive = revision.getStartOffset( Space.ACTIVE );
            int endActive = revision.getEndOffset( Space.ACTIVE )+1;
            int startOrig = revision.getStartOffset( Space.ORIGINAL );
            int endOrig = revision.getEndOffset( Space.ORIGINAL );
            String revText = doc.getSourceDocument().getRawXMLContent().substring(startOrig,endOrig);
            RevisionOffset revOffset = new RevisionOffset();
            revOffset.type = revision.getType();

            // Is this a revision that has been accepted in the current
            // form of the document?
            if ( doc.getAcceptedRevisionIndexes().contains(index)) {
                // Accepted adds will already have their text in the document. Just
                // accept the active offsets as is (plus any accumulated offsets from
                // other revisions) and set styles
                if ( revision.getType().equals(Type.ADD)) {
                    revOffset.selectionStart = startActive + positionOffset;
                    revOffset.selectionEnd = endActive + positionOffset;
                    this.view.setSelectionStart(revOffset.selectionStart);
                    this.view.setSelectionEnd(revOffset.selectionEnd);
                    this.view.setCharacterAttributes( this.styles.getStyle("add"), true);
                } else {
                    // Accepted deletes have had their text removed  from the document,
                    // but the deleted text must be added to this edit view. This addition
                    // must be accounted for so tack its lenth on to a running offset
                    // adjust to keep all future revisions in line.                    
                    revOffset.selectionStart = startActive+positionOffset+1;
                    revOffset.selectionEnd = revOffset.selectionStart+revText.length();
                    positionOffset += revText.length();
                    try {
                        this.view.getDocument().insertString(revOffset.selectionStart, revText, 
                            this.styles.getStyle("delete"));
                    } catch (BadLocationException e) {
                        SimpleLogger.logError("Unable insert deleted '" + revText + "' at pos " 
                            + revOffset.selectionStart +" : " + e.toString());
                    }
                }
            } else {
                // Unaccepted deletions still have their text as-is in the doc.
                // Just accept active offsets, plus any prior accumulated offset
                // and style the text as deleted
                if ( revision.getType().equals(Type.DELETE)) {
                    revOffset.selectionStart = startActive + positionOffset;
                    revOffset.selectionEnd = endActive + positionOffset;
                    this.view.setSelectionStart(revOffset.selectionStart);
                    this.view.setSelectionEnd(revOffset.selectionEnd);
                    this.view.setCharacterAttributes( this.styles.getStyle("delete"), true);
                } else {
                    // Unaccepted adds do NOT have their text in the doc. Insert style text
                    // and account for the extra length.
                    // Start is the same, but end is start plus length. Add this length
                    // to a running offest total to keep all future revisons in line
                    revOffset.selectionStart = startActive+positionOffset+1;
                    revOffset.selectionEnd = revOffset.selectionStart+revText.length();
                    positionOffset += revText.length();
                    try {
                        this.view.getDocument().insertString(revOffset.selectionStart, revText, 
                            this.styles.getStyle("add"));
                    } catch (BadLocationException e) {
                        SimpleLogger.logError("Unable insert added '" + revText + "' at pos " 
                            + revOffset.selectionStart +" : " + e.toString());
                    }
                }
            }   
            
            // add it to the offsets map and do an initial render
            this.revisionOffsetMap.put(index, revOffset);
            renderRevision( index, (index == this.revisionIndex) );
            index++;
        }
        
        // scroll to the first active revision
        RevisionOffset ro = this.revisionOffsetMap.get(this.revisionIndex);
        this.view.setCaretPosition(ro.selectionStart);
        
        // Determine new size to show doc. Formula (ratio of len = ratio of height):
        //    origLen / newLen = origPreferredHeight / finalHeight <- need this
        // solved for final:
        //   finalHeight = (origPreferredHeight * newLen) / origLen
        float newLen = this.view.getDocument().getLength();
        View v = this.view.getUI().getRootView(this.view);
        Insets margin = this.view.getMargin();
        int pw = this.view.getPreferredSize().width - RenderingConstants.MARGIN_SIZE - margin.left;
        v.setSize(pw, Integer.MAX_VALUE);        
        float origPreferred = v.getPreferredSpan(View.Y_AXIS) + margin.top + margin.bottom; 
        float preferredHeight = (origPreferred * newLen) / doc.getDocumentLength();
        Dimension prefferedSize = new Dimension(pw, (int) preferredHeight);         
        this.view.setPreferredSize(prefferedSize);
    }
    
    private void renderRevision(Integer index, boolean active) {
        RevisionOffset revOffset = this.revisionOffsetMap.get(index);
        this.view.setSelectionStart(revOffset.selectionStart);
        this.view.setSelectionEnd(revOffset.selectionEnd);
        
        // now add styles for revision management
        if ( active ) {
            if ( this.workingRevisionIndexes.contains(index) ) {
                this.view.setCharacterAttributes(this.styles.getStyle("focus-accepted"), true);  
            } else {
                if ( revOffset.type.equals(Type.ADD)) {
                    this.view.setCharacterAttributes(this.styles.getStyle("focus-add"), true);
                } else {
                    this.view.setCharacterAttributes(this.styles.getStyle("focus-del"), true);
                }
            }
        } else {
            // first, force the default revision style
            String style = "delete";
            if ( revOffset.type.equals(Type.ADD)) {
                style = "add";
            }
            this.view.setCharacterAttributes(this.styles.getStyle(style), true); 
            
            if ( this.workingRevisionIndexes.contains(index) ) {
                this.view.setCharacterAttributes(this.styles.getStyle("accept"), false);
            } else {
                this.view.setCharacterAttributes(this.styles.getStyle("reject"), false);
            }
        }
        
        this.view.setCaretPosition(revOffset.selectionStart);
    }

    public void nextRevision() {
        renderRevision(this.revisionIndex, false);
        this.revisionIndex++;
        if ( this.revisionIndex >= this.totalRevisions) {
            this.revisionIndex = 0;
        }
        renderRevision(this.revisionIndex, true);
    }
  
    public void previousRevision() {
        renderRevision(this.revisionIndex, false);
        this.revisionIndex--;
        if ( this.revisionIndex < 0) {
            this.revisionIndex = this.totalRevisions-1;
        }
        renderRevision(this.revisionIndex, true);
    }
       

    public void acceptRevision() {
        if ( this.workingRevisionIndexes.contains(this.revisionIndex) == false ) {
            this.workingRevisionIndexes.add( this.revisionIndex );
            renderRevision(this.revisionIndex, true);
        }
    }
    
    public void acceptAllRevisions() {
        this.workingRevisionIndexes.clear();
        for (Integer idx=0; idx<this.totalRevisions; idx++) {
            this.workingRevisionIndexes.add(idx);
        }
        renderAllRevisions();
    }

    public void rejectRevision() {
        this.workingRevisionIndexes.remove( this.revisionIndex );
        renderRevision(this.revisionIndex, true);
    }
    
    public void rejectAllRevisions() {
        this.workingRevisionIndexes.clear();
        renderAllRevisions();
    }
    
    private void renderAllRevisions() {
        for (int idx=0; idx<this.totalRevisions;idx++) {
            renderRevision( idx, (this.revisionIndex==idx));
        }
    }
    
    public void cancelChanges() {
        this.workingRevisionIndexes.clear();
    }
    
    public void commitChanges(JuxtaSession session) throws LoggedException {
        JuxtaDocument doc = this.view.getJuxtaDocument();
        doc.setAcceptedRevisions(this.workingRevisionIndexes);
        JuxtaDocumentFactory factory = new JuxtaDocumentFactory(doc.getEncoding());
        factory.reparseDocument(doc, null);
        session.refreshComparisonSet();
        session.markAsModified();
    }
    
    public void mouseReleased(MouseEvent event) {
        // find the revision under the mouse click (if any)
        int clickPos = this.view.viewToModel( event.getPoint() );
        for (Entry<Integer, RevisionOffset> set : this.revisionOffsetMap.entrySet() ) {
            if ( set.getValue().contains(clickPos) ) {
                // set active and toggle its acceped/rejected status
                renderRevision(this.revisionIndex, false);
                this.revisionIndex = set.getKey();
                if ( this.workingRevisionIndexes.contains( this.revisionIndex)) {
                    this.workingRevisionIndexes.remove( this.revisionIndex );
                } else {
                    this.workingRevisionIndexes.add( this.revisionIndex );
                }
                renderRevision(this.revisionIndex, true);
                break;
            }       
        }        
    }

    public void mouseMoved(MouseEvent event) {
        int pos = this.view.viewToModel( event.getPoint() );
        RevisionOffset hitRevision = null;
        for (Entry<Integer, RevisionOffset> set : this.revisionOffsetMap.entrySet() ) {
            if ( set.getValue().contains(pos) ) {
                hitRevision = set.getValue();
                break;
            }  
        }      
        
        if (hitRevision != null) {
            String origVer = getVersionedTextFragment(hitRevision.selectionStart, hitRevision.selectionEnd, false);
            String revisedVer = getVersionedTextFragment(hitRevision.selectionStart, hitRevision.selectionEnd, true);
            this.view.setToolTipText("<html><table><tr><td><b>Original:</b></td><td>\"" + origVer + "\"</td></tr>" +
                    "<tr><td><b>Revised:</b></td><td>\"" + revisedVer + "\"</td></tr></table></html>");
            this.view.setCursor(JuxtaUserInterfaceStyle.HOTSPOT_CURSOR);
            ToolTipManager.sharedInstance().setDismissDelay(5*60*1000);
        } else {
            this.view.setCursor(JuxtaUserInterfaceStyle.NORMAL_CURSOR);
            this.view.setToolTipText(null);
        }
    }

    private String getVersionedTextFragment(int revisionStart, int revisionEnd, boolean revised) {
        try {
            // NOTE: this offset pos is in terms of the text presented in the
            // diplomatic text vew. It includes ALL additions and deletions
            int min = Math.max(0, revisionStart - 20);
            int max = Math.min(revisionEnd + 20, this.view.getDocument().getLength());
            String frag = this.view.getDocument().getText(min, (max - min));

            // run thru ALL revisions and toss those that fall outside
            // of out min/max fragment range. Update fragment with those
            // that fall within its limits. Note that the revisionMap is a LinkedHashMap
            // so its order of iteration is predictable; the order added. In this case, that
            // is increasing numeric index (and therfore increasing offset into doc)
            int delTxtLen = 0;
            for (Entry<Integer, RevisionOffset>  entry : this.revisionOffsetMap.entrySet()) {
                Integer idx = entry.getKey();
                RevisionOffset ro = this.revisionOffsetMap.get(idx);
                
                // skip all of those that occur before this fragment
                if (ro.selectionEnd-delTxtLen < min ) {
                    continue;
                }
                
                // once we are past the end of the fragment STOP
                if ( ro.selectionStart-delTxtLen > max ) {
                    break;
                }
                
                // make up some rectangles base on the text range so we can do
                // easy intersection calculations
                Rectangle fragR = new Rectangle(min, 0, (max - min), 1);
                Rectangle revR = new Rectangle(ro.selectionStart-delTxtLen, 0, (ro.selectionEnd - ro.selectionStart), 1);

                // within fragment range...
                if ( revR.intersects(fragR)) {
                     
                    // only thing necessary to do here is REMOVE text. Two scenarios for this:
                    // Rev is accepted and type is delete OR rev is NOT accepted and its an ADD.
                    if ( (revised == true  && ro.type.equals(Type.DELETE)) || 
                         (revised == false && ro.type.equals(Type.ADD)) ) {               
      
                        Rectangle intersectR = revR.intersection(fragR);
                        int delStart = intersectR.x - min;
                        delStart = Math.max(0, delStart);
                        int delEnd = delStart + intersectR.width;
                        delEnd = Math.min(delEnd, this.view.getDocument().getLength());
                        int len = (delEnd - delStart);
                        delTxtLen += len;
                        max  -= len;
                        frag = frag.substring(0, delStart) + frag.substring(delEnd);
                    }
                }
            }
            return "..."+frag.trim()+"...";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // These are all NO-OP
    public void mouseEntered(MouseEvent event) {}
    public void mouseExited(MouseEvent event) {}
    public void mousePressed(MouseEvent event) {}
    public void mouseClicked(MouseEvent event) {}
    public void mouseDragged(MouseEvent event) {}
    
    /**
     * Track data about the currently selected revision
     */
    private static class RevisionOffset {
        int selectionStart;
        int selectionEnd;
        Revision.Type type;
        boolean contains(int offset ) {
            return ( offset >= this.selectionStart && offset <= this.selectionEnd);
        }
        @Override
        public String toString() {
            return type.toString()+" Start: "+selectionStart+", End: "+selectionEnd;
        }
    }
}
