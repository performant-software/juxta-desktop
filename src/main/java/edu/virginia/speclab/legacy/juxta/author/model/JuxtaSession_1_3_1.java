/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Educational Community License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.opensource.org/licenses/ecl1.txt">
 * http://www.opensource.org/licenses/ecl1.txt.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2006 by 
 * The Rector and Visitors of the University of Virginia. 
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.virginia.speclab.legacy.juxta.author.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.collation.Collation;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.legacy.juxta.author.model.manifest.DocumentManifestXMLFile;

/**
 * This is the top level model object for Juxta. It houses all serializable data related to 
 * a particular user session. It also tracks certain transient aspects of the user session.
 * 
 * @author Nick
 *
 */
public class JuxtaSession_1_3_1
{
    private boolean modified;
    private File saveFile;
    
    private boolean lineated;
    private Collation currentCollation;
    private HashSet currentCollationFilter;
    
    private ComparisonSet comparisonSet;    
    private DocumentManager documentManager;
	private AnnotationManager annotationManager;
	    
    private LinkedList listeners;
    
    public static final String DEFAULT_BASE_PATH = System.getProperty("user.dir")+File.separator+"sample";
    public static final String JUXTA_FILE_EXTENSION = "jxt";
    
    private JuxtaSession_1_3_1( ComparisonSet comparisonSet, DocumentManager documentManager )
    {
        this.comparisonSet = comparisonSet;
        this.documentManager = documentManager;
        listeners = new LinkedList();
        currentCollationFilter = new HashSet();
		annotationManager = new AnnotationManager(this);
		this.documentManager.getMovesManager().setSession(this);

    }
    
    public void addListener( JuxtaSessionListener listener )
    {
        listeners.add(listener);
    }
    
    public void removeListener( JuxtaSessionListener listener )
    {
        listeners.remove(listener);
    }
    
    /**
     * Creates a new <code>JuxtaSession</code> object which loads from the specified file. 
     * If no file is given, a new session will be created. 
     * @param file A <code>File</code> object that points to the saved juxta session or null.
     * @return A <code>JuxtaSession</code> object.
     * @throws ReportedException If we are unable to load the file and/or create a new session.
     */
    public static JuxtaSession_1_3_1 createSession( File file ) throws ReportedException 
    {
        DocumentManager documentManager = new DocumentManager(file);        
        ComparisonSet comparisonSet = new ComparisonSet(documentManager,true);
        documentManager.getMovesManager().addListener(comparisonSet);
        
        TokenizerSettings settings = documentManager.getStoredTokenizerSettings();
        
        if( settings != null )
        {
            comparisonSet.setTokenizerSettings(settings);
        }
        
        JuxtaSession_1_3_1 session = new JuxtaSession_1_3_1(comparisonSet,documentManager);
        session.setSaveFile(file);
        return session;
    }
    
    /**
     * Launches asynchronous collation of all documents in the comparison set that 
     * have not been collated and calls back to the provided listener when loading is complete. 
     * @param listener Listener for the completion of loading.
     * @throws ReportedException If we encounter an error loading the data. 
     */
    public void startSession( LoaderCallBack listener ) throws ReportedException 
    {
        // get the list of documents that need to be collated
        LinkedList documentList = documentManager.getUncollatedDocuments();
        comparisonSet.addLoaderCallBack( listener );
        comparisonSet.startLoader( documentList );
    }
    
    /**
     * Obtain the <code>ComparisonSet</code> object for this session.
     * @return the <code>ComparisonSet</code> object for this session.
     */
    public ComparisonSet getComparisonSet()
    {
        return comparisonSet;
    }

    /**
     * Serializes this session to the specified file.
     * @param file A valid <code>File</code> object. 
     * @throws ReportedException If there is an error trying to write the file.
     */
    public void saveSession( File file ) throws ReportedException, LoggedException
    {
        if( file == null ) throw new ReportedException("Error writing juxta session file.","Error writing juxta session file, file is null.");
		
        try
        {
            // save the manifest file to the temp dir
            DocumentManifestXMLFile juxtaFile = new DocumentManifestXMLFile(this);        
            juxtaFile.save();

			// record the current save file location
			this.saveFile = file;			
			
			// save to data file
			documentManager.save(file);
            
			// mark the session as up to date
			this.modified = false;
        }
        catch( IOException e )
        {
            throw new ReportedException(e, "Error saving juxta session.");
        }
    }

    private void fireCurrentCollationChanged( Collation collation )
    {
        for( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            JuxtaSessionListener listener = (JuxtaSessionListener) i.next();
            listener.currentCollationChanged(collation);            
        }
    }
    
    private void fireDocumentAdded( JuxtaDocument document )
    {
        for( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            JuxtaSessionListener listener = (JuxtaSessionListener) i.next();
            listener.documentAdded(document);            
        }
    }
    
    private void fireSessionModified()
    {
        for( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            JuxtaSessionListener listener = (JuxtaSessionListener) i.next();
            listener.sessionModified();            
        }
    }
    
    public void addDocument( JuxtaDocument document, int fragmentStart, int fragmentLength, LoaderCallBack callBack ) throws ReportedException
    {
    	// This adds the portion of the document that is currently selected in the File View.
		DocumentSetLoader loader = new DocumentSetLoader(new File(document.getFileName()),callBack, fragmentStart, fragmentLength, document.getEncoding());
		loader.start();	
    }

    /**
     * Adds the specified document to the session, collating it in the process.
     * @param document A Juxta document that is not part of the comparison set.
     * @return A <code>JuxtaDocument</code> object that is part of the comparison set.
     * @throws ReportedException If an error is encountered loading the associated file.
     */
    public void addDocument( JuxtaDocument document, LoaderCallBack callBack ) throws ReportedException 
    {
		DocumentSetLoader loader = new DocumentSetLoader(new File(document.getFileName()),callBack,document.getEncoding());
		loader.start();	
    }
	
	private JuxtaDocument addDocument( String documentName, String fileName, String encoding ) throws ReportedException
    {     
        JuxtaDocument document = documentManager.addDocument(documentName,fileName,encoding);
        if( document != null )
        {
            comparisonSet.addCollation(document);            
            fireDocumentAdded(document);
        }            
        
        markAsModified();
        return document;
    }
	
	private JuxtaDocument addDocument( String documentName, String fileName, int fragmentStart, int fragmentLength, String encoding ) throws ReportedException
    {     
        JuxtaDocument document = documentManager.addDocumentFragment(documentName, fileName, fragmentStart, fragmentLength, encoding);
        if( document != null )
        {
            comparisonSet.addCollation(document);            
            fireDocumentAdded(document);
        }            
        
        markAsModified();
        return document;
    }
	
	public void addDocuments( File files[], LoaderCallBack callBack, String fileEncoding )
	{
		DocumentSetLoader loader = new DocumentSetLoader(files,callBack,fileEncoding);
		loader.start();
	}
    
    /**
     * Sets the specified document to be the current base text for display purposes. If 
     * the document has not yet been collated as the base text, collate it first then return.
     * @param document The <code>DocumentMode</code> of the new base text.
     * @return <code>true</code> if a collation already existed for this document, <code>false</code> otherwise.  
     * @throws ReportedException If there is an error collating this document.
     */
    public boolean setBaseText( JuxtaDocument document ) throws ReportedException
    {
        if( document == null )
        {
            this.currentCollation = null;
        }
        else
        {
            Collation collation = comparisonSet.getCollation(document);
            
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
    public DocumentManager getDocumentManager()
    {
        return documentManager;
    }

    /**
     * Obtain the collation of the current base text for this session.
     * @return
     */
    public Collation getCurrentCollation()
    {
        return currentCollation;
    }

    public boolean isLineated()
    {
        return lineated;
    }    

    public void setLineated(boolean lineated)
    {
        this.lineated = lineated;
    }

    /**
     * Updates the list of the set of documents to filter out when returning collation results.
     * These documents are temporarily not included in the difference results or in the histogram data.
     * @param collationFilter A <code>HashSet</code> of <code>JuxtaDocument</code> objects.
     */
    public void setCurrentCollationFilter(HashSet collationFilter)
    {
        this.currentCollationFilter = new HashSet(collationFilter);
        
        if( currentCollation != null )
        {
            currentCollation.setCollationFilter(currentCollationFilter);
            fireCurrentCollationFilterChanged(currentCollation);
        }
    }

    private void fireCurrentCollationFilterChanged(Collation collation) 
    {
    	  for( Iterator i = listeners.iterator(); i.hasNext(); )
          {
              JuxtaSessionListener listener = (JuxtaSessionListener) i.next();
              listener.currentCollationFilterChanged(collation);            
          }
	}

	/**
     * Removes the specified document from the comparison set.
     * @param document A <code>JuxtaDocument</code> object.
     * @throws ReportedException If there is an error removing the document or its associated cache file.
     */
    public void removeDocument(JuxtaDocument document) throws ReportedException
    {
		annotationManager.removeAnnotations(document);
        comparisonSet.removeCollation(document);		
        markAsModified();
    }

    /**
     * Get the path to the base directory used to resolve relative paths encountered in juxta 
     * data files.
     * @return A <code>File</code> object pointing to the base directory.
     */
    public File getBasePath()
    {
        return documentManager.getBasePath();
    }

    /**
     * 
     * @return <code>true</code> if the session has been modified since it was last saved, <code>false</code> otherwise.
     */
    public boolean isModified()
    {
        return modified;
    }

    /**
     * Obtain the current save file for this session if there is one. 
     * @return A <code>File</code> pointing to the current save file or null if there isn't one.
     */
    public File getSaveFile()
    {
        return saveFile;
    }

    /**
     * Set the current save file for this session. 
     * @param saveFile A <code>File</code> object.
     */
    public void setSaveFile(File saveFile)
    {
        this.saveFile = saveFile;
    }

    /**
     * Mark this session as having been modified since the last save point.     *
     */
    public void markAsModified()
    {
        this.modified = true;
        fireSessionModified();
    }

    /**
     * Frees system resources associated with this object.
     */
    public void close() throws ReportedException
    {
        // shut down loader
        if( comparisonSet != null )
            comparisonSet.stopLoader(); 
        
    }

    /**
     * Regenerates the collation cache files for the associated comparison set.
     * @throws LoggedException If there is a problem regenerating the cache files.
     */
	public void refreshComparisonSet() throws LoggedException 
	{
	    // shut down loader
        if( comparisonSet != null )
        {            
			comparisonSet.reset();            
        }		
	}
    
	private class DocumentSetLoader extends Thread
	{
		private File[] files;
		private LoaderCallBack callBack;
        private String fileEncoding;
		private int fragmentStart;
		private int fragmentLength;
		private boolean fragment;
		
		public DocumentSetLoader( File files[], LoaderCallBack callBack, String fileEncoding )
		{
			super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
			this.callBack = callBack;
			this.files = files;			
		}
		
		public DocumentSetLoader( File file, LoaderCallBack callBack, String fileEncoding )
		{
			super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
			this.callBack = callBack;			
			this.files = new File[1];
			this.files[0] = file;
		}
		
		public DocumentSetLoader( File file, LoaderCallBack callBack, int fragmentStart, int fragmentLength, String fileEncoding )
		{
			super("DocumentSetLoader");
            this.fileEncoding = fileEncoding;
			this.callBack = callBack;			
			this.files = new File[1];
			this.files[0] = file;
			this.fragmentStart = fragmentStart;
			this.fragmentLength = fragmentLength;
			this.fragment = true;
		}

		public void run() 
		{	
			if( files == null ) return;
				
            for( int i=0; i < files.length; i++ )
            {
                try
                {
                    String fileName = files[i].getName();
                    String path = files[i].getAbsolutePath();
                    if( fragment ) {
                    	addDocument(fileName,path,fragmentStart,fragmentLength,fileEncoding);
                    }
                    else {
                    	addDocument(fileName,path,fileEncoding);                    	
                    }
                } 
                catch (LoggedException e)
                {
                    ErrorHandler.handleException(e);
                }                
            }          
			
			if( callBack != null ) callBack.loadingComplete();
		}
	}

	public AnnotationManager getAnnotationManager() {
		return annotationManager;
	}

    
}
