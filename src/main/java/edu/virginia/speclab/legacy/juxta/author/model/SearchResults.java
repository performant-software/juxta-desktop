package edu.virginia.speclab.legacy.juxta.author.model;

import java.util.ArrayList;
import java.util.Iterator;

import edu.virginia.speclab.legacy.diff.document.search.FoundPhrase;

public class SearchResults {
	
	ArrayList searchResults;
	private String originalQuery;
	
	public SearchResults( String originalQuery ) {
		this.originalQuery = originalQuery;
		searchResults = new ArrayList();
	}
	
	public void addPhrases(int id, ArrayList phrases) {
		for( Iterator i = phrases.iterator(); i.hasNext(); ) {
			FoundPhrase phrase = (FoundPhrase) i.next();
			searchResults.add(new SearchResult( id, phrase));			
		}		
	}
	
	public static SearchResult factory(int position, String textFragment)
	{
		SearchResults sr = new SearchResults("");
		return sr.new SearchResult(-1, null);
	}
		
	public class SearchResult {
		
		private int documentID;
		private FoundPhrase phrase;
		
		public SearchResult( int documentID, FoundPhrase phrase ) {
			this.documentID = documentID;
			this.phrase = phrase;
		}

		public int getDocumentID() {
			return documentID;
		}

		public int getOffset() {
			return phrase.getOffset();
		}

		public String getTextFragment() {
			return phrase.getTextFragment();
		}
				
		public int getLength()
		{
			return phrase.getLength();
		}
		
		public String dump()
		{
			return " pos: " + getOffset() + " len: " + getLength();
		
		}
		
		public String illustrateSelection(String baseText)
		{
			String str = "";
			int pos = getOffset();
			str += "S: " + Truncate(insertBrackets(baseText, pos, pos + getLength()), pos, pos + getLength()) + "\n"; 
			return str;
		}
		
		private String Truncate(String strSrc, int iStart, int iEnd)
		{
			int first = iStart - 20;
			if (first < 0) first = 0;
			int last = iEnd + 20; 
			if (last >= strSrc.length())
				last = strSrc.length()-1;
			return "[" + iStart + "," + iEnd + "] " + strSrc.substring(first, last);
		}
		
		private String insertBrackets(String strSrc, int iStart, int iEnd)
		{
			try
			{
				if (iStart == iEnd)
				{
					String str = strSrc.substring(0, iStart) + "[" + "]" + strSrc.substring(iEnd); 
					return str.replaceAll("\n", "/");
				}
				else
				{
					String str = strSrc.substring(0, iStart) + "[" + strSrc.substring(iStart, iEnd) + "]" + strSrc.substring(iEnd); 
					return str.replaceAll("\n", "/");
				}
			}
			catch (Exception e)
			{
				return "Error:" + iStart + " " + iEnd + " " + strSrc;
			}
		}
		
	}

	public ArrayList getSearchResults() {
		return searchResults;
	}

	public String getOriginalQuery() {
		return originalQuery;
	}

}
