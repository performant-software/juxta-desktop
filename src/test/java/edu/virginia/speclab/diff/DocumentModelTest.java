/*
 * Created on Feb 25, 2005
 *
 */
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
 
package edu.virginia.speclab.diff;

import edu.virginia.speclab.diff.OffsetRange;
import java.io.File;
import java.util.List;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.document.DocumentModelFactory;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

/**
 * @author Nick
 *
 * Unit test for DocumentModel
 * 
 */
public class DocumentModelTest extends TestCase
{
    String testData, testData2;
    
    public static final int DATA1_LINES = 20;
    public static final int DATA2_LINES = 1;
    
     
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
//        if(!loggingInitialized)
//        {
//            SimpleLogger.initConsoleLogging();
//            SimpleLogger.logInfo("setting up logging");   
//            loggingInitialized = true;
//        }
        testData = TestDataGenerator.generateTestData(DATA1_LINES);
        testData2 = TestDataGenerator.generateTestData(DATA2_LINES);
    }
    
    public void testDocumentModel()
    {
        SimpleLogger.logInfo("Testing document model...");

        TokenizerSettings settings = TokenizerSettings.getDefaultSettings();
        DocumentModel documentModel = DocumentModelFactory.createFromString(testData+testData2 );
        documentModel.tokenize(settings);
 
        //TODO
    }
    
    public void testGetLocationMarkerSubset() {
    	DocumentModelFactory factory = new DocumentModelFactory("UTF-8");
    	DocumentModel document = factory.createFromFile(new File("test_data/dam1.xml"));
    	
    	// grab all the markers      	
    	List subset = document.getLocationMarkerSubset(0, document.getDocumentLength());
    	assertNotNull(subset);    
    	assertEquals( document.getLocationMarkerList().size(), subset.size() );

    	LocationMarker marker = (LocationMarker) subset.get(0);
    	int markerStart = marker.getStartOffset(OffsetRange.Space.ACTIVE);
    	int markerEnd = marker.getEndOffset(OffsetRange.Space.ACTIVE);

    	// inclusive
    	subset = document.getLocationMarkerSubset(markerStart, markerEnd);
    	assert( subset.contains(marker) );

    	// contains start
    	subset = document.getLocationMarkerSubset(markerStart, markerEnd+1);
    	assert( subset.contains(marker) );

    	// contains end
    	subset = document.getLocationMarkerSubset(markerStart-1, markerEnd);
    	assert( subset.contains(marker) );

    	// contains points in between
    	subset = document.getLocationMarkerSubset(markerStart-1, markerEnd+1);
    	assert( subset.contains(marker) );
    }
}
