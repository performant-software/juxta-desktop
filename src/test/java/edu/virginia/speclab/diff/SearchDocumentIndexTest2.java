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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import edu.virginia.speclab.juxta.author.model.SearchResults.SearchResult;
import edu.virginia.speclab.util.SimpleLogger;

public class SearchDocumentIndexTest2 extends TestCase {
	
	private JuxtaSession juxtaAuthorSession = null;
	public static final String TEST_FILE_NAME = "sample/BlessedDamozel.jxt";

	boolean loggingInitialized = false;
	
	// This tests complex searches with Lucene. It uses BlessedDamozel and different cases of searches that have
	// returned unusual results in the past.
	
	public void setUp()
	{
        if(!loggingInitialized)
        {
            SimpleLogger.initConsoleLogging();
            SimpleLogger.setLoggingLevel(DiffAlgorithm.VERBOSE_LOGGING);
            SimpleLogger.logInfo("setting up logging");            
            loggingInitialized = true;
        }
        
        File file = null;

        try
        {
            file =  new File(TEST_FILE_NAME);
        } 
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        
        try
        {
            juxtaAuthorSession = JuxtaSession.createSession(file,null,true);
        } 
        catch (LoggedException e)
        {
            e.printStackTrace();
            fail();
        }
        
    }
	
	public void tearDown()
	{
	
	}

	class ResultData
	{
		public String doc;
		public int position;
		public String fragment;
		public Object location;
	}
	class SearchData
	{
		public String query;
		public ArrayList results;
	}

	ArrayList aSearchData = new ArrayList();
	
   	//String astrSearches[] = { "tree", "tree within", "sun", "tree with", "stars", "clover", "her" };
   	//String astrSearches[] = { "tree" };
   	
   	String getAttr(Node node, String attr)
   	{
   		if (node == null) return "";
  		if (node.getAttributes() == null) return "";
  		if (node.getAttributes().getNamedItem(attr) == null) return "";
       	return node.getAttributes().getNamedItem(attr).getNodeValue();
   	}
   	
   	private void readData() throws ReportedException
   	{
   		String xmlFile = "diff/edu/virginia/speclab/diff/test/BlessedDamozelSearchTestData.xml";
   		try
   		{
   			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
   			DocumentBuilder parser = factory.newDocumentBuilder();
   			Node document = parser.parse(xmlFile);
   			NodeList top = document.getChildNodes().item(0).getChildNodes();
   			if (top != null)
   			{
   				for (int i = 0; i < top.getLength(); i++)
   				{
   					Node node = top.item(i);

   					if (node.getNodeType() == Node.ELEMENT_NODE
   							&& node.getNodeName().equals("test"))
   					{
   						//<test query="tree">
   						//<result doc="proof" position="1000" fragment="mystic tree" />
   						SearchData sd = new SearchData();
   						sd.query = getAttr(node, "query");
   						sd.results = new ArrayList();
   						aSearchData.add(sd);

   						NodeList nlResult = node.getChildNodes(); 
   						if (nlResult != null)
   						{
   							for (int j = 0; j < nlResult.getLength(); j++)
   							{
   								Node nodeResult = nlResult.item(j);
   								if (nodeResult.getNodeType() == Node.ELEMENT_NODE
   										&& nodeResult.getNodeName().equals("result"))
   								{
   									ResultData rd = new ResultData();
   									rd.doc = getAttr(nodeResult, "doc");
   									rd.position = Integer.parseInt(getAttr(nodeResult, "position"));
   									rd.fragment = getAttr(nodeResult, "fragment");
   									rd.location = getAttr(nodeResult, "location");
   									sd.results.add(rd);
   								}
   							}
   						}
   					}
   				}
   			}
   		}
   		catch (SAXException e)
   		{
   			throw new ReportedException(e, xmlFile + " is not well formed.");
   		}
   		catch (IOException e)
   		{
   			throw new ReportedException( e, "IO Exception parsing document: " + xmlFile);
   		} 
   		catch (FactoryConfigurationError e)
   		{
   			throw new ReportedException( "Could not locate factory class.","Factory Configuration Error" );
   		} 
   		catch (ParserConfigurationException e)
   		{
   			throw new ReportedException( e, "Could not locate a JAXP Parser.");
   		}  	
   	}
   	
   	
   	public void testSearch()
    {
 		SimpleLogger.logInfo("Performing Blessed Damozel Search Test...");
		DocumentManager dm = juxtaAuthorSession.getDocumentManager();
		
		try
		{
			readData();
		}
		catch (ReportedException e)
		{
			e.printStackTrace();
			fail();		
		}
		
		boolean bCreateXml = false;	// if this is true, then the XML file is generated to the console. If false, then the test is actually run.
    	for(int iSearch = 0; iSearch < aSearchData.size(); ++iSearch)
    	{
    		SearchData sd = (SearchData)aSearchData.get(iSearch);
    		
    		if (!bCreateXml)
    			SimpleLogger.logInfo("--- Search for: " + sd.query);
			
			SearchResults fragments = null;
			try
			{
				fragments = dm.search(sd.query);
			}
			catch (ReportedException e)
			{
				e.printStackTrace();
				fail();
			}
			
			List<SearchResult> results = fragments.getSearchResults();
			if (bCreateXml)
				SimpleLogger.logInfo("\t<test query=\"" + sd.query+ "\">");
			else
				assertEquals("searched for: " + sd.query + " testing num hits ", sd.results.size(), results.size());
			
			for (int j = 0; j < results.size(); ++j)
			{
				SearchResult sr = (SearchResult)results.get(j);
				ResultData rd = (bCreateXml?null:(ResultData)sd.results.get(j));
				if (bCreateXml)
					SimpleLogger.logInfo("\t\t<result doc=\"" + dm.lookupDocument(sr.getDocumentID()).getFileName() + "\" position=\"" + Integer.toString(sr.getOffset()) +
							"\" fragment=\"" + encodeStr(encodeStr(sr.getTextFragment())) + 
							"\" location=\"" + getLocationMarker(dm, sr) + 
						"\"/>");
				else
				{
					SimpleLogger.logInfo(rd.doc + "," + Integer.toString(rd.position) + "," + rd.fragment);
					assertEquals(rd.doc, dm.lookupDocument(sr.getDocumentID()).getFileName());
					assertEquals(rd.position, sr.getOffset());
					assertEquals(rd.fragment, encodeStr(sr.getTextFragment()));
					assertEquals(rd.location, getLocationMarker(dm, sr));
				}
			}
			if (bCreateXml)
				SimpleLogger.logInfo("\t</test>");
    	}
    }

	private String getLocationMarker(DocumentManager dm, SearchResult sr)
	{
		SimpleLogger.logInfo(sr.illustrateSelection(dm.lookupDocument(sr.getDocumentID()).getDocumentText()));
		LocationMarker loc = dm.lookupDocument(sr.getDocumentID()).getLocationMarker(sr.getOffset());
		if (loc == null) return "";
		return loc.getLocationName();
//		List locs = dm.lookupDocument(sr.getDocumentID()).getLocationMarker(sr.getPosition(), sr.getSelectionLength());
//		if (locs == null) return "";
//		if (locs.size() == 0) return "";
//		Iterator i = locs.iterator();
//		String str = ((LocationMarker)i.next()).getLocationName();
//		for ( ; i.hasNext(); )
//			str += ", " + ((LocationMarker) i.next()).getLocationName();
//		return str;
	}
   	
   	String encodeStr(String str)
   	{
   		str = str.replaceAll("\n",	"/n");
  		str = str.replaceAll("&", "&amp;");
  		str = str.replaceAll("<", "&lt;");
   		str = str.replaceAll(">", "&gt;");
   		return str;
   	}
}
