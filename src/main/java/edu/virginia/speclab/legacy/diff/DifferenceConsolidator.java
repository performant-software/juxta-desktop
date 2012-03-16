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

/**
 * @author Cortlandt
 */

package edu.virginia.speclab.legacy.diff;

import java.util.LinkedList;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;

public class DifferenceConsolidator 
{
	private DocumentModel baseDoc, witnessDoc; 


	
	public DifferenceConsolidator(DocumentModel baseDocument, DocumentModel witnessDocument) 
	{
		this.baseDoc = baseDocument;
		this.witnessDoc = witnessDocument;
	}

	public void consolidateDifferences(DifferenceSet diffSet)
	{
		/*for( Iterator i = diffSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = (Difference) i.next();
			System.out.println("Difference of Type:" + difference.getType());
			System.out.println("At " + difference.getOffset(Difference.BASE) 
					+ " in the base   , of length " + difference.getLength(Difference.BASE));
			System.out.println("At " + difference.getOffset(Difference.WITNESS) 
					+ " in the witness, of length " + difference.getLength(Difference.WITNESS));
		}*/
		
		LinkedList list = diffSet.getDifferenceList();
		
		for(int i =0; i < list.size(); i++)
		{
			Difference difference = (Difference) list.get(i);
			if(i < list.size()-1)
			{
				Difference nextDifference = (Difference) list.get(i+1);
				//if the offset + length of the first difference is equal 
				//to the offset of the second
				int baseDifferenceSpacing = nextDifference.getOffset(Difference.BASE) 
				- difference.getOffset(Difference.BASE) - difference.getLength(Difference.BASE);
				int witnessDifferenceSpacing = nextDifference.getOffset(Difference.WITNESS) 
				- difference.getOffset(Difference.WITNESS) - difference.getLength(Difference.WITNESS);
				
				//remove blank space between differences
				if (baseDifferenceSpacing == 1)
					baseDifferenceSpacing--;
				if (witnessDifferenceSpacing == 1)
					witnessDifferenceSpacing--;
				
				/*System.out.println("Base DS: " + baseDifferenceSpacing + " Witness DS: " + witnessDifferenceSpacing);
				System.out.println("First type: " + difference.getType() + " Second type: " + nextDifference.getType());*/
				
				//if we have a change immediately followed by a delete or change in the base text
				if(baseDifferenceSpacing == 0 && difference.getType() == Difference.CHANGE && (nextDifference.getType() == Difference.DELETE  || nextDifference.getType() == Difference.CHANGE))
				{
					Difference newDifference = new Difference(difference.getBaseDocumentID(),
							difference.getWitnessDocumentID(),difference.getType());
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					newDifference.setBaseTextLength(nextDifference.getOffset(Difference.BASE) +
							nextDifference.getLength(Difference.BASE) - difference.getOffset(Difference.BASE));
					
					newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
					newDifference.setWitnessTextLength(difference.getLength(Difference.WITNESS));
					
					list.remove(i);//remove both of the originals differences
					list.remove(i);
					list.add(i, newDifference);//and add the new combined change
					--i;
				} 
//				if we have a delete immediately followed by a change in the base text
				else if(baseDifferenceSpacing == 0 && difference.getType() == Difference.DELETE &&  nextDifference.getType() == Difference.CHANGE)
				{
					Difference newDifference = new Difference(difference.getBaseDocumentID(),
							difference.getWitnessDocumentID(),Difference.CHANGE);
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					newDifference.setBaseTextLength(nextDifference.getOffset(Difference.BASE) +
							nextDifference.getLength(Difference.BASE) - difference.getOffset(Difference.BASE));
					
					newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
					newDifference.setWitnessTextLength(nextDifference.getLength(Difference.WITNESS));
					
					list.remove(i);//remove both of the originals differences
					list.remove(i);
					list.add(i, newDifference);//and add the new combined change
					--i;
				} 
				//if we have a change immediately followed by a insert or change in the witness text
				else if (witnessDifferenceSpacing == 0 && difference.getType() == Difference.CHANGE &&  (nextDifference.getType() == Difference.INSERT || nextDifference.getType() == Difference.CHANGE))
				{
					Difference newDifference = new Difference(difference.getBaseDocumentID(),
							difference.getWitnessDocumentID(),difference.getType());
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					newDifference.setBaseTextLength(difference.getLength(Difference.BASE));
					
					newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
					newDifference.setWitnessTextLength(nextDifference.getOffset(Difference.WITNESS) +
							nextDifference.getLength(Difference.WITNESS) - difference.getOffset(Difference.WITNESS));
					
					list.remove(i);
					list.remove(i);
					list.add(i, newDifference);
					--i;
				}
//				if we have an insert immediately followed by a change in the witness text
				else if (witnessDifferenceSpacing == 0 && difference.getType() == Difference.INSERT && nextDifference.getType() == Difference.CHANGE)
				{
					Difference newDifference = new Difference(difference.getBaseDocumentID(),
							difference.getWitnessDocumentID(),Difference.CHANGE);
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					newDifference.setBaseTextLength(nextDifference.getLength(Difference.BASE));
					
					newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
					newDifference.setWitnessTextLength(nextDifference.getOffset(Difference.WITNESS) +
							nextDifference.getLength(Difference.WITNESS) - difference.getOffset(Difference.WITNESS));
					
					list.remove(i);
					list.remove(i);
					list.add(i, newDifference);
					--i;
				}
			}
		}
	}
	
	public void consolidateInsertDelete(DifferenceSet diffSet)
	{
		/*for( Iterator i = diffSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = (Difference) i.next();
			System.out.println("Difference of Type:" + difference.getType());
			System.out.println("At " + difference.getOffset(Difference.BASE) 
					+ " in the base   , of length " + difference.getLength(Difference.BASE));
			System.out.println("At " + difference.getOffset(Difference.WITNESS) 
					+ " in the witness, of length " + difference.getLength(Difference.WITNESS));
		}*/
		/*
		 * 	Difference of Type:1
			At 949 in the base   , of length 25
			At 1069 in the witness, of length 0
			Difference of Type:2
			At 975 in the base   , of length 0
			At 1069 in the witness, of length 17
		 */
		
		LinkedList list = diffSet.getDifferenceList();
		
		for(int i =0; i < list.size(); i++)
		{
			Difference difference = (Difference) list.get(i);
			if(i < list.size()-1)
			{

				Difference nextDifference = (Difference) list.get(i+1);
				//first test to see if the difference is zero length on both sides
				if( (nextDifference.getType() == Difference.DELETE && nextDifference.getLength(Difference.BASE) == 0) 
						|| (nextDifference.getType() == Difference.INSERT && nextDifference.getLength(Difference.WITNESS) == 0))
				{
					//if so, remove it and stay in the same place
					list.remove(i+1);
					--i;
				}
				else
				{
					//check for distance between the differences
					int baseDifferenceSpacing = nextDifference.getOffset(Difference.BASE) 
					- difference.getOffset(Difference.BASE) - difference.getLength(Difference.BASE);
					int witnessDifferenceSpacing = nextDifference.getOffset(Difference.WITNESS) 
					- difference.getOffset(Difference.WITNESS) - difference.getLength(Difference.WITNESS);
					if (baseDifferenceSpacing == 1)
						baseDifferenceSpacing--;
					if (witnessDifferenceSpacing == 1)
						witnessDifferenceSpacing--;
					
					/* assign the first difference we are looking at in the list and the second
					 * based on which one is an insert and which is delete. only proceed if
					 * we have an insert/delete pair
					 */
					Difference deleteDiff = (difference.getType() == Difference.DELETE) ? difference : (nextDifference.getType() == Difference.DELETE) ? nextDifference : null;
					Difference insertDiff = (difference.getType() == Difference.INSERT) ? difference : (nextDifference.getType() == Difference.INSERT) ? nextDifference : null;
	
						
					
					if( deleteDiff != null && insertDiff != null )
					{
						//if the offsets match up (for example, we are inserting into the same place
						//which was vacated by a delete
						if((deleteDiff.getOffset(Difference.WITNESS) == insertDiff.getOffset(Difference.WITNESS )) 
								|| (deleteDiff.getOffset(Difference.BASE) == insertDiff.getOffset(Difference.BASE )))
						{
							//then consolidate the insert/delete into a change, remove the old
							//insert and delete, and add the new change
							Difference newDifference = new Difference(deleteDiff.getBaseDocumentID(),
									deleteDiff.getWitnessDocumentID(),Difference.CHANGE);
							newDifference.setBaseOffset(deleteDiff.getOffset(Difference.BASE));
							newDifference.setBaseTextLength(deleteDiff.getLength(Difference.BASE));
							newDifference.setWitnessOffset(insertDiff.getOffset(Difference.WITNESS));
							newDifference.setWitnessTextLength(insertDiff.getLength(Difference.WITNESS));
							//set the string distance
				            TRStringDistance tr = new TRStringDistance(baseDoc,witnessDoc,newDifference);
				           	newDifference.setDistance(tr.getDistance());
							list.remove(i);
							list.remove(i);
							list.add(i, newDifference);
						}
					}
					//if we have two deletes in a row, with no space in between
					else if(baseDifferenceSpacing == 0 && difference.getType() == Difference.DELETE && nextDifference.getType() == Difference.DELETE)
					{
						Difference newDifference = new Difference(difference.getBaseDocumentID(),
								difference.getWitnessDocumentID(),difference.getType());
						
						newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
						newDifference.setBaseTextLength(
								nextDifference.getOffset(Difference.BASE)
								+ nextDifference.getLength(Difference.BASE)
								- difference.getOffset(Difference.BASE));
						
						newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
						newDifference.setWitnessTextLength(0);
						
						list.remove(i);
						list.remove(i);
						list.add(i, newDifference);
						--i;
					}
//					if we have two inserts in a row, with no space in between
					else if(witnessDifferenceSpacing == 0 && difference.getType() == Difference.INSERT && nextDifference.getType() == Difference.INSERT)
					{
						Difference newDifference = new Difference(difference.getBaseDocumentID(),
								difference.getWitnessDocumentID(),difference.getType());
						
						newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
						newDifference.setBaseTextLength(0);
						
						newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
						newDifference.setWitnessTextLength(
								nextDifference.getOffset(Difference.WITNESS)
								+ nextDifference.getLength(Difference.WITNESS)
								- difference.getOffset(Difference.WITNESS));
						
						list.remove(i);
						list.remove(i);
						list.add(i, newDifference);
						--i;
					}
				}
			}
		}
	}

}
