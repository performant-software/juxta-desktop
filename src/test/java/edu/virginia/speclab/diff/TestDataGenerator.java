/*
 * Created on Feb 18, 2005
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

/**
 * @author Nick
 *
 */
public class TestDataGenerator
{    
    public static String generateTestData( int numberOfLines )
    {
        StringBuffer testData = new StringBuffer();
        
        for( int i=0; i < numberOfLines; i++ )
        {
            testData.append(randomText(80)).append('\n');            
        }        
        
        return testData.toString();
    }
    
    public static String randomText( int numberOfCharacters )
    {
        StringBuffer text = new StringBuffer();
        
        for( int i=0; i < numberOfCharacters; i++ )
        {
            text.append(randomCharacter());
        }
        
        return text.toString();        
    }
    
    public static char randomCharacter()
    {
        int startChar = '1';
        int endChar = 'z';
        int charRange =  endChar - startChar;
        double randomNumber = Math.random();
        int value = (int)(( randomNumber * charRange ) + startChar);     
        return (char) value;        
    }
}
