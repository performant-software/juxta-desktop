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

/**
 * @author Cortlandt
 */

package edu.virginia.speclab.diff;

import java.util.LinkedList;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import java.util.Iterator;

public class DifferenceConsolidator 
{
	private DocumentModel baseDoc, witnessDoc; 


	
	public DifferenceConsolidator(DocumentModel baseDocument, DocumentModel witnessDocument) 
	{
		this.baseDoc = baseDocument;
		this.witnessDoc = witnessDocument;
	}

	public TokenizerSettings getTokenizerSettings()
	{
		if(baseDoc==null)
			return null;
		return baseDoc.getTokenizerSettings();
	}
	
	/**
	 * Checks to make sure the entire string is composed of 
	 * ignorable characters (as determined by baseDocument's 
	 * tokenizer settings).
	 * @param connector
	 * The string which is being checked.
	 * @return
	 * Returns true only if connector is made up entirely of ignorable characters.
	 */
	private boolean shouldBeIgnored(String connector)
	{
		for(int j = 0 ; j < connector.length(); j++)
		{
			char ch = connector.charAt(j);
			if(!Character.isWhitespace(ch))
			{
				if(getTokenizerSettings().filterPunctuation())
				{
					if(Character.isLetter(ch)||Character.isDigit(ch))
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void consolidateDifferences(DifferenceSet diffSet)
	{
//		for( Iterator i = diffSet.getDifferenceList().iterator(); i.hasNext(); )
//		{
//			Difference difference = (Difference) i.next();
//			System.out.println("Difference of Type:" + difference.getType());
//			System.out.println("At " + difference.getOffset(Difference.BASE)
//					+ " in the base   , of length " + difference.getLength(Difference.BASE));
//			System.out.println("At " + difference.getOffset(Difference.WITNESS)
//					+ " in the witness, of length " + difference.getLength(Difference.WITNESS));
//		}
		
		LinkedList list = diffSet.getDifferenceList();
		
		for(int i =0; i < list.size(); i++)
		{
			Difference difference = (Difference) list.get(i);
			if(i < list.size()-1)
			{
				Difference nextDifference = (Difference) list.get(i+1);
				//if the offset + length of the first difference is equal 
				//to the offset of the second
				int endOfBaseDifference = difference.getOffset(Difference.BASE) + difference.getLength(Difference.BASE);
				int baseDifferenceSpacing = nextDifference.getOffset(Difference.BASE) 
				- endOfBaseDifference;
				
				int endOfWitnessDifference = difference.getOffset(Difference.WITNESS) + difference.getLength(Difference.WITNESS);
				int witnessDifferenceSpacing = nextDifference.getOffset(Difference.WITNESS) 
				- endOfWitnessDifference;
				
				
				String baseDifferenceSpace = "";
				String witnessDifferenceSpace = "";
				
				if(baseDoc!=null)
				{
					baseDifferenceSpace = baseDoc.getDocumentText()
					.substring(endOfBaseDifference,
							nextDifference.getOffset(Difference.BASE));
					witnessDifferenceSpace = witnessDoc.getDocumentText()
					.substring(endOfWitnessDifference,
							nextDifference.getOffset(Difference.WITNESS));
				}
				
				boolean baseDifferencesAdjacent = shouldBeIgnored(baseDifferenceSpace);				
				boolean witnessDifferencesAdjacent = shouldBeIgnored(witnessDifferenceSpace);	

				
				//remove blank spaces between differences
				if (baseDifferencesAdjacent)
					baseDifferenceSpacing=0;
				if (witnessDifferencesAdjacent)
					witnessDifferenceSpacing=0;
				
				/*System.out.println("Base DS: " + baseDifferenceSpacing + " Witness DS: " + witnessDifferenceSpacing);
				System.out.println("First type: " + difference.getType() + " Second type: " + nextDifference.getType());*/
				
				//if we have a change immediately followed by a delete or change in the base text
				if(baseDifferenceSpacing == 0 && difference.getType() == Difference.CHANGE && (nextDifference.getType() == Difference.DELETE  || nextDifference.getType() == Difference.CHANGE))
				{
					Difference newDifference = new Difference(difference.getBaseDocument(),
							difference.getWitnessDocument(),difference.getType());
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					newDifference.setBaseTextLength(nextDifference.getOffset(Difference.BASE) +
							nextDifference.getLength(Difference.BASE) - difference.getOffset(Difference.BASE));
					
					newDifference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
					
					if(nextDifference.getType() == Difference.CHANGE)
					{
						newDifference.setWitnessTextLength(nextDifference.getOffset(Difference.WITNESS) +
								nextDifference.getLength(Difference.WITNESS) - difference.getOffset(Difference.WITNESS));
					}
					else
					{
						newDifference.setWitnessTextLength(difference.getLength(Difference.WITNESS));
					}
					
					list.remove(i);//remove both of the originals differences
					list.remove(i);
					list.add(i, newDifference);//and add the new combined change
					--i;
				} 
//				if we have a delete immediately followed by a change in the base text
				else if(baseDifferenceSpacing == 0 && difference.getType() == Difference.DELETE &&  nextDifference.getType() == Difference.CHANGE)
				{
					Difference newDifference = new Difference(difference.getBaseDocument(),
							difference.getWitnessDocument(),Difference.CHANGE);
					
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
					Difference newDifference = new Difference(difference.getBaseDocument(),
							difference.getWitnessDocument(),difference.getType());
					
					newDifference.setBaseOffset(difference.getOffset(Difference.BASE));
					if(nextDifference.getType() == Difference.CHANGE)
					{
						newDifference.setWitnessTextLength(nextDifference.getOffset(Difference.BASE) +
								nextDifference.getLength(Difference.BASE) - difference.getOffset(Difference.BASE));
					}
					else
					{
						newDifference.setBaseTextLength(difference.getLength(Difference.BASE));
					}
					
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
					Difference newDifference = new Difference(difference.getBaseDocument(),
							difference.getWitnessDocument(),Difference.CHANGE);
					
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
					int endOfBaseDifference = difference.getOffset(Difference.BASE) + difference.getLength(Difference.BASE);
					int baseDifferenceSpacing = nextDifference.getOffset(Difference.BASE) 
					- endOfBaseDifference;
					
					int endOfWitnessDifference = difference.getOffset(Difference.WITNESS) + difference.getLength(Difference.WITNESS);
					int witnessDifferenceSpacing = nextDifference.getOffset(Difference.WITNESS) 
					- endOfWitnessDifference;
					
					
					String baseDifferenceSpace = "";
					String witnessDifferenceSpace = "";
					
					if(baseDoc!=null)
					{
						//if the nextDifference delete points into a place in the base text
						//which is farther back than the difference, then reverse those
						//indices to get the substring to test for ignorable characters.
						if(endOfBaseDifference<=nextDifference.getOffset(Difference.BASE))
						{
							baseDifferenceSpace = baseDoc.getDocumentText()
							.substring(endOfBaseDifference,
									nextDifference.getOffset(Difference.BASE));
						}
						else
						{
							baseDifferenceSpace = baseDoc.getDocumentText()
							.substring(nextDifference.getOffset(Difference.BASE),
									endOfBaseDifference);
						}
						
						if(endOfWitnessDifference<=nextDifference.getOffset(Difference.WITNESS))
						{
							witnessDifferenceSpace = witnessDoc.getDocumentText()
							.substring(endOfWitnessDifference,
									nextDifference.getOffset(Difference.WITNESS));
						}
						else
						{
							witnessDifferenceSpace = witnessDoc.getDocumentText()
							.substring(nextDifference.getOffset(Difference.WITNESS),
									endOfWitnessDifference);
						}
					}
					
					boolean baseDifferencesAdjacent = shouldBeIgnored(baseDifferenceSpace);				
					boolean witnessDifferencesAdjacent = shouldBeIgnored(witnessDifferenceSpace);	

					
					//remove blank spaces between differences
					if (baseDifferencesAdjacent)
						baseDifferenceSpacing=0;
					if (witnessDifferencesAdjacent)
						witnessDifferenceSpacing=0;
					
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
							Difference newDifference = new Difference(deleteDiff.getBaseDocument(),
									deleteDiff.getWitnessDocument(),Difference.CHANGE);
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
						Difference newDifference = new Difference(difference.getBaseDocument(),
								difference.getWitnessDocument(),difference.getType());
						
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
						Difference newDifference = new Difference(difference.getBaseDocument(),
								difference.getWitnessDocument(),difference.getType());
						
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
