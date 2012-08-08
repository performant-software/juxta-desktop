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
 

package edu.virginia.speclab.juxta.author.model.manifest;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import edu.virginia.speclab.util.RelativePath;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * A <code>DocumentEntry</code> object corresponds with a document text file
 * and collation data file pair on the disk. These objects are indexable by
 * document ID. 
 * @author Nick
 *
 */
public class DocumentEntry
{
    private int documentID, activeRangeStart, activeRangeEnd;
    
    private File sourceFile, cacheDirectory;
    
    private static final String COLLATION_CACHE_FILE_EXTENSION = "dat";
    
	/**
	 * Constructs a <code>DocumentEntry</code> from an entry in the manifest 
	 * file.
	 * @param node The XML DOM node which contains information about this document.
	 * @param basePath The base path used to read relative path information.
	 * @param cacheDirectory The directory to access the corresponding cache file.
	 * @throws ReportedException If there is a problem locating the source file.
	 */
    public DocumentEntry( ComparandNode node, File basePath, File cacheDirectory ) throws ReportedException
    {		
		this.documentID = node.getDocumentID();
		this.sourceFile = createSourceFile(node.getFile(),basePath);
		this.cacheDirectory = cacheDirectory;
        this.activeRangeStart = node.getActiveRangeStart();
        this.activeRangeEnd = node.getActiveRangeEnd();
    }

	/**
	 * Constructs a <code>DocumentEntry</code> object from a <code>DocumentModel</code>.
	 * @param document The document this entry will represent.
	 * @param cacheDirectory The directory to access the corresponding cache file.
	 * @throws ReportedException If there is a problem locating the source file.
	 */
    public DocumentEntry( JuxtaDocument document, File basePath, File cacheDirectory ) throws ReportedException
    {
        this.documentID = document.getID();
        this.sourceFile = createSourceFile(document.getFileName(),basePath);
        this.cacheDirectory = cacheDirectory;
    }
 
    private File createCollationCacheFile()
    {
        return new File( cacheDirectory.getAbsolutePath() + "/" + documentID + "." + COLLATION_CACHE_FILE_EXTENSION );
    }

	/**
	 * Determine if a cache file has been created for this document yet.
	 * @return <code>true</code> if it has, <code>false</code> otherwise.
	 */
    public boolean cacheFileExists()
    {
        File cacheFile = createCollationCacheFile();
        
        if( cacheFile != null )
        {
            return cacheFile.exists();
        }
        
        return false;
    }
    
	/**
	 * Deletes the corresponding collation cache file from the disk if it
	 * exists. Otherwise, it returns silently. 
	 */
    public void deleteCacheFile()
    {
        File cacheFile = createCollationCacheFile();
        
        if( cacheFile != null && cacheFile.exists() )
        {
            cacheFile.delete();
        }
    }
    
    private File createSourceFile( String fileName, File basePath ) throws ReportedException
    {        
    	File file = new File(fileName);
    	if( !file.isAbsolute() ) {
    		file = new File( RelativePath.appendToPath(basePath.getPath(), fileName) );
    	}
    	
		// if the file exists, add it, otherwise complain
        if( file.exists())
        {
            return file;    
        }
        else
        {
            String exception = "File not found: "+file.getPath();
            SimpleLogger.logError(exception);
            throw new ReportedException(exception,"File not found when attempting to create source file.");
        }
    }

	/**
	 * The document ID for the corresponding document, matches ID in 
	 * <code>DocumentModel</code>
	 * @return The document ID.
	 */
    public int getDocumentID()
    {
        return documentID;
    }
    
	/**
	 * Constructs a new <code>DocumentModel</code> object for the document
	 * represented by this entry.
	 * @param The version of Juxta that generated this file.
	 * @return A <code>DocumentModel</code> object.
	 * @throws ReportedException If there was an error loading the document.
	 */
    public JuxtaDocument loadDocument( String juxtaVersion ) throws ReportedException
    {
		JuxtaDocumentFactory documentFactory = new JuxtaDocumentFactory(juxtaVersion);
		
		JuxtaDocument document = documentFactory.readFromFile(sourceFile);		

		if( documentID != 0 )
		{
            document.setID(documentID);
            if (activeRangeStart > -1 && activeRangeEnd > -1)
            {
                // these values are from ORIGINAL space
                document.setActiveRange(new OffsetRange(document, activeRangeStart, activeRangeEnd, Space.ORIGINAL));
            }
		}
		return document;	
    }
    
	/**
	 * De-serializes the collation from the disk. If the corresponding source 
	 * file has a more recent timestamp on it, load is aborted so that the 
	 * cache can be regenerated. Also fails if the file is not found or if the 
	 * the <code>Collation</code> class has been updated since the file was 
	 * written.
	 * @return A <code>Collation</code> object or <code>null</code> if 
	 * the loading process was unsuccessful.
	 */
    public Collation loadCollation()
    {
		SimpleLogger.logInfo("loading collation data for document id:"+documentID);
        File cacheFile = createCollationCacheFile();
        Collation collation = null;
        
        if( checkAvailableMemory(cacheFile) == false ) return null;
                
        if( sourceFile.lastModified() > cacheFile.lastModified() )
        {
            SimpleLogger.logInfo("source file modified since last collation: "+documentID);
            return null;
        }
                
        try
        {
            FileInputStream fileInStream = new FileInputStream(cacheFile);
            ObjectInputStream inStream = new ObjectInputStream(fileInStream);
            collation = (Collation) inStream.readObject();
            fileInStream.close();                    
        } 
        catch (FileNotFoundException e)
        {
            SimpleLogger.logInfo("unable to locate cache file: "+documentID);
            return null;            
            
        } 
        catch (IOException e)
        {            
            SimpleLogger.logInfo("cache file corrupted: "+documentID);
            return null;            
        }
        catch (ClassNotFoundException e)
        {
            SimpleLogger.logInfo("cache file obsolete: "+documentID);
            return null;            
        }
        
        return collation;
    }
    
	private boolean checkAvailableMemory(File cacheFile) 
    {
        long fileSize = cacheFile.length();    
        long freeMemory = Runtime.getRuntime().freeMemory();
        
        if( freeMemory - fileSize <= 0 ) return false;        
        else return true;
    }
	
    /**
     * Writes the document data to the document source file.
     * @param document The document to be saved.
     * @throws IOException If there is a problem writing the document to disk.
     */
	public void saveDocument( JuxtaDocument document ) throws ReportedException
	{
		JuxtaDocumentFactory factory = new JuxtaDocumentFactory(document.getEncoding());
        factory.writeToFile(document,sourceFile);
	}

    /**
	 * Serializes the specified collation to disk. Fails if the collation's
	 * documentID does not correspond with the ID for this document entry.
	 * @param collation The collation to serialize.
	 * @throws IOException If there is a problem writing the file.
	 */
    public void cacheCollation( Collation collation ) throws IOException
    {
        if( collation.getBaseDocumentID() != documentID ) 
        {
            SimpleLogger.logError("Collation ID does not match document ID");
            return;
        }
        
        File cacheFile = createCollationCacheFile();
        FileOutputStream fileOutStream = new FileOutputStream(cacheFile);
        ObjectOutputStream outStream = new ObjectOutputStream(fileOutStream);
        outStream.writeObject(collation);
        fileOutStream.close();   
    }

	public File getSourceFile() 
	{
		return sourceFile;
	}

	public File getCacheFile() 
	{
		return createCollationCacheFile(); 
	}
	
}