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

import java.util.ArrayList;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.document.search.FoundPhrase;
import edu.virginia.speclab.diff.document.search.PhraseFinder;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

public class TestPhraseFinder extends TestCase {

	private static boolean loggingInitialized;

	protected void setUp() throws Exception {
		super.setUp();

		if (!loggingInitialized) {
			SimpleLogger.initConsoleLogging();
			SimpleLogger.setLoggingLevel(DiffAlgorithm.VERBOSE_LOGGING);
			SimpleLogger.logInfo("setting up logging");
			loggingInitialized = true;
		}
	}

	public void testSearchForPhrase() {			
		// test direct match
		assertEquals( 1, countHits( "test rabbit", "This is a test rabbit bunny", 0) );
		assertEquals( 0, countHits( "test rabbit", "This is a muffin", 0) );
		
		// test out of order terms
		assertEquals( 0, countHits( "test rabbit", "rabbit This is a test funny bunny", 0) );
		assertEquals( 1, countHits( "test rabbit", "rabbit This is a test funny bunny", 3) );

		// test slop after
		assertEquals( 0, countHits( "test rabbit", "This is a test bunny rabbit", 0) );
		assertEquals( 1, countHits( "test rabbit", "This is a test bunny rabbit ", 1) );
		assertEquals( 0, countHits( "this rabbit", "This is a test bunny rabbit ", 3) );
		assertEquals( 1, countHits( "this rabbit", "This is a test bunny rabbit ", 4) );
		
		// test slop before
		assertEquals( 0, countHits( "test rabbit", "rabbit This is a test funny bunny", 1) );
		assertEquals( 0, countHits( "test rabbit", "rabbit This is a test funny bunny", 2) );
		assertEquals( 1, countHits( "test rabbit", "rabbit This is a test funny bunny", 3) );

		// test text fragments
		assertEquals( "This is a <b>test rabbit</b> bunn", getTextFragment( "test rabbit", "This is a test rabbit bunny", 0) );
		assertEquals( "<b>rabbit This is a test</b> funny bunn", getTextFragment( "test rabbit", "rabbit This is a test funny bunny", 3) );

		// test scoring
		assertEquals( 1.0, getScore( "test rabbit", "This is a test rabbit bunny", 0) );
		assertEquals( 0.25, getScore( "test rabbit", "rabbit This is a test funny bunny", 3) );

		// TODO test multiple instances of the same phrase
		
		// TODO test multiple instances of a secondary keyword
		
		
	}
	
	protected String getTextFragment( String search, String text, int slopFactor ) {
		ArrayList results = null;
		try {
			PhraseFinder finder = new PhraseFinder(text);
			results = finder.searchforPhrase(search, slopFactor);
		} catch (ReportedException e) {
			e.printStackTrace();
			fail();
		}
		
		if( results.size() > 0 ) {
			FoundPhrase phrase = (FoundPhrase) results.get(0);
			return phrase.getTextFragment();
		} else {
			return null;
		}
	}
	
	protected double getScore( String search, String text, int slopFactor ) {
		ArrayList results = null;
		try {
			PhraseFinder finder = new PhraseFinder(text);
			results = finder.searchforPhrase(search, slopFactor);
		} catch (ReportedException e) {
			e.printStackTrace();
			fail();
		}
		
		if( results.size() > 0 ) {
			FoundPhrase phrase = (FoundPhrase) results.get(0);
			return phrase.getScore();
		} else {
			return 0.0;
		}
	}

	protected int countHits( String search, String text, int slopFactor ) {
		ArrayList results = null;
		try {
			PhraseFinder finder = new PhraseFinder(text);
			results = finder.searchforPhrase(search, slopFactor);
		} catch (ReportedException e) {
			e.printStackTrace();
			fail();
		}
		
		return results.size();
	}
}
