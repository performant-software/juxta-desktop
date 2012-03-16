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

package edu.virginia.speclab.juxta.author.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.Ostermiller.util.StringTokenizer;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;

public class JuxtaDocument extends DocumentModel {
    
    private BiblioData biblioData;
    private List<JuxtaDocumentListener> listeners;
    private String parseTemplateGuid;
    private List<Integer> acceptedRevisions;

    public JuxtaDocument(DocumentModel document, BiblioData biblioData) {
        super(document);
        this.parseTemplateGuid = "";
        this.acceptedRevisions = new ArrayList<Integer>();
        this.listeners = new LinkedList<JuxtaDocumentListener>();
        this.biblioData = biblioData;
        if (this.biblioData == null) {
            this.biblioData = BiblioData.createNew();
        }
    }
    
    /**
     * Set the list of accepted revisions as string containing
     * a comma separated list of revision indexes. 
     *  
     * @param revisonsList
     */
    public void setAcceptedRevisions( final String revisonsList ) {
        this.acceptedRevisions.clear();
        StringTokenizer st = new StringTokenizer(revisonsList, ",");
        while (st.hasMoreTokens() ) {
            this.acceptedRevisions.add( Integer.parseInt(st.nextToken()));
        }
        Collections.sort( this.acceptedRevisions );
    }
    
    /**
     * Set the list of accepted revision indexes
     *  
     * @param revisonsList
     */
    public void setAcceptedRevisions( final List<Integer> revisonIndexList ) {
        this.acceptedRevisions.clear();
        this.acceptedRevisions.addAll(revisonIndexList);
        Collections.sort( this.acceptedRevisions );
    }
    
    /**
     * Get a list of all revsions that have been accepted for this
     * version of the document
     * @return
     */
    public List<Revision> getAcceptedRevisions() {
        List<Revision> revs = new ArrayList<Revision>();
        for ( Integer idx : this.acceptedRevisions )  {
            revs.add( this.revisions.get( idx ));
        }
        return revs;
    }
    
    /**
     * Get a list of all revsion indexes that have been accepted into
     * this document.
     * @return
     */
    public List<Integer> getAcceptedRevisionIndexes() {
        return Collections.unmodifiableList( this.acceptedRevisions );
    }
    
    /**
     * Get the accepted revisions as a comma separated string
     * @return
     */
    public String getAcceptedRevisionsString() {
        StringBuilder sb = new StringBuilder();
        for (Integer idx : this.acceptedRevisions ) {
            if ( sb.length() > 0 ) {
                sb.append(",");
            }
            sb.append(idx.toString());
        }
        return sb.toString();
    }
    
    /**
     * Check if this document has accepted any revisions
     */
    public final boolean hasAcceptedRevisions() {
        return (this.acceptedRevisions.size()>0);
    }
    
    /**
     * Check if there are any revisions availble for this document
     */
    public final boolean hasRevisions() {
        return (this.revisions.size() > 0);
    }

    public void setParseTemplateGuid(final String guid) {
        this.parseTemplateGuid = guid;
    }

    public final String getParseTemplateGuid() {
        return parseTemplateGuid;
    }

    public void addJuxtaDocumentListener(JuxtaDocumentListener listener) {
        listeners.add(listener);
    }

    public void removeJuxtaDocumentListener(JuxtaDocumentListener listener) {
        listeners.remove(listener);
    }

    private void fireDocumentNameChanged(String name) {
        for (JuxtaDocumentListener listener : this.listeners ) {
            listener.nameChanged(name);
        }
    }

    public JuxtaDocument(JuxtaDocument document, String docName) {
        super(document);
        this.listeners = new LinkedList<JuxtaDocumentListener>();
        this.biblioData = document.biblioData;
        setDocumentName(docName);
    }

    public JuxtaDocument(JuxtaDocument document, String docName, String filename) {
        super(document);
        this.listeners = new LinkedList<JuxtaDocumentListener>();
        this.biblioData = document.biblioData;
        this.fileName = filename;
        setDocumentName(docName);
    }

    public JuxtaDocument(JuxtaDocument document, int fragmentStart, int fragmentEnd) {
        this(document, document.ID, fragmentStart, fragmentEnd);
    }

    public JuxtaDocument(JuxtaDocument document, int documentID, int fragmentStart, int fragmentEnd) {
        super(document, fragmentStart, fragmentEnd);
        this.ID = documentID;
        this.listeners = new LinkedList<JuxtaDocumentListener>();
        this.biblioData = document.biblioData;
    }

    public BiblioData getBiblioData() {
        return biblioData;
    }

    public void setBiblioData(BiblioData biblioData) {
        if (biblioData != null) {
            if (this.biblioData != null) {
                if (!this.biblioData.getShortTitle().equals(biblioData.getShortTitle()))
                    fireDocumentNameChanged(biblioData.getShortTitle());
            }

            if (this.biblioData == null) {
                fireDocumentNameChanged(biblioData.getShortTitle());
            }
        }

        this.biblioData = biblioData;
    }

    public Image getImageAt(int centerOffset) {
        LocationMarker locationMarker = null;
        for ( LocationMarker nextMarker : this.locationMarkerList ) {
           if (nextMarker.hasImage()) {
                int startOffset = nextMarker.getStartOffset(OffsetRange.Space.ACTIVE);
                int endOffset = nextMarker.getEndOffset(OffsetRange.Space.ACTIVE);
                if (centerOffset >= startOffset && centerOffset < endOffset) {
                    locationMarker = nextMarker;
                    break;
                }
            }
        }

        if (locationMarker != null) {
            return locationMarker.getImage();
        } else
            return null;
    }

    @Override
    public String getDocumentName() {
        return biblioData.getShortTitle();
    }

    public void setDocumentName(String name) {
        BiblioData newData = new BiblioData(biblioData.getTitle(), name, biblioData.getAuthor(),
            biblioData.getEditor(), biblioData.getSource(), biblioData.getDate(), biblioData.getNotes(),
            biblioData.getSortDate());
        this.biblioData = newData;

        fireDocumentNameChanged(name);
    }

    @Override
    public String toString() {
        return getDocumentName();
    }

    public boolean containsOnlyWhitespace(int start, int end) {
        String s = this.getDocumentText().substring(start, end);
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i)))
                return false;
        }
        return true;
    }
}
