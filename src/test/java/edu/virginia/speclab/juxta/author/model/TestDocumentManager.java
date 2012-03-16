
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

import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.MultiPassDiff;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.DocumentManagerAccess;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionFile;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import junit.framework.TestCase;

public class TestDocumentManager extends TestCase
{
    public static final String testDocumentA = "sample/sample-source/BlessedDamozel/1-1847.morgms.rad.xml";
    public static final String testDocumentB = "sample/sample-source/BlessedDamozel/1-1847.princefrag.rad.xml";
    public static final String testDocumentC = "sample/sample-source/BlessedDamozel/1-1870.1pr.trox.rad.xml";
    public static final String testDocumentD = "test_data/dam1.xml";
    
    public void testDocumentManager()
    {
        
        try
        {
            TokenizerSettings settings = TokenizerSettings.getDefaultSettings();
            DocumentManager documentManager = new DocumentManager(null);
            DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
            documentManager.loadManifest();

            JuxtaDocument testDocA = documentManager.addDocument("A",testDocumentA,"UTF-8");
            testDocA.tokenize(settings);
            
            JuxtaDocument testDocB = documentManager.addDocument("B",testDocumentB,"UTF-8");
            testDocB.tokenize(settings);
            
            JuxtaDocument testDocC = documentManager.addDocument("C",testDocumentC,"UTF-8");
            testDocC.tokenize(settings);
                        
    	    MultiPassDiff mpd = new MultiPassDiff(testDocA,testDocB);
            DifferenceSet diffSet = mpd.getDifferenceSet();            
            Collation collation = new Collation();                        
            collation.addDifferenceSet(diffSet);

    	    MultiPassDiff mpd2 = new MultiPassDiff(testDocA,testDocC);
            DifferenceSet diffSet2 = mpd2.getDifferenceSet();                                               
            Collation collation2 = new Collation();
            collation2.addDifferenceSet(diffSet2);
            
            documentManager.cacheCollation(testDocA,collation);
            documentManager.cacheCollation(testDocA,collation2);
            
            Collation collationX = documentManager.loadCollation(testDocA);            
            assertNotNull(collationX);            
            
            assertEquals( collationX.getBaseDocumentID(), testDocA.getID() );            
            assertNotNull( collationX.getHistogramData() );
            assertNull( documentManager.loadCollation(testDocC) );
            assertTrue( documentManager.collationCacheExists(testDocA) ); 
        }
        catch( LoggedException e )
        {
            e.printStackTrace();
            fail();            
        }
    }
    
    public void testGetUniqueDocumentName() {
    	
    	DocumentManager documentManager = null;
    	try
    	{
           documentManager = new DocumentManager(null);
           DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
           documentManager.loadManifest();
           documentManager.addDocument("A",testDocumentA,"UTF-8");
	       assertEquals( "morgms-1", documentManager.getUniqueDocumentName("morgms") );
           documentManager.addDocument("A",testDocumentA,"UTF-8");
	       assertEquals( "morgms-2", documentManager.getUniqueDocumentName("morgms") );
	       assertEquals( "unique", documentManager.getUniqueDocumentName("unique") );
    	}
    	catch( LoggedException e )
    	{
           e.printStackTrace();
           fail();            
    	}
    }
    
    public void testGetUniqueFileName() {
    	     	
    	DocumentManager documentManager = null;
    	try
    	{
           documentManager = new DocumentManager(null);
           DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
           documentManager.loadManifest();
           documentManager.addDocument("A",testDocumentA,"UTF-8");	       
           
           File originalFile = new File( testDocumentA );
           File targetFile = new File( JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/" + 
					JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + 
					originalFile.getName() );
           
           File uniqueFile = documentManager.getUniqueFileName(targetFile);
           
           // the file should be named differently so as not to stomp on the previously added file
           assertNotSame( targetFile, uniqueFile );
    	}
    	catch( LoggedException e )
    	{
           e.printStackTrace();
           fail();            
    	}    	
    }
    
    public void testSearch() {        
        try
        {
            TokenizerSettings settings = TokenizerSettings.getDefaultSettings();
            DocumentManager documentManager = new DocumentManager(null);
            DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
            documentManager.loadManifest();
            JuxtaDocument testDocA = documentManager.addDocument("A",testDocumentA,"UTF-8");
            testDocA.tokenize(settings);
            
            JuxtaDocument testDocB = documentManager.addDocument("B",testDocumentB,"UTF-8");
            testDocB.tokenize(settings);
            
            JuxtaDocument testDocC = documentManager.addDocument("C",testDocumentC,"UTF-8");
            testDocC.tokenize(settings);
        
            SearchResults results = documentManager.search("Heaven");
            assertNotNull( results );
            assertTrue( results.getSearchResults().size() > 0 );
        }
        catch( LoggedException e )
        {
            e.printStackTrace();
            fail();            
        }    	
    }
    
    public void testSearchDocument() {        
        try
        {
            TokenizerSettings settings = TokenizerSettings.getDefaultSettings();
            DocumentManager documentManager = new DocumentManager(null);
            DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
            documentManager.loadManifest();
            JuxtaDocument testDocA = documentManager.addDocument("A",testDocumentA,"UTF-8");
            testDocA.tokenize(settings);
                    
            SearchResults results = documentManager.search("Heaven");
            assertNotNull( results );
            assertTrue( results.getSearchResults().size() > 0 );
        }
        catch( LoggedException e )
        {
            e.printStackTrace();
            fail();            
        }    	
    }

    public void testAddDocumentFragment() {
    	
    	try {    		
            TokenizerSettings settings = TokenizerSettings.getDefaultSettings();

            DocumentManager documentManager = new DocumentManager(null);
            DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
            documentManager.loadManifest();
            JuxtaDocument testDocA = documentManager.addDocument("D",testDocumentD,"UTF-8"); 
            assertNotNull( testDocA );
            
            testDocA.tokenize(settings);
           
            String docAFragment = testDocA.getSubString(10,100);
            assertNotNull( docAFragment );
            assertEquals( 100, docAFragment.length() );
            
            JuxtaDocument fragmentA = documentManager.addDocumentFragment("D Frag",testDocumentD, 10, 100, "UTF-8");
 
            assertNotNull( fragmentA );
            assertEquals( docAFragment, fragmentA.getDocumentText() );
 
            fragmentA.tokenize(settings);

    	    MultiPassDiff mpd = new MultiPassDiff(testDocA,fragmentA);
            DifferenceSet diffSet = mpd.getDifferenceSet();            
            Collation collation = new Collation();                        
            collation.addDifferenceSet(diffSet);
            
            documentManager.cacheCollation(testDocA,collation);
            
            Collation collationX = documentManager.loadCollation(testDocA);            
            assertNotNull(collationX);            
            
            assertEquals( collationX.getBaseDocumentID(), testDocA.getID() );            
            assertNotNull( collationX.getHistogramData() );
    	}
        catch( LoggedException e )
        {
            e.printStackTrace();
            fail();            
        }

    }
    
}
