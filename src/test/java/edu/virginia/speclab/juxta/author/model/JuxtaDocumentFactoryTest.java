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

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import junit.framework.TestCase;

public class JuxtaDocumentFactoryTest extends TestCase {
	
	public void testReadFromFile() {
		JuxtaDocumentFactory factory = new JuxtaDocumentFactory();
		
		// try reading a text file
		try {
			File testFile = new File("test_data/dam1.txt");
			JuxtaDocument doc = factory.readFromFile(testFile);
			assertEquals( testFile.getPath(), doc.getFileName() );	
		} catch (ReportedException e) {
			e.printStackTrace();
			fail();
		}

		// try reading an xml file
		try {
			File testFile = new File("test_data/dam1.xml");
			JuxtaDocument doc = factory.readFromFile(testFile);
			assertEquals( testFile.getPath(), doc.getFileName() );	
		} catch (ReportedException e) {
			e.printStackTrace();
			fail();
		}
}

}
