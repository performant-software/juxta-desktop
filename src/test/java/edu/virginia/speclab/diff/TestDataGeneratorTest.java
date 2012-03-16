/*
 * Created on Feb 21, 2005
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

import junit.framework.TestCase;

/**
 * @author Nick
 * 
 * Tests the test data generator.
 */
public class TestDataGeneratorTest extends TestCase
{

    public void testGenerateTestData()
    {
        String testData = TestDataGenerator.generateTestData(10);
        
        assertTrue( testData.length() > 0 );
        
        int newLineCount = 0;
        for( int i=0; i < testData.length(); i++ )
        {
            char c = testData.charAt(i);
            if( c == '\n' ) newLineCount++;
        }

        assertEquals( 10, newLineCount );
    }

    public void testRandomText()
    {
        String testData = TestDataGenerator.randomText(80);
        assertEquals( 80, testData.length() );
    }

    public void testRandomCharacter()
    {
        char c = TestDataGenerator.randomCharacter();
        assertTrue( 0 != c );
    }

}
