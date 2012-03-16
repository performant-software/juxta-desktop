package edu.virginia.speclab.legacy.diff.document.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import edu.virginia.speclab.exceptions.ReportedException;

public class PhraseFinder {
	
	private SimpleAnalyzer analyzer;
	private ArrayList documentTokens;
	private String sourceDocumentText;
	
	public PhraseFinder( String sourceDocumentText ) throws ReportedException {
		analyzer = new SimpleAnalyzer();
		TokenStream documentTokenStream = analyzer.tokenStream("document", new StringReader(sourceDocumentText));
		this.sourceDocumentText = sourceDocumentText;
		
		try {
			documentTokens = convertTokenStreamToArray( documentTokenStream );
		} catch( IOException e ) {
			throw new ReportedException(e, "IO Error occurred while tokenizing the source text.");
		}
	}
	
	public ArrayList searchForPhrase( ArrayList queryTokens, int slopFactor ) {
		
		ArrayList foundPhrases = new ArrayList();
		
		// convert to delta factor, which is the index offset necessary to obtain this slop value
		int deltaFactor = slopFactor+1;

		// if the query is not zero length
		if( queryTokens.size() > 0 ) {
			// find all the matches to the first term
			ArrayList firstTermMatches = findAllMatchingTokens( (Token)queryTokens.get(0) );
			
			// iterate through the matches to the first term
			for( Iterator i = firstTermMatches.iterator(); i.hasNext(); ) {
				Integer matchIndex = (Integer) i.next();
				
				// add the matching token to a new phrase
				FoundPhrase foundPhrase = new FoundPhrase(matchIndex);				
				foundPhrase.addToken((Token)documentTokens.get(matchIndex),matchIndex);
				
				// go through the rest of the query terms and verify they are within slopFactor distance
				boolean foundAll = true;
				for( int j=1; j < queryTokens.size(); j++ ) {
					Token currentToken = (Token) queryTokens.get(j);
					// TODO what about multiple hits on a token within the window?
					int nextFoundAt = findMatchingToken( currentToken, matchIndex-deltaFactor, matchIndex+deltaFactor );
					if( nextFoundAt == -1 ) {
						foundAll = false;
						break;
					} else {
						foundPhrase.addToken((Token)documentTokens.get(nextFoundAt),nextFoundAt);
					}
				}
				
				// if all the terms were found, add this phrase to the results list
				if( foundAll ) {
					foundPhrase.createTextFragment(documentTokens, sourceDocumentText);
					foundPhrases.add(foundPhrase);
				}					
			}			
		}
		
		return foundPhrases;		
	}
		
	private ArrayList findAllMatchingTokens(Token searchToken) {		
		ArrayList matchingTokens = new ArrayList();
		
		for( int i=0; i < documentTokens.size(); i++ ) {
			Token documentToken = (Token) documentTokens.get(i);
			if( matchTerms( documentToken.termBuffer(), searchToken.termBuffer() ) ) {
				matchingTokens.add(i);
			}
		}
		return matchingTokens;
	}

	public ArrayList searchforPhrase( String phrase, int slopFactor ) throws ReportedException {
		
		TokenStream queryTokenStream = analyzer.tokenStream("query", new StringReader(phrase));

		ArrayList queryTokens; 
		try {
			queryTokens = convertTokenStreamToArray( queryTokenStream );
		} catch( IOException e ) {
			throw new ReportedException(e, "IO Error occurred while parsing the query.");
		}
		
		return searchForPhrase( queryTokens, slopFactor );
	}
	
	public static ArrayList convertTokenStreamToArray(TokenStream documentTokenStream) throws IOException {
		ArrayList documentTokens = new ArrayList();
		
		Token documentToken;
		while( (documentToken = documentTokenStream.next()) != null ) {
			documentTokens.add(documentToken);
		}			
		
		return documentTokens;
	}
	
	private boolean matchTerms( char[] termA, char[] termB ) {
		if( termA.length != termB.length ) return false;
		
		for( int i = 0; i < termA.length; i++ ) {
			if( termA[i] != termB[i] ) return false;
		}
		
		return true;
	}

	public int findMatchingToken( Token searchToken, int startOffset, int endOffset ) {
		if( documentTokens.size() == 0 ) return -1;

		// clip window on token array bounds
		if( startOffset < 0 ) startOffset = 0;
		if( endOffset < 0 ) endOffset = 0;
		if( startOffset >= documentTokens.size() ) startOffset = documentTokens.size()-1;
		if( endOffset >= documentTokens.size() ) endOffset = documentTokens.size()-1;
		
		// now search within the window
		for( int currentPosition=startOffset; currentPosition <= endOffset; currentPosition++ ) {
			Token documentToken = (Token) documentTokens.get(currentPosition);
			// if we find a match
			if( matchTerms( documentToken.termBuffer(), searchToken.termBuffer() ) ) {
				return currentPosition;
			}
		}
		return -1;
	}		
}
