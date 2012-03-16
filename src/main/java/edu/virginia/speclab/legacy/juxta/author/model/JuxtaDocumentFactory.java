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

package edu.virginia.speclab.legacy.juxta.author.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.document.Block;
import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.legacy.diff.document.DocumentModelFactory;
import edu.virginia.speclab.legacy.diff.document.DocumentParser;
import edu.virginia.speclab.legacy.diff.document.DocumentWriter;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.legacy.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * Responsible for loading and saving <code>JuxtaDocument</code> objects. 
 * @author Nick
 *
 */
public class JuxtaDocumentFactory  
{
    private DocumentModelFactory factory;    
    private Charset encodingCharSet;
    
    // parsing states
    private static final int STRUCTURE_MODE = 0;    
    private static final int BIBLIO_MODE = 1;
    private static final int DOCUMENT_MODE = 2;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public JuxtaDocumentFactory()
    {
        factory = new DocumentModelFactory(DEFAULT_ENCODING);        
        encodingCharSet = Charset.forName(DEFAULT_ENCODING);
    }

    public JuxtaDocumentFactory( String encoding )
    {
        factory = new DocumentModelFactory(encoding);        
        encodingCharSet = Charset.forName(encoding);
    }
	
    /**
     * Reads the specified text or xml file, parses it and returns a juxta document model object.
     * @param fileName The path to the target file.
     * @return A <code>JuxtaDocument</code> object.
     * @throws ReportedException If there was a problem reading the file.
     */
	public JuxtaDocument readFromFile( File documentFile ) throws ReportedException
	{
        if( documentFile.getName().endsWith("xml") )
        {
			SimpleLogger.logInfo("Parsing document: "+ documentFile.getPath());
            return parseDocument(documentFile);
        }
        else
        {	
            // import the document from a text file
            DocumentModel document = factory.createFromFile(documentFile);
            
            // the new document object
			return new JuxtaDocument(document,BiblioData.createNew(documentFile.getName()));
        }            
    }
	
	public JuxtaDocument createFragmentFromExistingDocument( JuxtaDocument existingDocument, int fragmentStart, int fragmentLength ) {
		String fragmentText = existingDocument.getSubString(fragmentStart, fragmentLength);
		DocumentModel document = new DocumentModel( existingDocument.getFileName(), fragmentText, "UTF-8" );
		return new JuxtaDocument( document, existingDocument.getBiblioData() );
	}
        
    /**
     * Reads just the bibliographic information from the specified file.
     * @param file A <code>File</code> object pointing to the target file.
     * @return A <code>BiblioData</code> object.
     * @throws ReportedException 
     */
    public BiblioData readBiblioData( File file ) throws ReportedException
    {
        if( file.getName().endsWith("xml") )
        {
            return parseBiblioData(file);
        }
        else
        {
            return BiblioData.createNew();
        }            
    }
    
    private BiblioData parseBiblioData( File file ) throws ReportedException
    {
        BiblioData data = null;
        
        try
        {
            XMLReader reader = XMLReaderFactory.createXMLReader();            
            JuxtaDocumentParser handler = new JuxtaDocumentParser();            
            reader.setContentHandler(handler);
            reader.parse( file.toURI().toString() );
            data = handler.getBiblioData();           
        } 
        catch (SAXException e)
        {
			throw new ReportedException(e,"There was an XML error parsing the file "+file.getName()+"." );
        } 
        catch (IOException e)
        {
			throw new ReportedException(e,"An error occured reading the file "+file.getName()+"." );
        } 
        
        return data;
    }
	
    /**
     * Writes the document to the target source file. This overwrites any existing file.
     * @param document The document to write.
     * @param sourceFile The file to write to.
     * @throws ReportedException If anything bad happens writing the file. 
     */
	public void writeToFile( JuxtaDocument document, File sourceFile ) throws ReportedException 
	{
		JuxtaDocumentWriter documentWriter = new JuxtaDocumentWriter(document);
		
		try 
		{
			documentWriter.writeDocument(sourceFile);
		} 
		catch (IOException e) 
		{
			throw new ReportedException(e,"An error occurred writing the file "+sourceFile);
		}
	}
	
    private JuxtaDocument parseDocument( File documentFile ) throws ReportedException
    {       
		JuxtaDocument document = null;
		
        try
        {           
            XMLReader reader = XMLReaderFactory.createXMLReader();            
			JuxtaDocumentParser handler = new JuxtaDocumentParser();
            handler.setBaseDirectory(documentFile);
            reader.setContentHandler(handler);
            reader.parse( documentFile.toURI().toString() );
			
			if( handler.getDocumentText() == null )
			{
				throw new ReportedException("Unable to load text: "+documentFile,
											"Unable to load file, XML document structure not compatible: "+documentFile);				
			}
			else
			{
				DocumentModel doc = new DocumentModel( documentFile.getPath(), handler.getDocumentText(), encodingCharSet.displayName() );
				doc.setEmphasisSpanList(handler.getEmphasisSpanList());
                doc.setLocationMarkerList(handler.getLocationMarkerList());
				doc.setBlockList(handler.getBlockList());
				document = new JuxtaDocument( doc, handler.getBiblioData() );	
				for(Iterator i=document.getBlockList().iterator();i.hasNext();)
				{
					Block b = (Block) i.next();
					String tag;
					//try getting first 50 chars, but otherwise text is too short, get all the way to end
					try{	
					tag = document.getDocumentText().substring(b.getStartOffset(), b.getStartOffset()+50) + "...";
					}catch(Exception e)
					{
						tag = document.getDocumentText().substring(b.getStartOffset());
					}
					b.setTexttag(tag);
				}
			}
			
        } 
        catch (SAXException e)
        {
			throw new ReportedException(e,"There was an XML error parsing the file "+documentFile+"." );
        } 
        catch (IOException e)
        {
			throw new ReportedException(e,"An error occured reading the file "+documentFile+"." );
        } 
		
		return document;
    }

    private class BiblioDataParser extends DefaultHandler
    {
        private StringBuffer currentString;
        private String currentFieldName;
        
        private String title;
        private String shortTitle;
        private String author;
        private String editor;
        private String source;
        private String date;
        private String notes;
        
        private BiblioData biblioData;
		
		public BiblioDataParser()
		{
			this.biblioData = BiblioData.createNew();
		}
        
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if( isValidFieldName(qName) )
            {
                currentFieldName = qName;
                currentString = new StringBuffer();
            }
            else
            {
                currentFieldName = null;
                currentString = null;
            }
        }
        
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if( currentString == null ) return;
           
            for( int i=start; i<start+length; i++ )
            {
                currentString.append(ch[i]);
            }
        }
        
        private boolean isValidFieldName( String fieldName )
        {
            if( fieldName.equals("title") || 
                fieldName.equals("short-title") ||
                fieldName.equals("author") ||
                fieldName.equals("editor") ||
                fieldName.equals("source") ||
                fieldName.equals("date") ||
                fieldName.equals("notes"))   return true;
            else return false;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if( currentFieldName == null || currentString == null ) return;
            
			if( qName.equals("bibliographic") )
            {
                biblioData = new BiblioData( title, shortTitle, author, editor, source, date, notes );
				return;
            }
            
            if( qName.equals("title") )
            {
                title = currentString.toString();
            }                
            else if( qName.equals("short-title") )
            {
                shortTitle = currentString.toString();
            }            
            else if( qName.equals("author") )
            {
                author = currentString.toString();
            }            
            else if( qName.equals("editor") )
            {
                editor = currentString.toString();
            }
            else if( qName.equals("date") ) 
            {
                date = currentString.toString();
            }                
            else if( qName.equals("source") )
            {
                source = currentString.toString();
            }                
            else if( qName.equals("notes") )
            {
                notes = currentString.toString();
            }
            else 
            {
				throw new SAXException("Encountered unexpected element in <bilbiographic> element: "+currentFieldName);
            }
        }

        public BiblioData getBiblioData()
        {
            return biblioData;
        }
                            
    }
    
    private class JuxtaDocumentParser extends DefaultHandler
    {
        private int currentMode;
        private DocumentParser markedDocumentHandler;
        private BiblioDataParser biblioDataHandler;
		
        public JuxtaDocumentParser()
        {           
            currentMode = STRUCTURE_MODE;
            markedDocumentHandler = new DocumentParser();
            biblioDataHandler = new BiblioDataParser();
        }


		public void setBaseDirectory( File baseDirectory )
        {
            markedDocumentHandler.setBaseDirectory(baseDirectory);
        }
		
		public BiblioData getBiblioData()
		{
			return biblioDataHandler.getBiblioData();
		}
        
        public List getLocationMarkerList() 
		{
			return markedDocumentHandler.getLocationMarkerList();
		}

		public List getEmphasisSpanList() 
		{
			return markedDocumentHandler.getEmphasisSpanList();
		}
        
        public List getBlockList() 
        {
        	return markedDocumentHandler.getBlockList();
			
		}

		public String getDocumentText() 
		{
			return markedDocumentHandler.getDocumentText();
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if( qName.equals("bibliographic") )
            {
                currentMode = BIBLIO_MODE;
            }            
            else if( qName.equals("text") )
            {
                currentMode = DOCUMENT_MODE;
            }

            // defer to bibliographic parser
            if( currentMode == BIBLIO_MODE )
                biblioDataHandler.startElement(uri,localName,qName,attributes);

            // defer to text parser
            if( currentMode == DOCUMENT_MODE )
                markedDocumentHandler.startElement(uri,localName,qName,attributes);
        }
        
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            // defer to bibliographic parser
            if( currentMode == BIBLIO_MODE )
                biblioDataHandler.characters(ch,start,length);

            // defer to text parser
            if( currentMode == DOCUMENT_MODE )
                markedDocumentHandler.characters(ch,start,length);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            // defer to bibliographic parser
            if( currentMode == BIBLIO_MODE )
                biblioDataHandler.endElement(uri,localName,qName);

            // defer to text parser
            if( currentMode == DOCUMENT_MODE )
                markedDocumentHandler.endElement(uri,localName,qName);

            if( qName.equals("text") || qName.equals("bibliographic") )
            {
                currentMode = STRUCTURE_MODE;
            }
        }		
    }
	
	private class JuxtaDocumentWriter
	{
		private JuxtaDocument document;
		
		public JuxtaDocumentWriter( JuxtaDocument document )
		{
			this.document = document;
		}
		
		public void writeDocument( File saveFile ) throws IOException
		{
			StringBuffer buffer = new StringBuffer();
			
	        buffer.append("<?xml version=\"1.0\" encoding=\""+encodingCharSet.displayName()+"\"?>\n");        
	        buffer.append("<juxta-document>\n");
			
			writeBilbioData(buffer);
			writeDocumentText(buffer);
			
			buffer.append("</juxta-document>\n");
			            
			OutputStreamWriter outStream = new OutputStreamWriter( new FileOutputStream(saveFile), encodingCharSet );
			BufferedWriter writer = new BufferedWriter(outStream);
	        writer.write(buffer.toString());
	        writer.close();
		}
		
		private void writeBilbioData( StringBuffer buffer )
		{
			BiblioData data = document.getBiblioData();
			
			buffer.append("<bibliographic>\n");
			
			buffer.append("<title>"+DocumentWriter.escapeText(data.getTitle())+"</title>\n");
			buffer.append("<short-title>"+DocumentWriter.escapeText(data.getShortTitle())+"</short-title>\n");
			buffer.append("<author>"+DocumentWriter.escapeText(data.getAuthor())+"</author>\n");
			buffer.append("<editor>"+DocumentWriter.escapeText(data.getEditor())+"</editor>\n");
			buffer.append("<source>"+DocumentWriter.escapeText(data.getSource())+"</source>\n");
			buffer.append("<date>"+DocumentWriter.escapeText(data.getDate())+"</date>\n");
			buffer.append("<notes>"+DocumentWriter.escapeText(data.getNotes())+"</notes>\n");
			
			buffer.append("</bibliographic>\n");
		}
		
		private void writeDocumentText( StringBuffer buffer )
		{
			buffer.append("<text>");
			
			TreeMap sortedStartMarkers = new TreeMap();
			TreeMap sortedEndMarkers = new TreeMap();
			
			List locationList = document.getLocationMarkerList();
            
            if( locationList != null )
            {
                for( Iterator i = locationList.iterator(); i.hasNext(); )
                {
                    LocationMarker marker = (LocationMarker) i.next();
                    
                    Integer startPosition = new Integer(marker.getOffset());
                    Integer endPosition = new Integer(marker.getOffset()+marker.getLength());
                    
                    List startList = (List) sortedStartMarkers.get(startPosition);
                    
                    if( startList == null )
                    {
                        startList = new LinkedList();
                        sortedStartMarkers.put(startPosition,startList);
                    }
                    
                    startList.add(marker);

                    List endList = (List) sortedEndMarkers.get(endPosition);
                    
                    if( endList == null )
                    {
                        endList = new LinkedList();
                        sortedEndMarkers.put(endPosition,endList);
                    }
                    
                    endList.add(marker);
                }
            }
            
            TreeMap sortedBlockMarkers = new TreeMap();
            List blockList = document.getBlockList();
            if(blockList != null)
            {
            	for(Iterator i = blockList.iterator(); i.hasNext(); )
            	{
            		Block block = (Block) i.next();
            		
            		Integer position = new Integer(block.getStartOffset());
            		
            		List positionList = (List) sortedBlockMarkers.get(position);
            		if(positionList == null)
            		{
            			positionList = new LinkedList();
            			sortedBlockMarkers.put(position,positionList);
            		}
            		positionList.add(block);
            	}
            }
			
            // iterate character by character through the document, placing down 
            // location markers where necessary.
            // 07/13/07 update : places block markers as well
			for( int j=0; j <= document.getDocumentLength(); j++ )
			{                
                Integer offsetKey = new Integer(j);
				List startMarkerList = (List) sortedStartMarkers.get(offsetKey);
                
				if( startMarkerList != null )
				{
                    for( Iterator k = startMarkerList.iterator(); k.hasNext(); )
                    {
                        LocationMarker marker = (LocationMarker) k.next();
                        writeMilestoneStart(buffer,marker);    
                    }
				}
				
                List endMarkerList = (List) sortedEndMarkers.get(offsetKey);
                
				if( endMarkerList != null )
				{
                    for( Iterator k = endMarkerList.iterator(); k.hasNext(); )
                    {
                        LocationMarker marker = (LocationMarker) k.next();
                        writeMilestoneEnd(buffer,marker);
                    }
				}
				
				List blockPositionList = (List) sortedBlockMarkers.get(offsetKey);
				
				if( blockPositionList != null )
				{
					for( Iterator k = blockPositionList.iterator(); k.hasNext(); )
                    {
                        Block block = (Block) k.next();
                        writeBlock(buffer,block);
                    }
				}
								
				if( j < document.getDocumentLength() )
                    DocumentWriter.escapeText(document.getDocumentText().charAt(j),buffer);				
			}
			
			buffer.append("</text>\n");			
		}
        
		private void writeMilestoneStart( StringBuffer buffer, LocationMarker locationMarker) 
		{
			// <m_s id="1" type="abc" img="xyz" n="1" />
			buffer.append("<m_s id=\""+locationMarker.getID()+"\" ");
			buffer.append("type=\""+locationMarker.getLocationType()+"\" ");
            
            if( locationMarker.getImage() != null )
                buffer.append("img=\""+locationMarker.getImage().getImageFile().getName()+"\" ");
            
            buffer.append("n=\""+locationMarker.getNumber()+"\" ");
			buffer.append("/>");
		}
		
		private void writeMilestoneEnd( StringBuffer buffer, LocationMarker locationMarker)
		{
			//<m_e refid="1"/>
			buffer.append("<m_e refid=\""+locationMarker.getID()+"\" ");
			buffer.append("/>");
		}
		
		private void writeBlock( StringBuffer buffer, Block block)
		{
			//account for blocks with no notes
			//and for blocks with illegal xml chars
			String notes = (block.getNotes() == null) ? "" : block.getNotes();
			notes = escapeNotes(notes);
			buffer.append("<block id=\""+block.getId()+"\" ");
			buffer.append("notes=\""+notes+"\" ");
			buffer.append("/>");
		}
		
		private String escapeNotes(String notes)
		{
			for(int i = 0 ; i < notes.length() ; i ++)
			{
				char ch = notes.charAt(i);
				String replacement = "" + ch;
				boolean replacementNeeded = true;
				switch(ch)
				{
				case '"':
					replacement = "&quot;";
					break;
				case '\'':
					replacement = "&apos;";
					break;
				case '>':
					replacement = "&gt;";
					break;
				case '<':
					replacement = "&lt;";
					break;
				case '&':
					replacement = "&amp;";
					break;
				default:
					replacementNeeded = false;
					break;
				}
				notes = notes.substring(0, i) + replacement + notes.substring(i+1, notes.length());
				//skip over the newly inserted ampersand
				//(otherwise we would never terminate
				if(replacementNeeded)
					i++;
			}
			return notes;
		}
	}
}
