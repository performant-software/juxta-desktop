/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Educational Community License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.opensource.org/licenses/ecl1.txt">
 * http://www.opensource.org/licenses/ecl1.txt.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2006 by 
 * The Rector and Visitors of the University of Virginia. 
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.virginia.speclab.legacy.diff.document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

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
				return new DocumentModel(documentFile.getPath(),documentText,encodingCharSet.displayName());
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
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
			DocumentParser handler = new DocumentParser(); 
            parser.parse(documentFile,handler);
			
			document = new DocumentModel(documentFile.getName(), handler.getDocumentText(), encodingCharSet.displayName() );
			document.setEmphasisSpanList(handler.getEmphasisSpanList());
			document.setLocationMarkerList(handler.getLocationMarkerList());
        } 
        catch (SAXException e)
        {
            //TODO 
            SimpleLogger.logError(e.toString());
            e.printStackTrace();
        } 
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            SimpleLogger.logError(e.toString());
            e.printStackTrace();
        } 
        catch (ParserConfigurationException e)
        {
            // TODO Auto-generated catch block
            SimpleLogger.logError(e.toString());
            e.printStackTrace();
        }        
		
		return document;
    }
   
}
