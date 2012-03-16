/*
 * Created on Mar 10, 2005
 *
 */
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.DifferenceConsolidator;
import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

/**
 * @author Nick
 *
 * Unit test to exercise paragraph diffing.
 */
public class DiffParagraphTest extends TestCase
{
    private static boolean loggingInitialized;
    
    private String baseText1 = "We hold these truths to be self-evident, that all men are created equal, that they are endowed by their Creator with certain unalienable Rights, that among these are Life, Liberty and the pursuit of Happiness. ";
    private String baseText2 = "--That to secure these rights, Governments are instituted among Men, deriving their just powers from the consent of the governed, --That whenever any Form of Government becomes destructive of these ends, it is the Right of the People to alter or to abolish it, and to institute new Government, laying its foundation on such principles and organizing its powers in such form, as to them shall seem most likely to effect their Safety and Happiness. ";
    private String baseText3 = "Prudence, indeed, will dictate that Governments long established should not be changed for light and transient causes; and accordingly all experience hath shewn, that mankind are more disposed to suffer, while evils are sufferable, than to right themselves by abolishing the forms to which they are accustomed. ";

    private String insertedText = "Inserted some text here. ";
    
    DifferenceConsolidator differenceConsolidator;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        if(!loggingInitialized)
        {
            SimpleLogger.initConsoleLogging();
            SimpleLogger.setLoggingLevel(DiffAlgorithm.VERBOSE_LOGGING);
            SimpleLogger.logInfo("setting up logging");            
            loggingInitialized = true;
        }
        
        differenceConsolidator = new DifferenceConsolidator(null, null);
    }

    public void testReflexivity()
    {
        SimpleLogger.logInfo("Performing Reflexivity Test...");
        DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText1,baseText1);
        
        assertNotSame( null, differenceSet );
        assertEquals( 0, differenceSet.getDifferenceList().size() );
    }

    public void testInsertion()
    {
        SimpleLogger.logInfo("Performing Insertion Test...");
         
        String baseText = baseText1 + " " + baseText2;
        String witnessText = baseText1 + " " + insertedText + " " + baseText2;
        
        TokenizerSettings settings = new TokenizerSettings(false,false,false);
        DiffAlgorithm diff = new DiffAlgorithm(settings);
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet );
        
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences found.", 1, diffList.size() );
        
        Difference difference = (Difference) diffList.getFirst();
        assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
        assertEquals( "Insertions should have zero length",  0, difference.getLength(Difference.BASE) );
        assertEquals( "Incorrect starting location", baseText1.length(), difference.getOffset(Difference.BASE) );
        assertEquals( "Incorrect starting location in witness", baseText1.length(), difference.getOffset(Difference.WITNESS) );
        assertEquals( "Incorrect number of inserted lines", insertedText.length(), difference.getLength(Difference.WITNESS) );
    }
    
    public void testGabler2()
	{
		SimpleLogger.logInfo("Performing Gabler2 Test...");
		String baseText = 		"The images of other males of his brood in whom he will see grotesque attempts on the part of nature to fortell or repeat himself.";
		String witnessText = 	"The images of other males of his brood will repel him. He will see in them grotesque attempts on the part of nature to fortell or repeat himself.";
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    String str = differenceSet.dumpAllDifferences();
	    SimpleLogger.logInfo(str);
	    assertNotSame( null, differenceSet);
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 3, diffList.size() );
	    
	    Difference difference = (Difference) diffList.getFirst();
	    // This test originally assumed that the first difference would be a change. The returned diff set showed it as
	    // and insert and delete instead, which is technically correct, although perhaps not as intuitive to the end user.
	    // The test reflects the insert and change. [Paul Rosen]
//	    assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );   
	    assertEquals("", difference.testContents(Difference.INSERT, 39, 0, 39, 14));
	    
	    difference = (Difference) diffList.get(1);
	    assertEquals("", difference.testContents(Difference.DELETE, 39, 7, 55, 0));
	    difference = (Difference) diffList.get(2);
	    assertEquals("", difference.testContents(Difference.INSERT, 59, 0, 67, 7));
	}

	public void testMove2()
    {
		new MoveTester();
    }

    public void testChange()
    {
       SimpleLogger.logInfo("Performing Change Test...");
       
       String baseText = baseText1 + baseText2 + baseText3;
       String witnessText = baseText1 + insertedText + baseText3;

       DiffAlgorithm diff = new DiffAlgorithm();
       DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
       assertNotSame( null, differenceSet);
       
       LinkedList diffList = differenceSet.getDifferenceList();
       assertEquals( "Incorrect number of differences", 1, diffList.size() );
       
       for( Iterator i = diffList.iterator(); i.hasNext(); )
       {
           Difference difference = (Difference) i.next();
           assertEquals( "Difference should be of type CHANGE", Difference.CHANGE, difference.getType() );
       }        
    }
    
    public void testInsertAndChange()
    {
    	SimpleLogger.logInfo("Performing I&C Test...");
    	String baseText = "This is the normal sentence.";
    	String witnessText = "This is not the normal hamburger.";
    	
    	DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet);
        
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences", 2, diffList.size() );
        
        Difference difference = (Difference) diffList.getFirst();
        assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
        
        difference = (Difference) diffList.get(1);
        assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );   
    }
    
    public void testGabler()
    {
    	SimpleLogger.logInfo("Performing Gabler Test...");
    	String baseText = "That which was lost in youth is reborn strangely in his wane of life: his daughter's child.";
    	String witnessText = "What was lost is given back to him: his daughter's child.";
    	
    	DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet);
        
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences", 3, diffList.size() );
        
        Difference difference = (Difference) diffList.getFirst();
        assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );
        
        difference = (Difference) diffList.get(1);
        assertEquals( "Wrong type", Difference.DELETE, difference.getType() );   
        
        difference = (Difference) diffList.get(2);
        assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );
    }
    
    public void testShuffle()
    {
    	SimpleLogger.logInfo("Performing Shuffle Test...");
    	String baseText = 		"one two three four";
    	String witnessText = 	"two four three one";
    	
    	DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet);
        
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences", 3, diffList.size() );
        
        /*Difference difference = (Difference) diffList.getFirst();
        assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
        
        difference = (Difference) diffList.get(1);
        assertEquals( "Wrong type", Difference.DELETE, difference.getType() );*/
    }
    
    public void testMoreMoves()
    {
    	SimpleLogger.logInfo("Performing More Moves Test...");
    	String baseText = 		"one four three five";
    	String witnessText = 	"one two three four";
    	
    	DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet);
        SimpleLogger.logInfo(differenceSet.dumpAllDifferences());
        
        differenceConsolidator.consolidateDifferences(differenceSet);
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences", 2, diffList.size() );   
    }
    
    public void testFlipFlop()
    {
    	SimpleLogger.logInfo("Performing Flip Flop Test...");
    	String baseText = 		"one two three four";
    	String witnessText = 	"one four three two";
    	
    	DiffAlgorithm diff = new DiffAlgorithm();
        DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
        assertNotSame( null, differenceSet);
        SimpleLogger.logInfo(differenceSet.dumpAllDifferences());
        
        differenceConsolidator.consolidateDifferences(differenceSet);
        LinkedList diffList = differenceSet.getDifferenceList();
        assertEquals( "Incorrect number of differences", 2, diffList.size() );   
    }
    
    public void testDeletion()
	{
	    SimpleLogger.logInfo("Performing Deletion Test...");
	
	    String baseText = baseText1 + " " + baseText2 + " " + baseText3;
	    String witnessText = baseText1 + " " + baseText3;
	
	    TokenizerSettings settings = new TokenizerSettings(false,false,false);
	    DiffAlgorithm diff = new DiffAlgorithm(settings);
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( differenceSet, null );             
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Wrong number of differences", 1, diffList.size() );
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.DELETE, difference.getType() );
	    assertEquals( "Witness length incorrect", 0, difference.getLength(Difference.WITNESS) );
	    assertEquals( "Incorrect starting location in base", baseText1.length(), difference.getOffset(Difference.BASE) );
	    assertEquals( "Incorrect starting location in witness", baseText1.length(), difference.getOffset(Difference.WITNESS) );        
	    assertEquals( "Incorrect number of deleted characters", baseText2.length(), difference.getLength(Difference.BASE)  );
	}

	public void testDeletionB()
	{
	    SimpleLogger.logInfo("Performing Deletion Test B...");
	
	    String baseText = "ABBA BAH COBRA";
	    String witnessText = "ABBA BAH";
	
	    TokenizerSettings settings = new TokenizerSettings(false,false,false);
	    DiffAlgorithm diff = new DiffAlgorithm(settings);
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( differenceSet, null );             
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Wrong number of differences", 1, diffList.size() );
	    
	    int baseStart = 9;
	    int witnessStart = 8;
	    int deletedLength = 5;
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.DELETE, difference.getType() );
	    assertEquals( "Witness length incorrect", 0, difference.getLength(Difference.WITNESS) );
	    assertEquals( "Incorrect starting location in base", baseStart, difference.getOffset(Difference.BASE) );
	    assertEquals( "Incorrect starting location in witness", witnessStart, difference.getOffset(Difference.WITNESS) );        
	    assertEquals( "Incorrect number of deleted characters", deletedLength, difference.getLength(Difference.BASE)  );
	}

	public void testShuffle2()
	{
		ArrayList list = new ArrayList();
		list.add("alpha");
		list.add("bravo");
		list.add("charlie");
		list.add("delta");
		list.add("echo");
		list.add("foxtrot");
		list.add("hotel");
		list.add("india");
		list.add("lima");
		list.add("romeo");
		list.add("whiskey");
		list.add("yankee");
		String baseText = "";
		String witnessText = "";
		for(Iterator i = list.iterator();i.hasNext();)
		{
			baseText = baseText + (String)i.next() + " ";
		}
		Collections.shuffle(list);
		for(Iterator i = list.iterator();i.hasNext();)
		{
			witnessText = witnessText + (String)i.next() + " ";
		}
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet);
	}

	public void testInsertionB()
	{
	    SimpleLogger.logInfo("Performing Insertion Test B...");
	     
	    String baseText = "ABBA BARK";
	    String witnessText = "ABBA BARK CEREMONY";
	    
	    TokenizerSettings settings = new TokenizerSettings(false,false,false);
	    DiffAlgorithm diff = new DiffAlgorithm(settings);
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet );
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences found.", 1, diffList.size() );
	    
	    int baseStart = 9;
	    int witnessStart = 10;
	    int insertLength = 8;
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
	    assertEquals( "Insertions should have zero length",  0, difference.getLength(Difference.BASE) );
	    assertEquals( "Incorrect starting location", baseStart, difference.getOffset(Difference.BASE) );
	    assertEquals( "Incorrect starting location in witness", witnessStart, difference.getOffset(Difference.WITNESS) );
	    assertEquals( "Incorrect number of inserted lines", insertLength, difference.getLength(Difference.WITNESS) );
	}

	public void testMove()
	{
	    SimpleLogger.logInfo("Performing Move Test...");
	
	    String baseText = "ABBA BABBA CADABRA";
	    String witnessText = "ABBA CADABRA BABBA"; 
	
	    TokenizerSettings settings = new TokenizerSettings(false,false,false);
	    DiffAlgorithm diff = new DiffAlgorithm(settings);
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( differenceSet, null );
        SimpleLogger.logInfo(differenceSet.dumpAllDifferences());
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 2, diffList.size() );
		
		int startBase = 5; 
		int startWitness = 5; 
		int baseLength = 0; 
		int witnessLength = 7;
	
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
	    assertEquals( "Incorrect number of lines in base", baseLength, difference.getLength(Difference.BASE) );
	    assertEquals( "Incorrect number of inserted lines in witness", witnessLength, difference.getLength(Difference.WITNESS) );
		assertEquals( "Incorrect starting location in witness", startWitness, difference.getOffset(Difference.WITNESS) );
		assertEquals( "Incorrect starting location in base", startBase, difference.getOffset(Difference.BASE) );		        
	    
	    startBase = 11; 
	    startWitness = 18; 
	    baseLength = 7; 
	    witnessLength = 0;
	
	    difference = (Difference) diffList.get(1);
	    assertEquals( "Wrong type", Difference.DELETE, difference.getType() );        
	    assertEquals( "Incorrect number of inserted lines in witness", witnessLength, difference.getLength(Difference.WITNESS) );
	    assertEquals( "Incorrect starting location in witness", startWitness, difference.getOffset(Difference.WITNESS) );
	    assertEquals( "Incorrect starting location in base", startBase, difference.getOffset(Difference.BASE) );
	    assertEquals( "Incorrect number of lines in base", baseLength, difference.getLength(Difference.BASE) );
	}

	public void testTwoSymbolMoveAndChange()
	{
		SimpleLogger.logInfo("Performing Two Symbol Move & Change Test...");
		String baseText = 		"one two OLDWORD four five six";
		String witnessText = 	"one four five two NEWWORD six";
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet);
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 2, diffList.size() );
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.DELETE, difference.getType() );
	    
	    difference = (Difference) diffList.get(1);
	    assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
	}

	public void testGabler3()
	{
		SimpleLogger.logInfo("Performing Gabler3 Test...");
		String baseText = 		"in whom he will see";
		String witnessText = 	"will repel him. He will see in them";
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet);
        SimpleLogger.logInfo(differenceSet.dumpAllDifferences());
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 5, diffList.size() );
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );
	}

	public void testTwoSymbolMove()
	{
		SimpleLogger.logInfo("Performing Two Symbol Move Test...");
		String baseText = 		"one two three four five six";
		String witnessText = 	"one ecks three four two six";
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet);
        SimpleLogger.logInfo(differenceSet.dumpAllDifferences());
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 2, diffList.size() );
	    
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );
	    
	    difference = (Difference) diffList.get(1);
	    assertEquals( "Wrong type", Difference.CHANGE, difference.getType() );
	}

	private class MoveTester
	{
		private String baseText,witnessText;
		private String testStrings[] = { "ABBA", "BABBA", "CADABRA", "DABBOO", "EEL" };
		private int  baseOffsets[], witnessOffsets[];
		private int  baseLengths[], witnessLengths[];
		private int insertWitness, insertBase;
		private int deleteWitness, deleteBase;

		public MoveTester()
		{
	        SimpleLogger.logInfo("Running MoveTester...");
			
			baseOffsets = new int[testStrings.length];
			witnessOffsets = new int[testStrings.length];
			baseLengths= new int[testStrings.length];
			witnessLengths= new int[testStrings.length];
			
			constructBaseText();
			
			for( int to=0; to < testStrings.length-1; to++ )
			{
				for( int from=0; from < testStrings.length-1; from++ )
				{
					if( to != from )
					{
						constructWitnessText(from,to);			
						testMove(from,to);
					}
				}
			}
		}

		private void testMove( int moveFrom, int moveTo )
		{
			SimpleLogger.logInfo("Testing move from: "+moveFrom+" to: "+moveTo);
			SimpleLogger.logInfo("base: "+baseText);
			SimpleLogger.logInfo("witness: "+witnessText);
			
	        TokenizerSettings settings = new TokenizerSettings(false,false,false);
	        DiffAlgorithm diff = new DiffAlgorithm(settings);
	        
			DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);	        
			assertNotSame( differenceSet, null );
			
			LinkedList diffList = differenceSet.getDifferenceList();
			assertEquals( "Incorrect number of differences", 2, diffList.size() );

			Difference difference = (Difference) diffList.get(0);	        
			int startBase = baseOffsets[insertBase]; 						
			int startWitness = witnessOffsets[insertWitness]; 
			int baseLength = 0; 
			int witnessLength = witnessLengths[insertWitness];
			testDifference(difference, Difference.INSERT, startBase, baseLength, startWitness, witnessLength );

			difference = (Difference) diffList.get(1);
	        startBase = baseOffsets[deleteBase]; 
			startWitness = witnessOffsets[deleteWitness]; 
			baseLength = baseLengths[deleteBase]; 
			witnessLength = 0;
			testDifference(difference, Difference.DELETE, startBase, baseLength, startWitness, witnessLength );
		}

		private void testDifference( Difference difference, int type, int startBase, int baseLength, int startWitness, int witnessLength )
		{
			assertEquals( "Wrong type", type, difference.getType() );
			assertEquals( "Incorrect witness offset", startWitness, difference.getOffset(Difference.WITNESS) );
	        assertEquals( "Incorrect base offset", startBase, difference.getOffset(Difference.BASE) );
	        assertEquals( "Incorrect witness length", witnessLength, difference.getLength(Difference.WITNESS) );	       
	        assertEquals( "Incorrect base length", baseLength, difference.getLength(Difference.BASE) );
		}

		private void constructWitnessText( int moveFrom, int moveTo ) 
		{
			String text = "";
			
			if( moveFrom < moveTo )
			{
				int newMoveTo = moveFrom;
				moveFrom = moveTo;
				moveTo = newMoveTo;
			}
			
			int inputIndex = 0, outputIndex = 0;
			while( inputIndex < testStrings.length )
			{
				if( outputIndex == moveTo )
				{
					insertBase = inputIndex; 
					insertWitness = outputIndex;
					text = addToWitness( text, testStrings[moveFrom], outputIndex++ );
					text = addToWitness( text, testStrings[inputIndex++], outputIndex++ );
				}
				else if( inputIndex != moveFrom )
				{
					text = addToWitness( text, testStrings[inputIndex++], outputIndex++ );
				}
				else
				{	
					deleteBase = inputIndex++;
					deleteWitness = outputIndex;
				}
			}
			
			witnessText = text;
		}
		
		private String addToWitness( String text, String addition, int symbolIndex )
		{
			if( text.length() > 0 ) text += " ";
			witnessOffsets[symbolIndex] = text.length();
			witnessLengths[symbolIndex] = addition.length();			
			return text += addition;
		}

		private void constructBaseText() 
		{
			String text = "";
			
			for( int i = 0; i < testStrings.length; i++ )
			{
				if( i != 0 ) text += " ";
				baseOffsets[i] = text.length();
				baseLengths[i] = testStrings[i].length();				
				text += testStrings[i];					
			}
			
			baseText = text;
		}
	}
}
