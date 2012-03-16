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

import edu.virginia.speclab.legacy.diff.document.DocumentModel;

/**
 * Houses the Multi-pass diff procedure.
 * @author Nick
 *
 */
public class MultiPassDiff 
{
	private static final int MAX_DIFF_PASSES = 10;
	private DiffAlgorithm diff;
	private DifferenceSet differenceSet;
	
	private int previousLargestChangeBlock;
	
	public DifferenceSet getDifferenceSet() { return differenceSet; }
	
    /**
     * Performs a multi-pass diff between the base and witness documents provided. This first
     * performs a normal diff, and then takes all the ares that are marked as change blocks and
     * diffs them against one another. The differences are integrated into a single set of differences,
     * providing a much higher resolution result set.
     * @return A <code>DifferenceSet</code> containing the differences between the two documents.
     */
	public MultiPassDiff( DocumentModel baseDocument, DocumentModel witnessDocument )
	{
		diff = new DiffAlgorithm(baseDocument.getTokenizerSettings());	
		DifferenceSet diffSet = diff.diffDocuments(baseDocument,witnessDocument);
		DifferenceConsolidator differenceConsolidator = new DifferenceConsolidator(baseDocument,witnessDocument);
		differenceConsolidator.consolidateDifferences(diffSet);
        
		/**
		 * Keeps doing a diff on the change blocks as long as a large change
		 * block is getting broken down, and the max number of passes haven't
		 * been made.
		 */
		previousLargestChangeBlock = Integer.MAX_VALUE;
		int iterationCount = 0;
		while(iterationCount < MAX_DIFF_PASSES
			&& diffSet.getLargestChangeBlock() < previousLargestChangeBlock)
		{
			iterationCount++;
//			System.out.println("Iteration number: " + iterationCount);
//			System.out.println("Largest Change Block: " + diffSet.getLargestChangeBlock());
//			System.out.println("Previous Largest Change Block: " + previousLargestChangeBlock);
			differenceSet = makeAdditionalPass(diffSet,baseDocument,witnessDocument);
			differenceConsolidator.consolidateDifferences(differenceSet);
			differenceConsolidator.consolidateInsertDelete(differenceSet);
			// if we are going to look at these differences later, 
			//the real diff algorithm needs the latest version of the changes.
			diff.updateDifferenceSet(differenceSet);	
			previousLargestChangeBlock = diffSet.getLargestChangeBlock();
			
			diffSet=differenceSet;				
		}
		differenceConsolidator.consolidateDifferences(differenceSet);
		differenceConsolidator.consolidateInsertDelete(differenceSet);
	}

	private DifferenceSet makeAdditionalPass(DifferenceSet originalDifferenceSet,DocumentModel baseDocument,DocumentModel witnessDocument) 
	{	
		DifferenceSet refinedDifferenceSet = new DifferenceSet();
		refinedDifferenceSet.setBaseDocument(baseDocument);
		refinedDifferenceSet.setWitnessDocument(witnessDocument);
		refinedDifferenceSet.setNumberOfSymbols(originalDifferenceSet.getNumberOfSymbols());
		
		for( Iterator i = originalDifferenceSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = (Difference) i.next();
			DiffAlgorithm subdiff = new DiffAlgorithm(baseDocument.getTokenizerSettings());
			
			if( difference.getType() == Difference.CHANGE )
			{
				String baseText = baseDocument.getSubString(difference.getOffset(Difference.BASE),
															difference.getLength(Difference.BASE) );
					
				String witnessText = witnessDocument.getSubString(difference.getOffset(Difference.WITNESS),
						  										  difference.getLength(Difference.WITNESS) );
				
				// perform a diff on the text with the change blocks
				DifferenceSet subDifferences = subdiff.diffStrings(baseText,witnessText);
				
				// add the resulting differences to the new difference set.
				addSubDifferences( refinedDifferenceSet, subDifferences, difference );
			}
			else
			{
				refinedDifferenceSet.addDifference(difference);
			}
		}
		
		return refinedDifferenceSet;
		
	}

	private static void addSubDifferences(DifferenceSet newDifferenceSet, DifferenceSet subDifferences, Difference difference) 
	{
		// go through all the sub difference, convert them into differences in the 
		// original documents and add them to the new difference set.
		for( Iterator i = subDifferences.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference subDifference = (Difference) i.next();
						
			Difference newDifference = new Difference( difference.getBaseDocumentID(), 
													   difference.getWitnessDocumentID(), 
													   subDifference.getType() );
			
			int adjustedBaseOffset = difference.getOffset(Difference.BASE) + subDifference.getOffset(Difference.BASE);
			int adjustedWitnessOffset = difference.getOffset(Difference.WITNESS) + subDifference.getOffset(Difference.WITNESS);

			newDifference.setBaseOffset(adjustedBaseOffset);
			newDifference.setWitnessOffset(adjustedWitnessOffset);
			newDifference.setBaseTextLength(subDifference.getLength(Difference.BASE));
			newDifference.setWitnessTextLength(subDifference.getLength(Difference.WITNESS));
            newDifference.setDistance(subDifference.getDistance());

			newDifferenceSet.addDifference(newDifference);
		}
	}

	// Given the offset in one document, this finds the offset of that character in the other document.
	// If there is a perfect match, then it is easy to know what to return.
	// If it doesn't appear in the other document, then return it's insert point.
	// If it is part of a change, return -1.
	public int getBaseOffset(int witnessOffset, boolean getEnd)
	{
		return diff.getCorrespondingBaseOffset(witnessOffset, getEnd);
	}

	public int getWitnessOffset(int baseOffset, boolean getEnd)
	{
		return diff.getCorrespondingWitnessOffset(baseOffset, getEnd);
	}
}
