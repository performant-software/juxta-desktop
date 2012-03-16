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

package edu.virginia.speclab.juxta.author.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.manifest.DocumentManifestXMLFile;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;

/**
 * This is the top level model object for Juxta. It houses all serializable data related to 
 * a particular user session. It also tracks certain transient aspects of the user session.
 * 
 * @author Nick
 *
 */
public class JuxtaSession {
    private boolean modified;
    private boolean exported;
    private File saveFile;

    private boolean lineated;
    private Collation currentCollation;
    private HashSet<JuxtaDocument> currentCollationFilter;

    private ComparisonSet comparisonSet;
    private DocumentManager documentManager;
    private AnnotationManager annotationManager;
    private LinkedList<JuxtaSessionListener> listeners;

    public static final String DEFAULT_BASE_PATH = System.getProperty("user.dir") + File.separator + "sample";
    public static final String JUXTA_FILE_EXTENSION = "jxt";

    private JuxtaSession(ComparisonSet comparisonSet, DocumentManager documentManager) {
        this.comparisonSet = comparisonSet;
        this.documentManager = documentManager;
        listeners = new LinkedList<JuxtaSessionListener>();
        currentCollationFilter = new HashSet<JuxtaDocument>();
        annotationManager = new AnnotationManager(this);
        this.documentManager.getMovesManager().setSession(this);
        this.exported = false;
    }
    
    public void setExported( boolean export ) {
        // can't unexport somethng thats already been exported
        if ( this.exported == false ) {
            this.exported = export;
        }
    }
    public boolean wasExported() {
        return this.exported;
    }
    

    public void addListener(JuxtaSessionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JuxtaSessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Creates a new <code>JuxtaSession</code> object which loads from the specified file. 
     * If no file is given, a new session will be created. 
     * @param file A <code>File</code> object that points to the saved juxta session or null.
     * @return A <code>JuxtaSession</code> object.
     * @throws LoggedException 
     */
    public static JuxtaSession createSession(File file, JuxtaAuthorFrame frame, boolean convertLegacyMoves)
        throws LoggedException {
        DocumentManager documentManager = new DocumentManager(file);
        DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
        documentManager.loadManifest();

        // if this is an older version, convert move data
        if (convertLegacyMoves && LegacyMoveDataConverter.isLegacyVersion(documentManager.getJuxtaVersion())) {
            return LegacyMoveDataConverter.convertMoveData(file, frame);
        } else {
            ComparisonSet comparisonSet = new ComparisonSet(documentManager, true);
            documentManager.getMovesManager().addListener(comparisonSet);

            TokenizerSettings settings = documentManager.getStoredTokenizerSettings();

            if (settings != null) {
                comparisonSet.setTokenizerSettings(settings);
            }
            
            for ( JuxtaDocument foo:documentManager.getDocumentList()) {
                int maxSize = foo.getSourceDocument().getOffsetMap().getSize();
                int rangeEnd = foo.getActiveTextRange().getEndOffset(Space.ORIGINAL);
                int rangeStart = foo.getActiveTextRange().getStartOffset(Space.ORIGINAL);
                if ( rangeEnd > maxSize ) {
                    System.err.println("INVALID DOCUMENT END RANGE. ADJUSTING!");
                    foo.getActiveTextRange().set(rangeStart, maxSize, Space.ORIGINAL);
                }
            }

            JuxtaSession session = new JuxtaSession(comparisonSet, documentManager);
            session.setSaveFile(file);

            // convert any move markers found to moves.
            LegacyMoveDataConverter.convertMoveMarkers(session);

            return session;
        }
    }

    /**
     * Launches asynchronous collation of all documents in the comparison set that 
     * have not been collated and calls back to the provided listener when loading is complete. 
     * @param listener Listener for the completion of loading.
     * @throws ReportedException If we encounter an error loading the data. 
     */
    @SuppressWarnings("rawtypes")
    public void startSession(LoaderCallBack listener) throws ReportedException {
        // get the list of documents that need to be collated
        LinkedList documentList = documentManager.getUncollatedDocuments();
        comparisonSet.addLoaderCallBack(listener);
        comparisonSet.startLoader(documentList);
    }

    /**
     * Obtain the <code>ComparisonSet</code> object for this session.
     * @return the <code>ComparisonSet</code> object for this session.
     */
    public ComparisonSet getComparisonSet() {
        return comparisonSet;
    }

    /**
     * Serializes this session to the specified file.
     * @param file A valid <code>File</code> object. 
     * @throws ReportedException If there is an error trying to write the file.
     */
    public void saveSession(File file) throws ReportedException, LoggedException {
        if (file == null) {
            throw new ReportedException("Error writing juxta session file.",
                "Error writing juxta session file, file is null.");
        }

        try {
            // save the manifest file to the temp dir
            DocumentManifestXMLFile juxtaFile = new DocumentManifestXMLFile(this);
            juxtaFile.save();

            // save to data file
            this.documentManager.save(file, true);
            this.saveFile = file;
            this.modified = false;

            
        } catch (IOException e) {
            throw new ReportedException(e, "Error saving juxta session.");
        }
    }
    
    public void saveSessionForExport(File file) throws ReportedException, LoggedException {
        try {
            // save the manifest file to the temp dir
            DocumentManifestXMLFile juxtaFile = new DocumentManifestXMLFile(this);
            juxtaFile.save();

            // save to data file
            this.documentManager.save(file, false);
            
        } catch (IOException e) {
            throw new ReportedException(e, "Error saving juxta session for export.");
        }
    }

    private void fireCurrentCollationChanged(Collation collation) {
        for (JuxtaSessionListener listener : this.listeners ) {
            listener.currentCollationChanged(collation);
        }
    }

    private void fireDocumentAdded(JuxtaDocument document) {
        for (JuxtaSessionListener listener : this.listeners ) {
            listener.documentAdded(document);
        }
    }

    private void fireSessionModified() {
        for (JuxtaSessionListener listener : this.listeners ) {
            listener.sessionModified();
        }
    }

    public void addDocument(JuxtaDocument document, int fragmentStart, int fragmentLength, LoaderCallBack callBack)
        throws ReportedException {
        // This adds the portion of the document that is currently selected in the File View.
        DocumentSetLoader loader = new DocumentSetLoader(new File(document.getFileName()), callBack, fragmentStart,
            fragmentLength, document.getEncoding());
        loader.start();
    }

    /**
     * Adds the specified document to the session, collating it in the process.
     * @param document A Juxta document that is not part of the comparison set.
     * @return A <code>JuxtaDocument</code> object that is part of the comparison set.
     * @throws ReportedException If an error is encountered loading the associated file.
     */
    public void addDocument(JuxtaDocument document, LoaderCallBack callBack) throws ReportedException {
        DocumentSetLoader loader = new DocumentSetLoader(new File(document.getFileName()), callBack,
            document.getEncoding());
        loader.start();
    }

    public void addExistingDocument(JuxtaDocument document, LoaderCallBack callBack) throws ReportedException {
        documentManager.addExistingDocument(document);
        new ExistingDocumentCollateLoader(document, callBack).start();
    }

    private class ExistingDocumentCollateLoader extends Thread {
        JuxtaDocument _document;
        LoaderCallBack _callback;

        public ExistingDocumentCollateLoader(JuxtaDocument document, LoaderCallBack callBack) {
            _document = document;
            _callback = callBack;
        }

        @Override
        public void run() {
            if (_document != null) {
                try {
                    comparisonSet.addCollation(_document);
                    fireDocumentAdded(_document);
                } catch (ReportedException ex) {

                }
            }

            if (_callback != null) {
                _callback.loadingComplete();
            }
            markAsModified();
        }

    }

    private JuxtaDocument addDocument(String documentName, String fileName, String encoding) throws ReportedException {
        JuxtaDocument document = documentManager.addDocument(documentName, fileName, encoding);
        if (document != null) {
            comparisonSet.addCollation(document);
            fireDocumentAdded(document);
        }

        markAsModified();
        return document;
    }

    private JuxtaDocument addDocument(String documentName, String fileName, int fragmentStart, int fragmentLength,
        String encoding) throws ReportedException {
        JuxtaDocument document = documentManager.addDocumentFragment(documentName, fileName, fragmentStart,
            fragmentLength, encoding);
        if (document != null) {
            comparisonSet.addCollation(document);
            fireDocumentAdded(document);
        }

        markAsModified();
        return document;
    }

    public void addDocuments(File files[], LoaderCallBack callBack, String fileEncoding) {
        DocumentSetLoader loader = new DocumentSetLoader(files, callBack, fileEncoding);
        loader.start();
    }

    /**
     * Sets the specified document to be the current base text for display purposes. If 
     * the document has not yet been collated as the base text, collate it first then return.
     * @param document The <code>DocumentMode</code> of the new base text.
     * @return <code>true</code> if a collation already existed for this document, <code>false</code> otherwise.  
     * @throws ReportedException If there is an error collating this document.
     */
    public boolean setBaseText(JuxtaDocument document) throws ReportedException {
        if (document == null) {
            this.currentCollation = null;
        } else {
            Collation collation = this.comparisonSet.getCollation(document);

            if (collation == null)
                return false;

            // make this the current collation
            this.currentCollation = collation;
            currentCollation.setCollationFilter(currentCollationFilter);
            fireCurrentCollationChanged(currentCollation);
        }

        return true;
    }

    /**
     * Obtain the <code>DocumentManager</code> object for this session.
     * @return
     */
    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    /**
     * Obtain the collation of the current base text for this session.
     * @return
     */
    public Collation getCurrentCollation() {
        return currentCollation;
    }

    public void resetCurrentCollation() {
        this.currentCollation = null;
    }

    public boolean isLineated() {
        return lineated;
    }

    public void setLineated(boolean lineated) {
        this.lineated = lineated;
    }

    /**
     * Updates the list of the set of documents to filter out when returning collation results.
     * These documents are temporarily not included in the difference results or in the histogram data.
     * @param collationFilter A <code>HashSet</code> of <code>JuxtaDocument</code> objects.
     */
    public void setCurrentCollationFilter(HashSet<JuxtaDocument> collationFilter) {
        this.currentCollationFilter = new HashSet<JuxtaDocument>(collationFilter);

        if (currentCollation != null) {
            currentCollation.setCollationFilter(currentCollationFilter);
            fireCurrentCollationFilterChanged(currentCollation);
        }
    }

    /**
     * Check if the specified <code>JuxtaDocument</code> is specified in the
     * current filter set. Return true if it is.
     * 
     * @param doc The document in question
     * @return True if the document is filtered, false otherwise
     */
    public final boolean isFiltered(final JuxtaDocument doc) {
        return this.currentCollationFilter.contains(doc);
    }

    private void fireCurrentCollationFilterChanged(Collation collation) {
        for (Iterator<JuxtaSessionListener> i = listeners.iterator(); i.hasNext();) {
            JuxtaSessionListener listener = i.next();
            listener.currentCollationFilterChanged(collation);
        }
    }

    /**
     * Removes the specified document from the comparison set.
     * @param document A <code>JuxtaDocument</code> object.
     * @throws ReportedException If there is an error removing the document or its associated cache file.
     */
    public void removeDocument(JuxtaDocument document) throws ReportedException {
        annotationManager.removeAnnotations(document);
        comparisonSet.removeCollation(document);
        markAsModified();
    }

    /**
     * Get the path to the base directory used to resolve relative paths encountered in juxta 
     * data files.
     * @return A <code>File</code> object pointing to the base directory.
     */
    public File getBasePath() {
        return documentManager.getBasePath();
    }

    /**
     * 
     * @return <code>true</code> if the session has been modified since it was last saved, <code>false</code> otherwise.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Obtain the current save file for this session if there is one. 
     * @return A <code>File</code> pointing to the current save file or null if there isn't one.
     */
    public File getSaveFile() {
        return saveFile;
    }

    /**
     * Set the current save file for this session. 
     * @param saveFile A <code>File</code> object.
     */
    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    /**
     * Mark this session as having been modified since the last save point.     *
     */
    public void markAsModified() {
        this.modified = true;
        this.exported = false;
        fireSessionModified();
    }

    /**
     * Frees system resources associated with this object.
     */
    public void close() throws ReportedException {
        // shut down loader
        if (comparisonSet != null)
            comparisonSet.stopLoader();

    }

    /**
     * Regenerates the collation cache files for the associated comparison set.
     * @throws LoggedException If there is a problem regenerating the cache files.
     */
    public void refreshComparisonSet() throws LoggedException {
        // shut down loader
        if (comparisonSet != null) {
            comparisonSet.reset();
        }
    }

    /**
     * Call when parse template <code>template</code> has changed. All documents
     * that use it will be reparsed.
     * 
     * @param template
     * @throws ReportedException 
     */
    public void reparseDocuments(ParseTemplate template) throws ReportedException {
        for (JuxtaDocument doc : getDocumentManager().getDocumentList()) {
            if (doc.getParseTemplateGuid().equals(template.getGuid())) {
                JuxtaDocumentFactory factory = new JuxtaDocumentFactory(doc.getEncoding());
                factory.reparseDocument(doc, template);
            }
        }
    }

    private class DocumentSetLoader extends Thread {
        private File[] files;
        private LoaderCallBack callBack;
        private String fileEncoding;
        private int fragmentStart;
        private int fragmentLength;
        private boolean fragment;

        public DocumentSetLoader(File files[], LoaderCallBack callBack, String fileEncoding) {
            super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
            this.callBack = callBack;
            this.files = files;
        }

        public DocumentSetLoader(File file, LoaderCallBack callBack, String fileEncoding) {
            super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
            this.callBack = callBack;
            this.files = new File[1];
            this.files[0] = file;
        }

        public DocumentSetLoader(File file, LoaderCallBack callBack, int fragmentStart, int fragmentLength,
            String fileEncoding) {
            super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
            this.callBack = callBack;
            this.files = new File[1];
            this.files[0] = file;
            this.fragmentStart = fragmentStart;
            this.fragmentLength = fragmentLength;
            this.fragment = true;
        }

        public void run() {
            if (files == null)
                return;

            for (int i = 0; i < files.length; i++) {
                try {
                    String fileName = files[i].getName();
                    String path = files[i].getAbsolutePath();
                    if (fragment) {
                        addDocument(fileName, path, fragmentStart, fragmentLength, fileEncoding);
                    } else {
                        addDocument(fileName, path, fileEncoding);
                    }
                } catch (LoggedException e) {
                    ErrorHandler.handleException(e);
                }
            }

            if (callBack != null)
                callBack.loadingComplete();
        }
    }

    public AnnotationManager getAnnotationManager() {
        return annotationManager;
    }

}
