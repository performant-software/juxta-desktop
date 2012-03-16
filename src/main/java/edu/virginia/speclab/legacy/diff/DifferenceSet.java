/*
 * Created on Feb 8, 2005
 *
 */
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

package edu.virginia.speclab.legacy.diff;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;

/**
 * @author Nick
 * 
 * A collection of the differences between two files compared with <code>DiffAlgorithm.</code> 
 */
public class DifferenceSet
{
    private int numberOfSymbols;
    private DocumentModel baseDocument, witnessDocument;
    private LinkedList differenceList;
    
    public DifferenceSet()
    {
        this.differenceList = new LinkedList();
    }
    
    public void addDifference( Difference difference )
    {
        differenceList.add(difference);
    }
    
    /**
     * @return Returns the differenceList.
     */
    public LinkedList getDifferenceList()
    {
        return differenceList;
    }
    
    public int getLargestChangeBlock()
    {
    	int sizeOfLargestChangeBlock = 0;
    	for (Object obj:differenceList)
    	{
    		Difference difference = (Difference) obj;
    		if(difference.getType() == Difference.CHANGE && 
    				difference.getLength(Difference.BASE) + difference.getLength(Difference.WITNESS) > sizeOfLargestChangeBlock)
    			sizeOfLargestChangeBlock = difference.getLength(Difference.BASE) + difference.getLength(Difference.WITNESS);
    	}
    	return sizeOfLargestChangeBlock;
    }
    
    public int getTotalChangeDistance()
    {
    	int totalChangeDistance = 0;
    	for (Object obj:differenceList)
    	{
    		Difference difference = (Difference) obj;
    		if(difference.getType() == Difference.CHANGE)
    		{
    			totalChangeDistance += difference.getDistance();
    		}
    	}
    	return totalChangeDistance;
    }
    
    public int getNumberOfDifferenceType(int type)
    {
    	int total = 0;
    	for (Object obj:differenceList)
    	{
    		Difference difference = (Difference) obj;
    		if(difference.getType() == type)
    		{
    			total++;
    		}
    	}
    	return total;
    }
   
    /**
     * @return Returns the baseDocument.
     */
    public DocumentModel getBaseDocument()
    {
        return baseDocument;
    }
    /**
     * @return Returns the witnessDocument.
     */
    public DocumentModel getWitnessDocument()
    {
        return witnessDocument;
    }
    /**
     * @param baseDocument The baseDocument to set.
     */
    public void setBaseDocument(DocumentModel baseDocument)
    {
        this.baseDocument = baseDocument;
    }
    /**
     * @param witnessDocument The witnessDocument to set.
     */
    public void setWitnessDocument(DocumentModel witnessDocument)
    {
        this.witnessDocument = witnessDocument;
    }

	/**
	 * Get the symbol length ratio for this difference set. Generally, 
	 * the higher this number, the more effective the diff algorithm 
	 * can be, because it has more unique symbols to work with.
	 * @return number of unique symbols/number of tokens in the base text 
	 */
    public double getSymbolLengthRatio()
    {
        List tokenList = baseDocument.getTokenList();
        
        if( tokenList == null ) return 0.0f;
        
		// this is the number of unique symbols across both documents
        double numSymbols = numberOfSymbols;
		
		// this is the number of symbols in the base document 
		// (including duplicates)
        double length = tokenList.size();        
        double ratio = 0.0;
        
        if( length > 0 )
            ratio = numSymbols/length;
		
        return ratio;
    }
    

    public void setNumberOfSymbols(int numberOfSymbols)
    {        
        this.numberOfSymbols = numberOfSymbols;
    }

	public int getNumberOfSymbols() {
		return numberOfSymbols;
	}
	
	public String dumpAllDifferences()
	{
		String str = "\n";
		for (Iterator i = differenceList.iterator(); i.hasNext(); )
		{
			Difference difference = (Difference)i.next();
			str += difference.dumpContents(baseDocument.getDocumentText(), witnessDocument.getDocumentText());
		}
		return str;
	}
	
	public String dumpAllDifferencesTruncated()
	{
		String str = "\n";
		for (Iterator i = differenceList.iterator(); i.hasNext(); )
		{
			Difference difference = (Difference)i.next();
			str += difference.dumpContentsTruncated(baseDocument.getDocumentText(), witnessDocument.getDocumentText());
		}
		return str;
	}
	
    
}
