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

package edu.virginia.speclab.diff.document.search;

import java.util.ArrayList;

import org.apache.lucene.analysis.Token;

public class FoundPhrase {
	private ArrayList tokens;
	private int lowestIndex, highestIndex;
	private int editDistanceReferenceIndex;
	private int editDistance;
	private boolean distanceMeasured;
	private String textFragment;
	private int textOffset, textLength;
	
	public FoundPhrase( int editDistanceReferenceIndex ) {
		this.editDistanceReferenceIndex  = editDistanceReferenceIndex;
		this.lowestIndex = Integer.MAX_VALUE;		
		distanceMeasured = false;
		tokens = new ArrayList();
	}
	
	public void addToken( Token token, int positionIndex ) {
		// calculate total edit distance for phrase
		if( positionIndex < lowestIndex ) lowestIndex = positionIndex;
		if( positionIndex > highestIndex ) highestIndex = positionIndex;
		editDistance += Math.abs(positionIndex - editDistanceReferenceIndex);	
		distanceMeasured = true;
		tokens.add(token);
	}
	
	public void createTextFragment( ArrayList documentTokens, String documentText ) {
		if( lowestIndex == Integer.MAX_VALUE ) return;
		
		Token startToken = (Token) documentTokens.get(lowestIndex);
		Token endToken = (Token) documentTokens.get(highestIndex);
		
		textOffset = startToken.startOffset();
		int textEnd = endToken.endOffset();
		textLength = textEnd - textOffset;
		
		// We want to return a little bit before and after the search, but we want to break the text at a word boundary.
		// So we move a number of characters before and after, then search earlier and later for white space.
		final int MIN_SURROUNDING_NUM_CHARS = 15;
		int start = textOffset - MIN_SURROUNDING_NUM_CHARS;
		if (start <= 0)
			start = 0;
		else
		{
			while (!Character.isWhitespace(documentText.charAt(start)) && (start > 0) )
				--start;
		}
		int end = textEnd + MIN_SURROUNDING_NUM_CHARS;
		if (end >= documentText.length()-1)
			end = documentText.length()-1;
		else
		{
			while (!Character.isWhitespace(documentText.charAt(end)) && (end < documentText.length()-1) )
				++end;
		}
		textFragment = documentText.substring(start, textOffset) + "<b>" + 
			documentText.substring(textOffset, textEnd) + "</b>" + documentText.substring(textEnd, end);	
	}
	
	public int getOffset() {
		return textOffset;
	}
	
	public int getLength() {
		return textLength;
	}
			
	public ArrayList getTokens() {
		return tokens;
	}

	public double getScore() {
		if( !distanceMeasured ) return 0.0;
		return 1.0/((double)editDistance);
	}

	public int getEditDistance() {
		return editDistance;
	}

	public String getTextFragment() {
		return textFragment;
	}
	
}