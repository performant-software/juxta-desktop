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

/**
 * Simple Singleton class to house access to the current DocumentManager from the current
 * session.
 *
 * @author ben
 */
public class DocumentManagerAccess {
    private static DocumentManagerAccess _instance;
    private DocumentManagerAccess() { }
    
    private DocumentManager _documentManager;
    
    public static DocumentManagerAccess getInstance()
    {
        if (_instance == null) 
        {
            _instance = new DocumentManagerAccess();
        }
        return _instance;
    }
    
    public void setDocumentManager(DocumentManager documentManager)
    {
        _documentManager = documentManager;
    }
    
    public DocumentManager getDocumentManager()
    {
        return _documentManager;
    }
}
