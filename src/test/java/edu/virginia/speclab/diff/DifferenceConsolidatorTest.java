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

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.DifferenceConsolidator;
import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

public class DifferenceConsolidatorTest extends TestCase {

	private DifferenceSet differenceSet;
	private DocumentModel baseDoc, witnessDoc;
	private DifferenceConsolidator differenceConsolidator;
	private static String docText = "Hello, hello, hello! Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!" +
			"Hello, hello, hello!Hello, hello, hello!Hello, hello, hello!";
	
	private static String docTextWitness = "12345678901234567890 \n:;'\".,?!1234567890";
	private static String baseAddition = "1234567890";
	
	protected void setUp() throws Exception 
	{
		super.setUp();
		baseDoc = new DocumentModel("",docText,"");
		witnessDoc = new DocumentModel("",docText,"");
		baseDoc.tokenize(new TokenizerSettings(true, true, true));
		witnessDoc.tokenize(new TokenizerSettings(true, true, true));
		differenceConsolidator = new DifferenceConsolidator(baseDoc,witnessDoc);	
	}
	
	private void addDifferences(boolean isTestCaseOne)
	{
		differenceSet = new DifferenceSet();
		
		if(isTestCaseOne)
		{
			Difference diff1 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
			diff1.setBaseOffset(0);
			diff1.setBaseTextLength(25);
			diff1.setWitnessOffset(0);
			diff1.setWitnessTextLength(25);

			Difference diff2 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
			diff2.setBaseOffset(25);
			diff2.setBaseTextLength(25);
			diff2.setWitnessOffset(25);
			diff2.setWitnessTextLength(0);

			Difference diff3 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
			diff3.setBaseOffset(100);
			diff3.setBaseTextLength(50);
			diff3.setWitnessOffset(100);
			diff3.setWitnessTextLength(50);

			differenceSet.addDifference(diff1);
			differenceSet.addDifference(diff2);
			differenceSet.addDifference(diff3);
		}
		else
		{
			Difference diff1 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
			diff1.setBaseOffset(0);
			diff1.setBaseTextLength(25);
			diff1.setWitnessOffset(0);
			diff1.setWitnessTextLength(0);
			
			Difference diff2 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
			diff2.setBaseOffset(25);
			diff2.setBaseTextLength(25);
			diff2.setWitnessOffset(0);
			diff2.setWitnessTextLength(25);
			
			Difference diff3 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
			diff3.setBaseOffset(100);
			diff3.setBaseTextLength(50);
			diff3.setWitnessOffset(100);
			diff3.setWitnessTextLength(50);
			
			differenceSet.addDifference(diff1);
			differenceSet.addDifference(diff2);
			differenceSet.addDifference(diff3);
		}
	}
	
    public void testConsolidateChange()
    {
    	addDifferences(true);
        SimpleLogger.logInfo("Performing Difference Consolidator Test...");
        differenceConsolidator.consolidateDifferences(differenceSet);
        
        assertNotSame( null, differenceSet );
        assertEquals( 2, differenceSet.getDifferenceList().size() );
        Difference firstDifference = ((Difference) differenceSet.getDifferenceList().getFirst());
        assertEquals( Difference.CHANGE, firstDifference.getType());
        assertEquals( 50, firstDifference.getLength(Difference.BASE));
    }
    
    public void testConsolidateDeleteThenChange()
    {
    	addDifferences(false);
    	differenceConsolidator.consolidateDifferences(differenceSet);
    	assertNotSame( null, differenceSet );
        assertEquals( 2, differenceSet.getDifferenceList().size() );
        Difference firstDifference = ((Difference) differenceSet.getDifferenceList().getFirst());
        assertEquals( Difference.CHANGE, firstDifference.getType());
        assertEquals( 50, firstDifference.getLength(Difference.BASE));
    }
    
    public void testConsolidateTwoDeletes()
    {
    	addTwoDeletes();
    	differenceConsolidator.consolidateInsertDelete(differenceSet);
    	assertNotSame( null, differenceSet );
        assertEquals( 1, differenceSet.getDifferenceList().size() );
        Difference firstDifference = ((Difference) differenceSet.getDifferenceList().getFirst());
        assertEquals( Difference.DELETE, firstDifference.getType());
        assertEquals( 50, firstDifference.getLength(Difference.BASE));
    		
    	
    }
	private void addTwoDeletes() 
	{
		differenceSet = new DifferenceSet();

		Difference diff1 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
		diff1.setBaseOffset(0);
		diff1.setBaseTextLength(25);
		diff1.setWitnessOffset(0);
		diff1.setWitnessTextLength(0);

		Difference diff2 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
		diff2.setBaseOffset(25);
		diff2.setBaseTextLength(25);
		diff2.setWitnessOffset(0);
		diff2.setWitnessTextLength(0);
		
		differenceSet.addDifference(diff1);
		differenceSet.addDifference(diff2);
	}
	
	public void testWhiteSpaceConsumptionChange()
	{
		addConsumptionChanges();
		differenceConsolidator.consolidateDifferences(differenceSet);
    	assertNotSame( null, differenceSet );
        assertEquals( 1, differenceSet.getDifferenceList().size() );
        Difference firstDifference = ((Difference) differenceSet.getDifferenceList().getFirst());
        assertEquals( Difference.CHANGE, firstDifference.getType());
        assertEquals( 10, firstDifference.getLength(Difference.BASE));
        assertEquals( 19, firstDifference.getLength(Difference.WITNESS));
	}
	
	public void testWhitespaceConsumptionDelete()
	{
		
		addConsumptionDeletes();
		differenceConsolidator.consolidateInsertDelete(differenceSet);
    	assertNotSame( null, differenceSet );
        assertEquals( 1, differenceSet.getDifferenceList().size() );
        Difference firstDifference = ((Difference) differenceSet.getDifferenceList().getFirst());
        assertEquals( Difference.DELETE, firstDifference.getType());
        assertEquals( 10, firstDifference.getLength(Difference.BASE));		
	}

	private void addConsumptionChanges() 
	{
		differenceSet = new DifferenceSet();	
		baseDoc = new DocumentModel("",docTextWitness + baseAddition,"");
		witnessDoc = new DocumentModel("",docTextWitness ,"");
		baseDoc.tokenize(new TokenizerSettings(true, true, true));
		witnessDoc.tokenize(new TokenizerSettings(true, true, true));
		
		Difference diff1 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
		diff1.setBaseOffset(30);
		diff1.setBaseTextLength(5);
		diff1.setWitnessOffset(20);
		diff1.setWitnessTextLength(3);

		Difference diff2 = new Difference(baseDoc, witnessDoc, Difference.CHANGE);
		diff2.setBaseOffset(35);
		diff2.setBaseTextLength(5);
		diff2.setWitnessOffset(29);
		diff2.setWitnessTextLength(10);
		
		differenceSet.addDifference(diff1);
		differenceSet.addDifference(diff2);
	}
	
	private void addConsumptionDeletes() 
	{
		differenceSet = new DifferenceSet();	
		baseDoc = new DocumentModel("",docTextWitness + baseAddition,"");
		witnessDoc = new DocumentModel("",docTextWitness ,"");
		baseDoc.tokenize(new TokenizerSettings(true, true, true));
		witnessDoc.tokenize(new TokenizerSettings(true, true, true));
		
		Difference diff1 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
		diff1.setBaseOffset(30);
		diff1.setBaseTextLength(5);
		diff1.setWitnessOffset(29);
		diff1.setWitnessTextLength(0);

		Difference diff2 = new Difference(baseDoc, witnessDoc, Difference.DELETE);
		diff2.setBaseOffset(35);
		diff2.setBaseTextLength(5);
		diff2.setWitnessOffset(21);
		diff2.setWitnessTextLength(0);
		
		differenceSet.addDifference(diff1);
		differenceSet.addDifference(diff2);
	}

}
