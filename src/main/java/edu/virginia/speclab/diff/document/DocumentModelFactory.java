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
 
package edu.virginia.speclab.diff.document;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;



import edu.virginia.speclab.util.SimpleLogger;

public class DocumentModelFactory
{
    private Charset encodingCharSet;

    public DocumentModelFactory( String encoding )
    {
        encodingCharSet = Charset.forName(encoding);
    }
    
    public static DocumentModel createFromString( String text )
    {
        DocumentModel document = new DocumentModel("",text,"UTF-8");
		return new DocumentModel(document);
    }
	
    public DocumentModel createFromFile( File documentFile )
    {
        SimpleLogger.logInfo("Loading document: "+ documentFile.getPath());

        try
        {
            if( documentFile.getName().endsWith("xml") )
            {
                return parseDocument(documentFile);
            }
            else
            {
                String documentText = loadPlainText(documentFile);
				return new DocumentModel(documentFile.getPath(),documentFile.getName(),documentText,encodingCharSet.displayName());
            }            
        } 
        catch (IOException e)
        {
            SimpleLogger.logError("Unable to open file: "+documentFile.getPath());
            return null;
        }
    }

	 /* (non-Javadoc)
     * @see edu.virginia.speclab.juxta.diff.algorithm.SymbolReader#openFile(java.lang.String)
     */
    public String loadPlainText(File documentFile ) throws IOException
    {               
        InputStreamReader inStream = new InputStreamReader( new FileInputStream(documentFile), encodingCharSet );
        BufferedReader reader = new BufferedReader(inStream);

        // get ready to read
        StringBuffer buffer = new StringBuffer();        
        char[] buf = new char[1024];
        
        // read the data from the file into the accumulation buffer
        while( reader.ready() )
        {
            int length = reader.read(buf);
            buffer.append(buf,0,length);               
        }
        
        // done        
        reader.close();
        
        String contents = buffer.toString();
        // Normalize the line endings. This is in case the file doesn't use just newlines between lines.
        contents = contents.replaceAll("\r\n", "\n");	// If both endings are used, ignore the carriage returns
        contents = contents.replaceAll("\r", "\n");	// If there are carriage returns that weren't paired with a newline, change them to a newline.
        return contents;
    }

    private DocumentModel parseDocument( File documentFile )
    {       
		DocumentModel document = null;
		
        try
        {
            JuxtaDocumentFactory jdf = new JuxtaDocumentFactory(encodingCharSet.displayName());
            document = jdf.readFromFile(documentFile);
        } 
        catch (ReportedException ex) {
            Logger.getLogger(DocumentModelFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
		return document;
    }
   
}
