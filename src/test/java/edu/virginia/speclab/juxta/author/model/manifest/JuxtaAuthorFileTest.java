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

import java.io.File;

import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import junit.framework.TestCase;

public class JuxtaAuthorFileTest extends TestCase
{
    public static final String TEST_FILE_NAME = "sample/BlessedDamozel.jxt";
    public static final String TEST_SAVE_FILE_NAME = "damozel_test.xml";
    
    public void testJuxtaAuthorFile()
    {
        // load test data
        JuxtaSession juxtaAuthorSession = loadTestSession(TEST_FILE_NAME);
        assertNotNull(juxtaAuthorSession);
    }

    public void testSave()
    {
        // load test data
        JuxtaSession juxtaAuthorSession = loadTestSession(TEST_FILE_NAME);
        
        // save to a new location
        try
        {
            juxtaAuthorSession.saveSession( new File(TEST_SAVE_FILE_NAME) );
        } 
        catch (LoggedException e)
        {
            e.printStackTrace();
            fail();
        }
        
        // attempt to load the file we just saved
        JuxtaSession juxtaAuthorSession2 = loadTestSession(TEST_SAVE_FILE_NAME);
        assertNotNull(juxtaAuthorSession2);
    }

    private JuxtaSession loadTestSession( String fileName )
    {
        File file = null;

        try
        {
            file =  new File(fileName);
        } 
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        
        JuxtaSession juxtaAuthorSession = null;
        try
        {
            juxtaAuthorSession = JuxtaSession.createSession(file,null,true);
        } 
        catch (LoggedException e)
        {
            e.printStackTrace();
            fail();
        }
        
        return juxtaAuthorSession;        
    }
    
}
