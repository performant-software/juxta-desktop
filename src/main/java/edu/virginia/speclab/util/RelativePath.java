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

public class RelativePath
{
    public static String createRelativePath( File baseDir, File targetFile ) throws IOException
    {
        // get absolute and unique paths to both locations
        File canonicalBaseDir = new File(baseDir.getCanonicalPath());
        File canonicalTargetFile = new File(targetFile.getCanonicalPath());
        
        // get the directory of the target file
        File targetParentDir = canonicalTargetFile.getParentFile();
        
        // compute a relative path from the base dir to the target dir
        String partialRelativePath = computePath( canonicalBaseDir, targetParentDir );
        
        // append the name of the file to the relative path        
        String relativePath = appendToPath( partialRelativePath, targetFile.getName() );
                
        // if we just got back the absolute path, the file is either on another branch
        // or another root of the tree. 
        if( relativePath.compareTo( canonicalTargetFile.getAbsolutePath() ) == 0 )
        {
            //TODO traverse other branches and roots  
        }
        
        // weed out pesky back slashes
        relativePath = relativePath.replace('\\','/');
        
        return relativePath;        
    }
    
    public static String appendToPath( String path, String name )
    {
        if( path.length() == 0 )
        {
            return name;
        }
        else if( path.endsWith(File.separator) )
        {
            return path + name;
        }
        // else, stick one in there
        else
        {
            return path + File.separator + name;
        }        
    }
    
    private static String computePath( File canonicalBaseDir, File targetParentDir )
    {
        // if the two paths are identical, we have reached the beginning of the path
        if( targetParentDir.compareTo(canonicalBaseDir) == 0 )
        {            
            return "";
        }
        // the two paths differ
        else
        {
            File targetParentParentDir = targetParentDir.getParentFile();
            
            if( targetParentParentDir == null )
            {
                // we've run out of path an still no match, must be on a different branch
                return targetParentDir.getPath();                 
            }
            else
            {
                // recursively travel up the path till we find the beginning
                String partialPath = computePath( canonicalBaseDir, targetParentParentDir );    

                // add just the name for the beginning of the path
                if( partialPath.length() == 0 )
                {
                    return targetParentDir.getName();
                }
                // otherwise append this name to the partial path and return
                else 
                {
                    return appendToPath( partialPath, targetParentDir.getName() );
                }
            }
        }
    }
    
   
    

}
