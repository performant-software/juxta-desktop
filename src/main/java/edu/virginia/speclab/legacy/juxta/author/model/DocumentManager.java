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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.collation.Collation;
import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.legacy.diff.document.DocumentModelFactory;
import edu.virginia.speclab.legacy.diff.document.Image;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.legacy.juxta.author.model.manifest.BiblioData;
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
    private static final long MAX_FILE_SIZE = 510000;
    
	private JuxtaSessionFile sessionFile;
    private LinkedList documentList;
	private MovesManager movesManager;
    
    /**
     * Constructs the document manager from the specified save file, loading the documents 
     * found therein.
     * @param sessionDataFile A <code>File</code> object pointing to the session save file.
     * @throws ReportedException If there is an error loading the session data file.
     */
    public DocumentManager( File sessionDataFile ) throws ReportedException
    {
        this.sessionFile = new JuxtaSessionFile(sessionDataFile);
        this.documentList = sessionFile.loadDocumentList();      
		movesManager = new MovesManager(this);
		// if the are existing moves, load them in.
		movesManager.load(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY);
    }
	

    /**
     * Handy function to get a document from a list by its id.
     * @param documentList A list of <code>JuxtaDocument</code> objects.
     * @param id The id to search for. 
     * @return The requested <code>JuxtaDocument</code> objects or <code>null</code>.
     */
    public static JuxtaDocument findDocumentById( List documentList, int id )
    {
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            Object object = (Object) i.next();
            
            if( object instanceof JuxtaDocument )
            {
                JuxtaDocument document = (JuxtaDocument) object;
                if( document.getID() == id )
                    return document;    
            }
        }
        
        return null;
    }
    
    public TokenizerSettings getStoredTokenizerSettings()
    {
        if( sessionFile != null )
            return sessionFile.getStoredTokenizerSettings();
        else
            return null;
    }
    
    public List getStoredAnnotationList()
    {
        if( sessionFile != null )
            return sessionFile.getAnnotationList();
        else
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
     * @param JuxtaDocument The <code>JuxtaDocument</code> object of the base document.
     * @return A <code>Collation</code> object if sucessfull, or null if the cache is 
     * not found, out of date, or corrupted.
     */
    public Collation loadCollation( JuxtaDocument JuxtaDocument )
    {
        return sessionFile.loadCollation(JuxtaDocument.getID());
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
    public JuxtaDocument addDocumentFragment( String documentName, String fileName, int fragmentStart, int fragmentLength, String encoding ) throws ReportedException {
		JuxtaDocument doc = null;		
		JuxtaDocumentFactory factory = new JuxtaDocumentFactory(encoding);
		int fragmentEnd = fragmentStart + fragmentLength;

		File originalFile = new File(fileName);

		if( fileName.endsWith("xml") )
		{
			File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + 
										JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + 
										originalFile.getName() );
			
			// ensure that this is a unique filename in this dir
			targetFile = getUniqueFileName( targetFile );

			// load the original
			JuxtaDocument originalDocument = factory.readFromFile(originalFile);
			
			// pull out fragment text and fragment markers
			String fragmentText = originalDocument.getSubString(fragmentStart, fragmentLength);
			List fragmentMarkers = originalDocument.getLocationMarkerSubset(fragmentStart, fragmentEnd);
			
			// offset the fragment markers to zero
			for( Iterator i = fragmentMarkers.iterator(); i.hasNext(); ) {
				LocationMarker locationMarker = (LocationMarker) i.next();
				locationMarker.setOffset(locationMarker.getOffset()-fragmentStart);				
			}	
			
			// create a new document model and associate fragment markers with text
			DocumentModel fragmentDocument = new DocumentModel( JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY +
					 targetFile.getName(), fragmentText, encoding );
			fragmentDocument.setLocationMarkerList(fragmentMarkers);
			
			// upgrade to a JuxtaDocument, adding original bibliographic data
			doc = new JuxtaDocument( fragmentDocument, originalDocument.getBiblioData() );
			
            // if there is not a document name specified by the XML, use the name given
            if( doc.getDocumentName() == "" ) {
                doc.setDocumentName(getUniqueDocumentName(documentName));            	
            } else {
            	String uniqueName = getUniqueDocumentName(doc.getDocumentName());
            	doc.setDocumentName(uniqueName);
            }

            // write the newly created document to disk
			factory.writeToFile(doc, targetFile);
			
			// copy over the original images
            transferImageFiles( doc, originalFile ); 
		}
		else
		{
			try
			{
				File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + 
						JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + 
						originalFile.getName() + ".xml" );
				
				// ensure that this is a unique filename in this dir
				targetFile = getUniqueFileName( targetFile );
				
				DocumentModelFactory docFactory = new DocumentModelFactory(encoding); 
				String text = docFactory.loadPlainText(originalFile);
				String fragmentText = text.substring(fragmentStart, fragmentEnd);
				DocumentModel documentModel = new DocumentModel( JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY +
																 targetFile.getName(), fragmentText, encoding );

                JuxtaDocument juxtaDoc = new JuxtaDocument( documentModel, BiblioData.createNew() );

                // automatically generate location markers
                LinkedList locationMarkers = LocationMarkerGenerator.generateNewLineMarkers(juxtaDoc);
                juxtaDoc.setLocationMarkerList(locationMarkers);
				
				factory.writeToFile(juxtaDoc,targetFile);
				doc = new JuxtaDocument( juxtaDoc, getUniqueDocumentName(originalFile.getName()) );
			}
			catch( IOException e )
			{
				throw new ReportedException(e,"unable to load document fragment as plain text.");
			}
		}
        
        if( doc != null ) 
        {        
            documentList.add(doc);
        }
        
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
    public JuxtaDocument addDocument( String name, String file, String encoding ) throws ReportedException
    {   
		JuxtaDocument doc = null;		
		JuxtaDocumentFactory factory = new JuxtaDocumentFactory(encoding);

		File originalFile = new File(file);
		
		if(originalFile.length()>MAX_FILE_SIZE)
		{
			throw new ReportedException(new Exception(),"The size of the file \"" + originalFile.getName() +
					"\" is " + Long.toString(originalFile.length())+ " bytes, which is too large to load. Try breaking the larger file into several files that are each smaller than " + Long.toString(MAX_FILE_SIZE)+ " bytes.");
		}

		if( file.endsWith("xml") )
		{
			File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + 
										JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + 
										originalFile.getName() );
			
			// ensure that this is a unique filename in this dir
			targetFile = getUniqueFileName( targetFile );

			try 
			{
				FileUtilities.copyFile(originalFile,targetFile,false);
			} 
			catch (IOException e) 
			{
				throw new ReportedException(e,"unable to import document as xml.");
			}

            doc = factory.readFromFile(targetFile);            
            transferImageFiles( doc, originalFile );
            
            // if there is not a document name specified by the XML, use the name given
            if( doc.getDocumentName() == "" ) {
                doc.setDocumentName(getUniqueDocumentName(name));            	
            } else {
            	String uniqueName = getUniqueDocumentName(doc.getDocumentName());
            	doc.setDocumentName(uniqueName);
            }
		}
		else
		{
			try
			{
				File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + 
						JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + 
						originalFile.getName() + ".xml" );
				
				// ensure that this is a unique filename in this dir
				targetFile = getUniqueFileName( targetFile );

				DocumentModelFactory docFactory = new DocumentModelFactory(encoding); 
				String text = docFactory.loadPlainText(originalFile);
				DocumentModel documentModel = new DocumentModel( JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY +
																 targetFile.getName(), text, encoding );

                JuxtaDocument juxtaDoc = new JuxtaDocument( documentModel, BiblioData.createNew() );

                // automatically generate location markers
                LinkedList locationMarkers = LocationMarkerGenerator.generateNewLineMarkers(juxtaDoc);
                juxtaDoc.setLocationMarkerList(locationMarkers);
				
				factory.writeToFile(juxtaDoc,targetFile);
				doc = new JuxtaDocument( juxtaDoc, getUniqueDocumentName(originalFile.getName()) );
			}
			catch( IOException e )
			{
				throw new ReportedException(e,"unable to load document as plain text.");
			}
		}
        
        if( doc != null ) 
        {        
            documentList.add(doc);
        }
        
        return doc;
    }
    
    private void transferImageFiles( JuxtaDocument oldDoc, File originalFile ) throws ReportedException {
        for( Iterator i = oldDoc.getImageList().iterator(); i.hasNext(); )
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
    	
    	for( Iterator i = documentList.iterator(); i.hasNext(); ) {
    		JuxtaDocument document = (JuxtaDocument) i.next();
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
     *
     */
    public void saveDocuments() throws ReportedException
    {
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            JuxtaDocument document = (JuxtaDocument) i.next();
            sessionFile.saveDocument(document);            
        }
    }
	
	public void save(File file) throws ReportedException 
	{		
		saveDocuments();
		movesManager.save(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY);
		sessionFile.save(file,documentList);		
	}
    
    /**
     * Retrieve the <code>JuxtaDocument</code> for the specified document. 
     * @param id The ID of the specified document.
     * @return A <code>JuxtaDocument</code> object.
     */
    public JuxtaDocument lookupDocument( int id )
    {
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            JuxtaDocument document = (JuxtaDocument) i.next();
            
            if( document.getID() == id )
            {
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
		
		for( Iterator i = documentList.iterator(); i.hasNext(); ) {
			JuxtaDocument document = (JuxtaDocument) i.next();
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
    public LinkedList getDocumentList()
    {
        return documentList;
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
	
	public void storeOldFragments()
	{
		for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            JuxtaDocument document = (JuxtaDocument) i.next();
            if(document != null) document.storeOldFragment();
        }
	}
	
//	public void resetFragments()
//	{
//		for( Iterator i = documentList.iterator(); i.hasNext(); )
//        {
//            JuxtaDocument document = (JuxtaDocument) i.next();
//            if(document != null) document.resetFragment();
//        }
//	}


	public MovesManager getMovesManager() {
		return movesManager;
	}
	

}
