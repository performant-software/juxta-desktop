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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipException;

import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.diff.document.search.PhraseFinder;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.FatalException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.Juxta;
import edu.virginia.speclab.juxta.author.model.manifest.DocumentEntry;
import edu.virginia.speclab.juxta.author.model.manifest.DocumentManifestXMLFile;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.util.FileUtilities;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * Whereas <code>JuxtaSession</code> represents the session currently in memory,
 * this class represents the collection of files on the disk. Higher level classes
 * in the model use this class to access the files on the disk.  
 * @author Nick
 *
 */
public class JuxtaSessionFile
{
    private DocumentManifestXMLFile documentManifest;
    private LinkedList documentEntryList;
    private TokenizerSettings storedTokenizerSettings;
    private LinkedList annotationList;
    
	private File archiveFile;
    private File sessionFile, sessionBaseDirectory;
    private File cacheDirectory;
	
	private static final String SYSTEM_TEMP_DIRECTORY = System.getProperty("java.io.tmpdir");
	public static final String JUXTA_TEMP_DIRECTORY =  SYSTEM_TEMP_DIRECTORY + "/juxta";
	public static final String JUXTA_DOCUMENT_DIRECTORY =  "docs/";
    public static final String JUXTA_SOURCE_DOCUMENT_DIRECTORY = "source/";
	public static final String JUXTA_INDEX_DIRECTORY = "index";
    private static final String CACHE_DIRECTORY = "juxta_cache";


	/**
	 * Create a new session file.  
	 * @param sessionFile The location of the manifest file on the disk or <code>null</code>
	 * for a new blank session. In this case, the cache files will be stored int the 
	 * system temp directory. 
	 * @throws ReportedException If there is trouble loading the session.
	 */
    public JuxtaSessionFile( File archiveFile ) throws ReportedException 
    {       
        this.archiveFile = archiveFile;
        
        // create the session directory
        initializeSessionDirectory(new File(JUXTA_TEMP_DIRECTORY));

        // create the juxta cache file directory 
        createCacheDirectory(this.sessionBaseDirectory);
        
        // create the docs dir
        createDocsDirectory(this.sessionBaseDirectory);
                
        if( archiveFile == null )
        {	
            this.sessionFile = null;
            documentEntryList = new LinkedList();
            annotationList = new LinkedList();			
        }
        else
        {
			try 
			{
				FileUtilities.unzip(archiveFile,this.sessionBaseDirectory);
			} 
			catch (ZipException e) 
			{
				throw new ReportedException(e,"Unable to open JXT file!");
			}
			catch (IOException e) 
			{
				throw new ReportedException(e,"Unable to read JXT file!");
			}
			
	        this.sessionFile = new File( this.sessionBaseDirectory.getAbsolutePath() + "/manifest.xml" );
			
        }
    }

    public void loadDocumentManifest() throws ReportedException
    {
        if (this.sessionFile != null)
        {
            documentManifest = new DocumentManifestXMLFile(this.sessionFile,cacheDirectory);
            documentEntryList = documentManifest.createDocumentEntrySet();
            storedTokenizerSettings = documentManifest.getTokenizerSettings();
        }
    }

    public void loadAnnotations()
    {
        if (this.sessionFile != null)
        {
            annotationList = documentManifest.createAnnotationList();
        }
    }
    
    /**
     * Load the parse templates configuration data from the juxta file. If the file
     * was saved without template configuration, default to loading the global
     * config
     * 
     * @return The parse template configuration
     * @throws ReportedException
     */
    public TemplateConfig loadTemplateConfiguration() throws ReportedException {
        // attempt to load templates.xml from the session root.
        // If this is not possible, default to the global config.
        File cfgFile = new File( this.sessionBaseDirectory.getAbsolutePath() + "/templates.xml" );
        if ( cfgFile.exists() == false) {
            cfgFile = new File("config/templates.xml");
        }
     
        TemplateConfig cfg = null;
        try {
            cfg = TemplateConfig.fromFile(cfgFile.toString());
            
            // make sure the config file now points to a location in the
            // juxta work area now. This ensures that the templates will
            // be persisted there instead of overwriting the global config
            cfg.updateFile( new File( this.sessionBaseDirectory.getAbsolutePath() + "/templates.xml" ) );
         } catch (FatalException e) {
             throw new ReportedException(e,"Unable to load JXT template configuraion");
         }
        return cfg;
    }

	private void initializeSessionDirectory( File sessionDir ) throws ReportedException
    {
        if( sessionDir.exists() )
        {
            // delete any existing juxta temp files
            FileUtilities.recursiveDelete(sessionDir,false);
        }
        else if( !sessionDir.mkdir() )
        {
            throw new ReportedException("Unable to create Juxta session directory.","Unable to create Juxta session directory");
        }       
        
        this.sessionBaseDirectory = sessionDir;
    }

    // create a cache directory to store collation cache files
    private void createCacheDirectory( File parentDirectory ) throws ReportedException
    {
        if( !parentDirectory.exists() )
        {
            SimpleLogger.logError("Unable to set cache directory.");
            return;
        }
        
        try 
		{
			cacheDirectory = new File(parentDirectory.getCanonicalPath() + "//" + CACHE_DIRECTORY);
		} 
		catch (IOException e) 
		{
	        throw new ReportedException(e, "Unable to create cache directory.");		
		}
        
        if( !cacheDirectory.exists() )
        {
            cacheDirectory.mkdir();
        }
        
        SimpleLogger.logInfo("Cache directory set to: "+cacheDirectory.getAbsolutePath());
    }

    // create a docs directory 
    private void createDocsDirectory( File parentDirectory ) throws ReportedException
    {
        if( !parentDirectory.exists() )
        {
            SimpleLogger.logError("Unable to set docs directory.");
            return;
        }
        
        try 
        {
            File docsDirectory = new File(parentDirectory.getCanonicalPath() + "/" + JUXTA_DOCUMENT_DIRECTORY);
            
            if( !docsDirectory.exists() )
            {
                docsDirectory.mkdir();
            }

            File sourceDirectory = new File(docsDirectory.getCanonicalPath() + "/" + JUXTA_SOURCE_DOCUMENT_DIRECTORY);
            if (!sourceDirectory.exists())
            {
                sourceDirectory.mkdir();
            }
        } 
        catch (IOException e) 
        {
            throw new ReportedException(e, "Unable to create docs directory.");        
        }
    }

	/**
	 * Loads from the disk the collation for which the specified document is the 
	 * base document. 
	 * @param documentID A valid document ID.
	 * @return The corresponding <code>Collation</code> object. Note that these 
	 * objects can be quite large. 
	 */
    public Collation loadCollation( int documentID )
    {
        DocumentEntry entry = getDocumentEntry(documentID);
        
        if( entry != null )
            return entry.loadCollation();
        else 
            return null;
    }
    
	/**
	 * Loads all of the source documents and passes them back in a list. 
	 * @return A list of <code>JuxtaDocument</code> objects.
	 */
    public LinkedList loadDocumentList()
    {
        LinkedList documentList = new LinkedList();        
        synchronized(documentEntryList)
        {
            for( Iterator i = documentEntryList.iterator(); i.hasNext(); )
            {
                DocumentEntry entry = (DocumentEntry) i.next();

                try
                {
                    JuxtaDocument document = entry.loadDocument(documentManifest.getJuxtaVersion());                    
                    documentList.add(document);
                }
                catch( ReportedException e )
                {
                    ErrorHandler.handleException(e);
                }
            }
        }
        return documentList;
    }
    
    public String getJuxtaVersion() {
    	if( documentManifest == null ) {    		
    		return Juxta.JUXTA_VERSION;
    	} else {
        	String version = documentManifest.getJuxtaVersion();
        	return version != null ? version : Juxta.JUXTA_VERSION;    		
    	}
    }
    
    /**
     * Save the specified document to the disk.
     * @param document The document to save.
     * @throws IOException If an error occurs writing the document out.
     */
    public void saveDocument( JuxtaDocument document ) throws ReportedException
    {
        DocumentEntry entry = getDocumentEntry(document.getID());
        
        if( entry != null )
        {
            entry.saveDocument(document);
        }        
    }
    
	/**
	 * Obtain the set of documents for which we can locate cache files. 
	 * @return A set of <code>Integer</code> objects containing document IDs.
	 */
    public HashSet getCachedDocumentIDList()
    {
        HashSet documentIDList = new HashSet();

        synchronized(documentEntryList)
        {
            for( Iterator i = documentEntryList.iterator(); i.hasNext(); )
            {
                DocumentEntry entry = (DocumentEntry) i.next();

                // if this document has a cached collation file
                if( entry.cacheFileExists() )
                {
                    documentIDList.add( new Integer( entry.getDocumentID() ));
                }
            }
        }
        
        return documentIDList;
    }
        
    public ArrayList search( String documentText, String searchQuery ) throws ReportedException {

    	PhraseFinder phraseFinder = new PhraseFinder( documentText );
		return phraseFinder.searchforPhrase(searchQuery, 5);
    }

    private DocumentEntry addDocumentEntry( JuxtaDocument document ) throws ReportedException
    {
        DocumentEntry documentEntry = new DocumentEntry( document, this.sessionBaseDirectory, this.cacheDirectory );
        synchronized(documentEntryList)
        {
            documentEntryList.add(documentEntry);
        }
        return documentEntry;
    }
    
    private DocumentEntry getDocumentEntry( int documentID )
    {
        synchronized(documentEntryList)
        {
            for( Iterator i = documentEntryList.iterator(); i.hasNext(); )
            {
                DocumentEntry documentEntry = (DocumentEntry) i.next();

                if( documentEntry.getDocumentID() == documentID )
                {
                    return documentEntry;
                }
            }
        }
        return null;
    }

	/**
	 * Removes the given document from the session, deleting the cache file 
	 * (does not delete source file)
	 * @param document The document to remove.
	 */
    public void removeDocument( JuxtaDocument document )
    {
        DocumentEntry entry = getDocumentEntry(document.getID());
        if( entry != null )
        {
            synchronized(documentEntryList)
            {
                documentEntryList.remove(entry);
            }
            entry.deleteCacheFile();
        }
    }
    
	/**
	 * Caches the given collation. If the document is new, adds it to the 
	 * session file model. 
	 * @param document The base document of the collation. 
	 * @param collation The collation to serialize.
	 * @throws ReportedException If there is a problem serializing the collation.
	 */
    public void cacheCollation( JuxtaDocument document, Collation collation ) throws ReportedException 
    {
        DocumentEntry entry = getDocumentEntry( collation.getBaseDocumentID() );
        
        if( entry == null )
        {
            entry = addDocumentEntry(document);
        }

        try 
        {
            entry.cacheCollation(collation);        
        } 
        catch (IOException e) 
        {
            throw new ReportedException(e, "Unable to cache collation: "+document.getDocumentName());       
        }
    }
    
    public String getArchiveFileName()
    {
        if( this.archiveFile != null ) return archiveFile.getName();
        else return null;
    }

    public File getSessionBaseDirectory()
    {
        return sessionBaseDirectory;
    }

	/**
	 * Deletes all cached collation files. 
	 */
	public void clearCollationData() 
	{
        synchronized(documentEntryList)
        {
    		for( Iterator i = documentEntryList.iterator(); i.hasNext(); )
    		{
    			DocumentEntry entry = (DocumentEntry) i.next();
    			entry.deleteCacheFile();
    		}
        }
	}

    public TokenizerSettings getStoredTokenizerSettings()
    {
        return storedTokenizerSettings;
    }

    public LinkedList getAnnotationList()
    {
        return annotationList;
    }

    /**
     * Save the session to a jxt (zip) file. The fullSave flag helps control what gets
     * zipped and is used for the export functionality. When set to true, cached data
     * and images will NOT be added to the jxt.
     * 
     * @param saveFile
     * @param documentList
     * @param fullSave
     * @throws ReportedException
     */
	public void save( File saveFile, LinkedList documentList, boolean fullSave ) throws ReportedException  
	{
		
		HashMap fileMap = new HashMap();
        synchronized(documentEntryList)
        {
            for( Iterator i = documentEntryList.iterator(); i.hasNext(); )
            {
                DocumentEntry entry = (DocumentEntry) i.next();
                File sourceFile = entry.getSourceFile();
                String zipEntryName = JUXTA_DOCUMENT_DIRECTORY+sourceFile.getName();
                fileMap.put(zipEntryName,sourceFile);
                String sourceZipEntryName = JUXTA_DOCUMENT_DIRECTORY + JUXTA_SOURCE_DOCUMENT_DIRECTORY + entry.loadDocument(Juxta.JUXTA_VERSION).getSourceDocument().getFileName();
                fileMap.put(sourceZipEntryName, new File(sourceFile.getParent() + "/" + JUXTA_SOURCE_DOCUMENT_DIRECTORY + entry.loadDocument(Juxta.JUXTA_VERSION).getSourceDocument().getFileName()));

                if( entry.cacheFileExists() && fullSave )
                {
                    File cacheFile = entry.getCacheFile();
                    String cacheZipEntry = "juxta_cache/"+cacheFile.getName();
                    fileMap.put(cacheZipEntry,cacheFile);
                }
            }
        }
        
        if ( fullSave ) {
            LinkedList imageList = processImageList(documentList);
            for( Iterator i = imageList.iterator(); i.hasNext(); )
            {
                Image image = (Image) i.next();
                File sourceFile = image.getImageFile();
                String zipEntryName = "docs/images/"+sourceFile.getName(); 
                fileMap.put(zipEntryName,sourceFile);
            }
        }

		fileMap.put("manifest.xml",new File( JUXTA_TEMP_DIRECTORY + "/manifest.xml" ));
		fileMap.put("moves.xml",new File( JUXTA_TEMP_DIRECTORY + "/moves.xml" ));
		fileMap.put("templates.xml",new File( JUXTA_TEMP_DIRECTORY + "/templates.xml" ));
	
		try 
		{
			FileUtilities.zip(fileMap,saveFile);
		} 
		catch (IOException e) 
		{
			throw new ReportedException(e,"An error occurred saving the file: "+saveFile.getName());
		}
	}
    
    private LinkedList processImageList(LinkedList documents)
    {
        LinkedList imageList = new LinkedList();
        
        for( Iterator i = documents.iterator(); i.hasNext(); )
        {
            JuxtaDocument document = (JuxtaDocument) i.next();
            
            List docImageList = document.getImageList();
            imageList.addAll(docImageList);
        }
        
        return imageList;
    }


}
