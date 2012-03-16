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
 

package edu.virginia.speclab.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class RelativePathTest extends TestCase
{

    public void testCreateRelativePath()
    { 	
        File tom1 = new File("data"+File.separator+"hamlet test"+File.separator+"folio.last.txt");
        File dataDir = new File(System.getProperty("user.dir"));
        File dataDir2 = new File("data"+File.separator+"hamlet test"+File.separator);
        File sample1 = new File("sample"+File.separator+"hamlet"+File.separator+"hamlet_folio.txt");
                
        try
        {
            System.out.println("base dir: "+dataDir2.getCanonicalPath());
            System.out.println("target file: "+sample1.getCanonicalPath());
            
            String relativePath = RelativePath.createRelativePath(dataDir, tom1);            
            assertEquals(relativePath,"data"+File.separator+"hamlet test"+File.separator+"folio.last.txt");
            
            String relativePath2 = RelativePath.createRelativePath(dataDir2, sample1);

            System.out.println(relativePath2);            
        } 
        catch (IOException e)
        {          
            e.printStackTrace();
            fail();
        }
    }

}
