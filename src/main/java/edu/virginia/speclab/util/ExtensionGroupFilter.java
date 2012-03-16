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
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.filechooser.FileFilter;

public class ExtensionGroupFilter extends FileFilter implements java.io.FileFilter 
{
    private String description;
    private LinkedList extensionFilterList;
    private boolean allowDirs;
    
    public ExtensionGroupFilter( String description, boolean allowDirs )
    {
        this.description = description;
        this.allowDirs = allowDirs;
        extensionFilterList = new LinkedList();    
    }
    
    public void addExtension( String extension )
    {
        extensionFilterList.add(extension);
    }
    
    public boolean accept( File f )
    {
        String filename = f.getName();
        
        for( Iterator i = extensionFilterList.iterator(); i.hasNext(); )
        {
            String extension = (String) i.next();
            if( filename.endsWith(extension) || (f.isDirectory() && allowDirs) )
                return true;
        }
        
        return false;
    }

    public String getDescription()
    {
        return description;
    }        
}
