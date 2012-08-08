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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.util.EncodingUtils;
import edu.virginia.speclab.util.FileUtilities;

/**
 * Top level object responsible for loading and saving documents from disk and serializing and 
 * de-serializing collation cache files. Houses a <code>JuxtaSessionFile</code> and the loaded 
 * <code>JuxtaDocument</code> objects.
 * 
 * @author Nick
 *
 */
public class DocumentManager implements Serializable
{
    private static final long MAX_FILE_SIZE = 1000000;  // 1M
    
	private JuxtaSessionFile sessionFile;
    private LinkedList<JuxtaDocument> documentList;
	private MovesManager movesManager;
    private TemplateConfig templateConfig;
    
    /**
     * Constructs the document manager from the specified save file, loading the documents 
     * found therein.
     * @param sessionDataFile A <code>File</code> object pointing to the session save file.
     * @throws ReportedException If there is an error loading the session data file.
     */
    public DocumentManager( File sessionDataFile ) throws ReportedException
    {
        this.sessionFile = new JuxtaSessionFile(sessionDataFile);
    }
	

    public void loadManifest() throws ReportedException
    {
        this.sessionFile.loadDocumentManifest();
        this.templateConfig = this.sessionFile.loadTemplateConfiguration();
        TemplateConfigManager.getInstance().setSessionConfig( this.templateConfig);
        
        this.documentList = sessionFile.loadDocumentList();
        this.sessionFile.loadAnnotations();      
        
     	movesManager = new MovesManager(this);
		// if the are existing moves, load them in.
		movesManager.load(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY);
    }

    public String getJuxtaVersion() {
    	return sessionFile.getJuxtaVersion();
    }
        
    public TokenizerSettings getStoredTokenizerSettings() {
        if (sessionFile != null) {
            return sessionFile.getStoredTokenizerSettings();
        }
        return null;
    }
    
    public List getStoredAnnotationList() {
        if (sessionFile != null) {
            return sessionFile.getAnnotationList();
        }
        return null;
    }
    
    /**
     * Serializes the specified collation of the supplied document to disk.
     * @param document The base text of the suppplied collation.
     * @param collation The collation to cache.
     * @throws ReportedException If there is an error during serialization.
     */
    public void cacheCollation( JuxtaDocument document, Collation collation ) throws ReportedException 
    {        
        sessionFile.cacheCollation(document,collation);
    }
    
    /**
     * Loads the collation cache for which the supplied document is the base document.      
     * @param doc The <code>JuxtaDocument</code> object of the base document.
     * @return A <code>Collation</code> object if sucessfull, or null if the cache is 
     * not found, out of date, or corrupted.
     */
    public Collation loadCollation( JuxtaDocument doc )
    {
        return sessionFile.loadCollation(doc.getID());
    }

    /**
     * Determines is a collation cache exists for a given document.
     * @param baseDocument A <code>JuxtaDocument</code> object.
     * @return <code>true</code> if a collation cache is found, <code>false</code> otherwise.
     */
    public boolean collationCacheExists( JuxtaDocument baseDocument )
    {        
        HashSet collationList = sessionFile.getCachedDocumentIDList();
        if( collationList != null && collationList.contains(new Integer(baseDocument.getID())) ) return true;
        else return false;
    }
    
    /**
     * 
     * @param name
     * @param document
     * @return
     * @throws ReportedException
     */
    public JuxtaDocument addDocumentFragment( String documentName, String fileName, int fragmentStart, int fragmentLength) throws ReportedException {
        JuxtaDocument doc = addDocument(documentName, fileName);
        doc.setActiveRange(new OffsetRange(doc, fragmentStart, fragmentStart + fragmentLength, OffsetRange.Space.ACTIVE));
        return doc;
    }
    

    /**
     * Adds a document to the document manager. This does not collate the document.
     * @param name The display name of the document.
     * @param file The path to the document file. 
     * @param tokensCached Optionally cache the tokenized document data.
     * @return A <code>JuxtaDocument</code> object for the specified file.
     * @throws ReportedException If there is an error loading the file.
     */
    public JuxtaDocument constructDocument( String name, String file) throws ReportedException
    {  
		File srcFile = new File(file);
		if( srcFile.length()>MAX_FILE_SIZE) {
            throw new ReportedException(new Exception(),"The size of the file \"" + srcFile.getName() +
                    "\" is " + Long.toString(srcFile.length())+ " bytes, which is too large to load.\nTry breaking the larger file into several files that are each smaller than " + Long.toString(MAX_FILE_SIZE)+ " bytes.");
        }
		
		// fix the encoding and leave results in temp file.
		File originalFile = null;
		try {
		    originalFile = EncodingUtils.fixEncoding(new FileInputStream(srcFile), file.endsWith("xml"));
        } catch (IOException e1) {
            throw new ReportedException(e1, "Unable to fix encoding of "+file);
        }
		
		

		File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" +
									JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY +
									srcFile.getName() );

        String targetWrapperFileName = JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + srcFile.getName();
        File targetWrapperFile;
		if( file.endsWith("xml") )
		{
            targetWrapperFile = new File(targetWrapperFileName);
        }
        else
        {
            targetWrapperFile = new File(targetWrapperFileName + ".xml");
        }

        // ensure that this is a unique filename in this dir
  		targetFile = getUniqueFileName( targetFile );
        // This too--this represents the "wrapper" doc that contains the bibliodata and a pointer to the source doc
        targetWrapperFile = getUniqueFileName( targetWrapperFile );

        try
        {
            // copy the source file to be where we want it right away, we'll just read it from there
            FileUtilities.copyFile(originalFile,targetFile,false);
        }
        catch (IOException e)
        {
            throw new ReportedException(e,"unable to copy file.");
        }

        JuxtaDocumentFactory factory = new JuxtaDocumentFactory();
        JuxtaDocument doc = factory.readFromFile(targetFile);
        
        // write it out to our final location--this produces two files
        factory.writeToFile(doc, targetWrapperFile);
        
        // make sure the image files are where we want them
        transferImageFiles( doc, originalFile );
        
        // re-read it to normalize document behavior
        doc = factory.readFromFile(targetWrapperFile);
            
        // if there is not a document name specified by the XML, use the name given
        if( doc.getDocumentName().equals("") ) {
            doc.setDocumentName(getUniqueDocumentName(name));
        } else {
            String uniqueName = getUniqueDocumentName(doc.getDocumentName());
            doc.setDocumentName(uniqueName);
        }

        return doc;
    }

    public JuxtaDocument addDocument( String name, String file ) throws ReportedException
    {
        JuxtaDocument doc = constructDocument(name, file);
        addExistingDocument(doc);
        return doc;
    }

    public void addExistingDocument(JuxtaDocument doc) {
        if (doc != null) {
            this.documentList.add(doc);
        }
    }
    
    private void transferImageFiles( JuxtaDocument oldDoc, File originalFile ) throws ReportedException {
        for( Iterator<Image> i = oldDoc.getImageList().iterator(); i.hasNext(); )
        {
            Image targetImage = (Image) i.next();
            
            File targetImageFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/docs/images/" + targetImage.getImageFile().getName() );
            File originalImageFile = new File( originalFile.getParentFile() + "/images/" + targetImageFile.getName() );                
                            
            try 
            {
                FileUtilities.copyFile(originalImageFile,targetImageFile,false);
            } 
            catch (IOException e) 
            {
                throw new ReportedException(e,"unable to import document image: "+targetImageFile.getName());
            }
        }
    }
 
    public String getUniqueDocumentName( String documentName ) {
    	return getUniqueDocumentName( documentName, 0 );
    }
    	 
    private String getUniqueDocumentName( String documentName, int count ) {
    	
    	String suffix = (count > 0) ? "-"+Integer.toString(count) : "";
    	String uniqueName = documentName + suffix;
    	
    	for( JuxtaDocument document : this.documentList) {
    		String name = document.getDocumentName(); 
    		if( name.equals(uniqueName) ) {
    			// this name is not unique, recurse to find a unique name
    			return getUniqueDocumentName( documentName, count+1 );
    		}
    	}
    	
    	return uniqueName;
    }
    
    public File getUniqueFileName( File file ) {
    	return getUniqueFileName( file, 0 );
    }
    	 
    private File getUniqueFileName( File file, int count ) {
    	
    	String filePath = file.getPath();
    	String suffix = (count > 0) ? "-"+Integer.toString(count) : "";
		String extension = FileUtilities.getFileExtension(filePath);
		String pathMinusExtension = FileUtilities.removeFileExtension(filePath);
		File uniqueFile = new File( pathMinusExtension + suffix + extension );

    	// recursively search for a unique filename in this directory
    	if( uniqueFile.exists() ) {
    		return getUniqueFileName( file, count + 1 );
    	}
    	
    	return uniqueFile;
    }
            
    /**
     * Remove the specified document from the document manager.
     * @param document
     */
    public void removeDocument( JuxtaDocument document )
    {
        movesManager.removeDocument(document.getID());
        documentList.remove(document);
        sessionFile.removeDocument(document);
    }

    /**
     * Save all of the documents to the disk.
     */
    private void saveDocuments() throws ReportedException
    {
        for( JuxtaDocument document : this.documentList) {
            this.sessionFile.saveDocument(document);            
        }
    }
	
    /**
     * Save the currrent juxta session to the specified file
     * @param file
     * @throws ReportedException
     */
    public void save(File file, boolean fullSave) throws ReportedException {
        saveDocuments();
        this.templateConfig.save();
        if ( fullSave == false ) {
            movesManager.saveForExport(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY);
        } else {
            movesManager.save(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY);
        }
        sessionFile.save(file, documentList, fullSave);
    }

    /**
     * Retrieve the <code>JuxtaDocument</code> for the specified document. 
     * @param id The ID of the specified document.
     * @return A <code>JuxtaDocument</code> object.
     */
    public JuxtaDocument lookupDocument(int id) {
        for (JuxtaDocument document : this.documentList) {
            if (document.getID() == id) {
                return document;
            }
        }

        return null;
    }
        
    private ArrayList searchDocument( JuxtaDocument document, String searchQuery ) throws ReportedException {
		return this.sessionFile.search( document.getDocumentText(), searchQuery);
    }

	public SearchResults search( String searchQuery ) throws ReportedException {
		
		SearchResults results = new SearchResults(searchQuery);
		
		for( JuxtaDocument document : this.documentList) {
			ArrayList phrases = searchDocument(document, searchQuery);
			results.addPhrases( document.getID(), phrases );
		}
		
		return results;
	}
	
	public File getBasePath()
    {
        return sessionFile.getSessionBaseDirectory();
    }

    /**
     * Get the list of documents in the system. 
     * @return A <code>LinkedList</code> of <code>JuxtaDocument</code> objects.
     */
    public LinkedList<JuxtaDocument> getDocumentList()
    {
        return this.documentList;
    }
    
    public String getArchiveFileName()
    {
        if( this.sessionFile != null ) return sessionFile.getArchiveFileName();
        else return null;
    }

    /**
     * Returns a list of the documents that do not have associate cache files.
     * @return
     */
	public LinkedList getUncollatedDocuments() 
	{
		// create a list which has all the documents in it
		LinkedList uncollatedList = new LinkedList(documentList);
		
		// get a list of the doc ids that have been cached
		HashSet cachedDocuments = sessionFile.getCachedDocumentIDList();
		
		// remove the cached docs from the list of all docs
		for( Iterator i = cachedDocuments.iterator(); i.hasNext(); )
		{
			Integer id = (Integer) i.next();			
			JuxtaDocument document = lookupDocument(id.intValue());
			uncollatedList.remove(document);
		}
		
		return uncollatedList;
	}

	public void clearCollationData() 
	{
		sessionFile.clearCollationData();
	}
	

	public MovesManager getMovesManager() {
		return movesManager;
	}
	

}
