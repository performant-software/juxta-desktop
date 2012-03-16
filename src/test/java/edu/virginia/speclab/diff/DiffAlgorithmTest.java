/*
 * Created on Jun 14, 2007
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.MultiPassDiff;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.document.DocumentModelFactory;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

/**
 * @author Nick
 *
 * Unit test to exercise paragraph diffing.
 */
public class DiffAlgorithmTest extends TestCase
{
    private static boolean loggingInitialized;  
    private String text1, text2;
    private DocumentModelFactory docFactory;
    
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
        docFactory = new DocumentModelFactory("windows-1252");
        text1 = docFactory.loadPlainText(new File("test_data/gab1.txt"));
        text2 = docFactory.loadPlainText(new File("test_data/gab2.txt"));
    }

	/*public void testTwoSymbolMove()
	{
		SimpleLogger.logInfo("Performing Two Symbol Move Test...");
		String baseText = 		"alpha bravo charlie delta echo foxtrot";
		String witnessText = 	"alpha xray charlie delta bravo foxtrot";
		
		DiffAlgorithm diff = new DiffAlgorithm();
	    DifferenceSet differenceSet = diff.diffStrings(baseText,witnessText);
	    assertNotSame( null, differenceSet);
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
	    assertEquals( "Incorrect number of differences", 2, diffList.size() );
	    
	    //order of insert & delete doesn't really matter -- just the fact that there are only 2 ops
	    Difference difference = (Difference) diffList.getFirst();
	    assertEquals( "Wrong type", Difference.INSERT, difference.getType() );
	    
	    difference = (Difference) diffList.get(1);
	    assertEquals( "Wrong type", Difference.DELETE, difference.getType() );
	}*/
	
	public void testGablerTexts()
	{
		SimpleLogger.logInfo("Performing Full Gabler Test...");
		DocumentModel model2 = new DocumentModel("test_data/gab.txt",text1,"windows-1252");
		DocumentModel model1 = new DocumentModel("test_data/gab.txt",text2,"windows-1252");
        TokenizerSettings settings = new TokenizerSettings(false,false,true);
        model1.tokenize(settings);
        model2.tokenize(settings);
		//DiffAlgorithm diff = new DiffAlgorithm();
        
	    MultiPassDiff mpd = new MultiPassDiff(model1, model2);
	    DifferenceSet differenceSet = mpd.getDifferenceSet();
	    assertNotSame( null, differenceSet);
	    MultiPassDiff mpd2 = new MultiPassDiff(model2, model1);
	    DifferenceSet differenceSet2 = mpd2.getDifferenceSet();
	    assertNotSame( null, differenceSet2);
	    
	    LinkedList diffList = differenceSet.getDifferenceList();
//	    LinkedList diffList2 = differenceSet2.getDifferenceList();
	    //assertEquals( "Incorrect number of differences", diffList2.size(), diffList.size() );
	    for(Iterator i = diffList.iterator(); i.hasNext();)
	    {
	    	Difference d = (Difference) i.next();
	    	assertFalse(((d.getType() == Difference.DELETE) || (d.getType() == Difference.INSERT) )&&(d.getLength(Difference.WITNESS)==0 )&& (d.getLength(Difference.BASE)==0 ));
	    }
	}
	
	public void testDamozelTexts()
	{
        try {
			text1 = docFactory.loadPlainText(new File("test_data/damozel 1855 MS.txt"));
	        text2 = docFactory.loadPlainText(new File("test_data/damozel 1870 1st.txt"));
		} catch (IOException e) 
		{
			e.printStackTrace();
		}

		SimpleLogger.logInfo("Performing Blessed Damozel Test...");
		DocumentModel model2 = new DocumentModel("dam1.txt",text1,"windows-1252");
		DocumentModel model1 = new DocumentModel("dam2.txt",text2,"windows-1252");
        TokenizerSettings settings = new TokenizerSettings(true,true,true);
        model1.tokenize(settings);
        model2.tokenize(settings);
	    MultiPassDiff mpd = new MultiPassDiff(model1, model2);
	    DifferenceSet differenceSet = mpd.getDifferenceSet();
	    try
	    {
	    	BufferedWriter out = new BufferedWriter(new FileWriter("../../test.txt"));
	    	out.write(differenceSet.dumpAllDifferencesTruncated());
	    	out.close();
	    }
	    catch (IOException e)
	    {
	    }

	    assertNotSame( null, differenceSet);
	    
//	    DifferenceSet differenceSet2 = MultiPassDiff.diffDocuments(model2,model1);
//	    assertNotSame( null, differenceSet2);
//	    
//	    LinkedList diffList = differenceSet.getDifferenceList();
//	    for(Iterator i = diffList.iterator(); i.hasNext();)
//	    {
//	    	Difference d = (Difference) i.next();
//	    	assertFalse(((d.getType() == Difference.DELETE) || (d.getType() == Difference.INSERT) )&&(d.getLength(Difference.WITNESS)==0 )&& (d.getLength(Difference.BASE)==0 ));
//	    }
	}
}
/*
�Mr Brandes accepts it, Stephen said, as 
the first play of the closing period.
�Does he? What does Mr Sidney Lee, or Mr 
Simon Lazarus as some aver his name 
is, say of it?
�Marina, Stephen said, a child of storm, 
Miranda, a wonder, Perdita, that which 
was lost. What was lost is given back 
to him: his daughter's child. My 
dearest wife, Pericles says, was like 
this maid. Will any man love 
the daughter if he has not loved 
the mother?
�The art of being a grandfather, Mr 
Best murmured.
�Will he not see reborn in her, 
with the memory of his own youth 
added, another image?
_____Do you know what you are 
talking about? Love, yes. Word known 
to all men. Amor vero aliquid 
alicui bonum vult unde et 
ea quae concupiscimus ��.
�His own image to a man with that 
queer thing genius is the standard 
of all experience, material and 
moral. Such an appeal will 
touch him. The images of other 
males of his brood will repel 
him. He will see in them grotesque 
attempts of nature to foretell 
or to repeat himself.

�Mr Brandes places it as the first play of the 
closing period, Stephen said.
�Does he? John Eglinton said. What does Mr 
Sidney Lee or Mr Simon Lazarus as 
some think his name is, say of that play?
�Marina, Stephen said, child of seastorm, 
Imogen, ______________, Miranda, a 
childish wonder, Perdita, that which 
was lost. That which was lost in 
youth is reborn strangely in his wane 
of life: his daughter's child. But 
who will love the daughter if he has 
not loved the mother? I don't 
know. But will he not see in her 
recreated and with the memory of his 
own youth and his own image added to her the images 
which first awakened his love?
_____Do you know what you are 
talking about? Love, yes word known to all men. Amor vero 
aliquid alicui bonum vult unde 
et ea quae concupiscimus ��.
�A man with that queer thing genius above all whose own 
image is to him, morally and 
materially, the standard of all 
experience. He will be touched 
by that appeal or he will be 
infallibly repelled by images of 
other males of his brood in 
whom he will see grotesque 
attempts on the part of nature 
to foretell or to repeat himself.
*/