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

package edu.virginia.speclab.juxta.author.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.MovesManager;
import edu.virginia.speclab.juxta.author.model.ComparisonSet;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.DocumentManagerAccess;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.MovesManager.FragmentPair;
import edu.virginia.speclab.juxta.author.model.MovesManager.MoveList;
import edu.virginia.speclab.util.SimpleLogger;
import junit.framework.TestCase;

public class TestMoves  extends TestCase
{
	private DocumentManager documentManager;
	private ComparisonSet comparisonSet;
	MovesManager movesManager;

	private static final String SYSTEM_TEMP_DIRECTORY = System.getProperty("java.io.tmpdir");
	private static final String file1 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file1.txt";
	private static final String file2 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file2.txt";
	private static final String file3 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file3.txt";
	private static final String file4 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file4.txt";
	private static final String file5 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file5.txt";
	private static final String file6 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file6.txt";
	private static final String file7 = SYSTEM_TEMP_DIRECTORY + File.separatorChar + "file7.txt";
	private DebugLogger log;
	
	protected void setUp() throws Exception
    {
        super.setUp();

        //SimpleLogger.initFileLogging(System.getProperty("user.dir") + "/juxta.log");
        SimpleLogger.initConsoleLogging();
        //SimpleLogger.setLoggingLevel(DiffAlgorithm.VERBOSE_LOGGING);
        log = new DebugLogger(System.getProperty("user.dir") + "/TestMoves.log");
        
        WriteFile(file1, "da fust line\nan' da secunt line\n3\nNow is the time for the fourth line\nfinally, the lassed line\nPsych! actually there is another line at the end.\n");
        WriteFile(file2, "da first line\nfinally, the last line\nan' da secunt line\n33\nNow is the tine for the forth line\nPsych! actually there is another line at the end.\n");
        WriteFile(file3, "first line\nan' da secunt line\n33\nfinally, the last line\nNow is the time for the fourth line\nPsych! actually there is another line at the end.\n");
        WriteFile(file4, "da fust line\nan' da secunt line\n3\nNow is the time for the fourth line\nfinally, the lassed line\n");
        WriteFile(file5, "da first line\nfinally, the last line\nan' da secunt line\n33\nNow is the tine for the forth line\n");
        WriteFile(file6, "From the fixt lull of heaven, she saw\nTime, like a pulse, shake fierce\nThrough all the worlds. Her gaze still strove,\nIn that steep gulph, to pierce\nThe swarm: and then she spake, as when\nThe stars sang in their spheres.\n\n�I wish that he were come to me,\nFor he will come,� she said.\n�Have I not prayed in solemn heaven?\nOn earth, has he not prayed?\nAre not two prayers a perfect strength?\nAnd shall I feel afraid?\n\n�When round his head the aureole clings,\nAnd he is clothed in white,\nI'll take his hand, and go with him\nTo the deep wells of light,\nAnd we will step down as to a stream\nAnd bathe there in God's sight.\n");
        WriteFile(file7, "From the fixed place of Heaven she saw\n Time like a pulse shake fierce\nThrough all the worlds. Her gaze still strove\n Within the gulf to pierce\nIts path; and now she spoke as when\n The stars sang in their spheres.\n\nThe sun was gone now; the curled moon\n Was like a little feather\nFluttering far down the gulf; and now\n She spoke through the still weather.\nHer voice was like the voice the stars\n Had when they sang together.\n\n(Ah sweet! Even now, in that bird's song,\n Strove not her accents there,\nFain to be hearkened? When those bells\n Possessed the mid-day air,\nStrove not her steps to reach my side\n Down all the echoing stair?)\n\n�I wish that he were come to me,\n For he will come,� she said.\n�Have I not prayed in Heaven?�on earth,\n Lord, Lord, has he not pray'd?\nAre not two prayers a perfect strength?\n And shall I feel afraid?\n\n�When round his head the aureole clings,\n And he is clothed in white,\nI'll take his hand and go with him\n To the deep wells of light;\nWe will step down as to a stream,\n And bathe there in God's sight\n");

        try
		{
	        documentManager = new DocumentManager(null);
            DocumentManagerAccess.getInstance().setDocumentManager(documentManager);
            documentManager.loadManifest();

	        comparisonSet = new ComparisonSet(documentManager,false);
			movesManager = documentManager.getMovesManager();
			movesManager.addListener(comparisonSet);
	
	        //LinkedList documentList = documentManager.getUncollatedDocuments();

	        addDoc(file1);
	        addDoc(file2);
		}
		catch( LoggedException e )
		{
			e.printStackTrace();
			fail();            
		}
   }

	public void testMovesManager()
	{
		// This tests the block manager. We add Moves in various orders and with various errors, and we see what blocks are
		// returned. We also remove documents to make sure that the Moves are deleted correctly.
		try
		{
			addDoc(file3);
            JuxtaDocument doc1 = getDoc(0);
            JuxtaDocument doc2 = getDoc(1);
            JuxtaDocument doc3 = getDoc(2);

			int docId1 = doc1.getID();
			int docId2 = doc2.getID();
			int docId3 = doc3.getID();

			// Make sure that nothing is returned before any manipulation.
			assertMoveIsEmpty(docId1, docId2);
			assertMoveIsEmpty(docId1, docId3);
			assertMoveIsEmpty(docId2, docId1);
			assertMoveIsEmpty(docId2, docId3);
			assertMoveIsEmpty(docId3, docId1);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n</moves>\n");

			MovesManager.Fragment leftFragment = movesManager.new Fragment();
			MovesManager.Fragment rightFragment = movesManager.new Fragment();

			// add a move and try all permutations of retrieval 
			leftFragment = movesManager.new Fragment(doc1, 10, 25);
			rightFragment = movesManager.new Fragment(doc2, 17, 27);
			movesManager.createMove(leftFragment, rightFragment);

			assertSingleMoveEntry(docId1, 10, 25, docId2, 17, 27);
			assertMoveIsEmpty(docId1, docId3);
			assertSingleMoveEntry(docId2, 17, 27, docId1, 10, 25);
			assertMoveIsEmpty(docId2, docId3);
			assertMoveIsEmpty(docId3, docId1);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file2.txt\" space2=\"original\" start2=\"17\" end2=\"27\" />\n</moves>\n");

			// add a move to a different pair and try all permutations of retrieval
			rightFragment = movesManager.new Fragment(doc3, 33, 43);
			movesManager.createMove(leftFragment, rightFragment);

			assertSingleMoveEntry(docId1, 10, 25, docId2, 17, 27);
			assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
			assertSingleMoveEntry(docId2, 17, 27, docId1, 10, 25);
			assertMoveIsEmpty(docId2, docId3);
			assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file2.txt\" space2=\"original\" start2=\"17\" end2=\"27\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n</moves>\n");

			// add a second move (in the opposite base/witness order)
			// Note that the returned moves are sorted by left hand side.
			leftFragment = movesManager.new Fragment(doc2, 48, 52);
			rightFragment = movesManager.new Fragment(doc1, 3, 8);
			movesManager.createMove(leftFragment, rightFragment);

			assertDoubleMoveEntry(docId1, 3, 8, 10, 25, docId2, 48, 52, 17, 27);
			assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
			assertDoubleMoveEntry(docId2, 17, 27, 48, 52, docId1, 10, 25, 3, 8);
			assertMoveIsEmpty(docId2, docId3);
			assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file2.txt\" space2=\"original\" start2=\"17\" end2=\"27\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"3\" end1=\"8\" doc2=\"file2.txt\" space2=\"original\" start2=\"48\" end2=\"52\" />\n</moves>\n");

			// change a move
			MoveList ml = movesManager.getAllMoves(docId1, docId2);
			MovesManager.FragmentPair fp = ml.get(0);
			leftFragment = movesManager.new Fragment(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE)-1, fp.first.getEndOffset(OffsetRange.Space.ACTIVE)-1);
			rightFragment = movesManager.new Fragment(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE)-1, fp.second.getEndOffset(OffsetRange.Space.ACTIVE)-1);
			movesManager.updateBlock(fp, leftFragment, rightFragment);

			assertDoubleMoveEntry(docId1, 2, 7, 10, 25, docId2, 47, 51, 17, 27);
			assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
			assertDoubleMoveEntry(docId2, 17, 27, 47, 51, docId1, 10, 25, 2, 7);
			assertMoveIsEmpty(docId2, docId3);
			assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file2.txt\" space2=\"original\" start2=\"17\" end2=\"27\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"2\" end1=\"7\" doc2=\"file2.txt\" space2=\"original\" start2=\"47\" end2=\"51\" />\n</moves>\n");

			// delete a move
			ml = movesManager.getAllMoves(docId1, docId2);
			movesManager.deleteMove(ml.get(0));

			assertSingleMoveEntry(docId1, 10, 25, docId2, 17, 27);
			assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
			assertSingleMoveEntry(docId2, 17, 27, docId1, 10, 25);
			assertMoveIsEmpty(docId2, docId3);
			assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file2.txt\" space2=\"original\" start2=\"17\" end2=\"27\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n</moves>\n");

			// remove a document
			documentManager.removeDocument(getDoc(1));	// this is the second document

			assertMoveIsEmpty(docId1, docId2);
			assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
			assertMoveIsEmpty(docId2, docId1);
			assertMoveIsEmpty(docId2, docId3);
			assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n</moves>\n");

			// put it back in
			addDoc(file2);
			docId2 = getDoc(2).getID();	// the previous second document is now in the third place.

			assertLastGoodCase(docId1, docId2, docId3);

			//
			// Error cases
			//

			// add an overlapping move
			boolean failed = false;
			try
			{
				leftFragment = movesManager.new Fragment(doc1, 14, 29);
				rightFragment = movesManager.new Fragment(doc3, 17, 27);
				movesManager.createMove(leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Overlapping move should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// add a move with a zero length
			failed = false;
			try
			{
				leftFragment = movesManager.new Fragment(doc1, 14, 14);
				rightFragment = movesManager.new Fragment(doc3, 17, 27);
				movesManager.createMove(leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Zero length move should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// add a move with a negative length
			failed = false;
			try
			{
				leftFragment = movesManager.new Fragment(doc1, 14, 29);
				rightFragment = movesManager.new Fragment(doc3, 17, 12);
				movesManager.createMove(leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Negative length move should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// add a move that is out of bounds
			failed = false;
			try
			{
				leftFragment = movesManager.new Fragment(doc1, 14, 1229);
				rightFragment = movesManager.new Fragment(doc3, 17, 27);
				movesManager.createMove(leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Out of bounds move should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// change a move that doesn't exist
			failed = false;
			try
			{
				leftFragment = movesManager.new Fragment(doc1, 14, 19);
				rightFragment = movesManager.new Fragment(doc3, 17, 27);
				fp = movesManager.getAllMoves(docId1, docId3).get(0);
				fp.first.resetDocument(doc2);
				movesManager.updateBlock(fp, leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Non-existent move should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// delete a move that doesn't exist
			failed = false;
			try
			{
				movesManager.deleteMove(fp);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Non-existent delete should have thrown an exception", true, failed);
			assertLastGoodCase(docId1, docId2, docId3);

			// change a move to one that overlaps
			leftFragment = movesManager.new Fragment(doc1, 30, 35);
			rightFragment = movesManager.new Fragment(doc3, 22, 26);
			movesManager.createMove(leftFragment, rightFragment);

			failed = false;
			try
			{
				fp = movesManager.getAllMoves(docId1, docId3).get(0);
				leftFragment = movesManager.new Fragment(doc1, 8, 35);
				movesManager.updateBlock(fp, leftFragment, rightFragment);
			}
			catch (LoggedException e) {
				failed = true;
			}
			assertEquals("Overlapped change should have thrown an exception", true, failed);
			assertMoveIsEmpty(docId1, docId2);
			assertDoubleMoveEntry(docId1, 10, 25, 30, 35, docId3, 33, 43, 22, 26);
			assertMoveIsEmpty(docId2, docId1);
			assertMoveIsEmpty(docId2, docId3);
			assertDoubleMoveEntry(docId3, 22, 26, 33, 43, docId1, 30, 35, 10, 25);
			assertMoveIsEmpty(docId3, docId2);
			assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"30\" end1=\"35\" doc2=\"file3.txt\" space2=\"original\" start2=\"22\" end2=\"26\" />\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n</moves>\n");
		}
		catch (LoggedException e) {
			fail();
		}
	}

	// This is what should be returned for all the error cases, because they shouldn't have changed the object
	private void assertLastGoodCase(int docId1, int docId2, int docId3) 
	{
		assertMoveIsEmpty(docId1, docId2);
		assertSingleMoveEntry(docId1, 10, 25, docId3, 33, 43);
		assertMoveIsEmpty(docId2, docId1);
		assertMoveIsEmpty(docId2, docId3);
		assertSingleMoveEntry(docId3, 33, 43, docId1, 10, 25);
		assertMoveIsEmpty(docId3, docId2);
		assertMovesManagerDump("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<moves>\n\t<move doc1=\"file1.txt\" space1=\"original\" start1=\"10\" end1=\"25\" doc2=\"file3.txt\" space2=\"original\" start2=\"33\" end2=\"43\" />\n</moves>\n");
	}
	
	private void assertSingleMoveEntry(int lhsDoc, int lhsOfs, int lhsEnd, int rhsDoc, int rhsOfs, int rhsEnd)
	{
		MoveList ml = movesManager.getAllMoves(lhsDoc, rhsDoc);
		assertEquals("Size of move list is wrong", 1, ml.size());
		FragmentPair fp = ml.get(0);
		assertMove(fp, lhsDoc, lhsOfs, lhsEnd, rhsDoc, rhsOfs, rhsEnd);
	}

	private void assertMove(FragmentPair fp, int lhsDoc, int lhsOfs, int lhsEnd, int rhsDoc, int rhsOfs, int rhsEnd)
	{
		assertFragment(fp.first, lhsDoc, lhsOfs, lhsEnd);
		assertFragment(fp.second, rhsDoc, rhsOfs, rhsEnd);
	}
	
	private void assertDoubleMoveEntry(int lhsDoc, int lhsOfs1, int lhsEnd1, int lhsOfs2, int lhsEnd2, 
			int rhsDoc, int rhsOfs1, int rhsEnd1, int rhsOfs2, int rhsEnd2)
	{
		MoveList ml = movesManager.getAllMoves(lhsDoc, rhsDoc);
		assertEquals("Size of move list is wrong", 2, ml.size());
		FragmentPair fp = ml.get(0);
		assertMove(fp, lhsDoc, lhsOfs1, lhsEnd1, rhsDoc, rhsOfs1, rhsEnd1);
		fp = ml.get(1);
		assertMove(fp, lhsDoc, lhsOfs2, lhsEnd2, rhsDoc, rhsOfs2, rhsEnd2);
	}

	private void assertMoveIsEmpty(int lhs, int rhs)
	{
		MoveList ml = movesManager.getAllMoves(lhs, rhs);
		assertEquals("Size of move list is wrong", 0, ml.size());
	}
	
	private void assertMovesManagerDump(String expected)
	{
		String dump = movesManager.serialize();
		assertEquals("The serialized value of block manager", expected, dump);
	}
	
	private void assertFragment(MovesManager.Fragment frag, int docId, int startIndex, int endIndex)
	{
		assertEquals("DocId", docId, frag.getDocumentID());
		assertEquals("StartIndex", startIndex, frag.getStartOffset(OffsetRange.Space.ACTIVE));
		assertEquals("EndIndex", endIndex, frag.getEndOffset(OffsetRange.Space.ACTIVE));
	}

	public void testLastWitnessLineCollateBug()
	{
		// The first two documents have been setup and collated by the time we get here.
		try
		{
			// get the collation for each document being the base first, before doing the move
			documentManager.removeDocument(getDoc(1));	// this is the second document
			documentManager.removeDocument(getDoc(0));	// this is the first document
			addDoc(file4);
			addDoc(file5);
			analyze("collatebaseline0");	// file1 is base, file2 is witness
			analyze("collatebaseline1");	// file2 is base, file1 is witness
		}
		catch( ReportedException e )
		{
			e.printStackTrace();
			fail();            
		}
	}
	
	public void testRecollateAfterMove()
	{
		// The first two documents have been setup and collated by the time we get here.
		documentManager.removeDocument(getDoc(0));
		documentManager.removeDocument(getDoc(0));
		addDoc(file6);
		addDoc(file7);
		
		try
		{
			LogHeader("Baseline");
			analyze("ram_baseline0");	// file6 is base, file7 is witness
			analyze("ram_baseline1");	// file7 is base, file6 is witness

			// Extra cases: 
			// In damozel, end a move with "stair?)" from 1855 MS. It acts differently if the "?)" is included in the move.
			// In damozel, in 1855 MS, select entire stanza "(Alas!...stair?)". There is no matching insert in the 1870 Proof.
			// In damozel, 1872 Fragment to 1855 MS. Match the entire fragment to where it goes. There are extra inserts and deletes.
			
			LogHeader("move to identical offsets on both sides");		// There shouldn't be inserted inserts and deletes when the moves overlap 
			createMove(0, 38, 71, 1, 40, 71);
			analyze("ram_move_to_identical_offsets_on_both_sides0");	// file6 is base, file7 is witness
			removeMove(0, 38, 71, 1, 40, 71);
			
			LogHeader("end move with a change and white space");	// the generated insert is too long
			createMove(0, 390, 414, 1, 118, 153);
			analyze("ram_end_move_with_a_change_and_white_space0");	// file6 is base, file7 is witness
			removeMove(0, 390, 414, 1, 118, 153);
			
			LogHeader("end move with multiple white space");	// an extra token is added to the generated insert
			createMove(0, 188, 223, 1, 673, 688);
			analyze("ram_end_move_with_multiple_white_space0");	// file6 is base, file7 is witness
			removeMove(0, 188, 223, 1, 673, 688);
			
			LogHeader("start move with a space");	// In a move, select a space as the first character, then the matching delete erroneously includes the word before.
			createMove(0, 43, 56, 1, 191, 204);
			analyze("ram_start_move_with_a_space0");	// file6 is base, file7 is witness
			removeMove(0, 43, 56, 1, 191, 204);
			
			LogHeader("extra delete bug");
			createMove(0, 521, 548, 1, 427, 633);
			analyze("ram_extra_delete_bug0");	// file6 is base, file7 is witness
			removeMove(0, 521, 548, 1, 427, 633);
			
			LogHeader("Create a move that completely encompasses a change.");
			createMove(0, 9, 18, 1, 75, 193);
			analyze("ram_encompass0");	// file6 is base, file7 is witness
			analyze("ram_encompass1");	// file7 is base, file6 is witness
			removeMove(0, 9, 18, 1, 75, 193);
			
			LogHeader("Create a move that overlaps the top of an insert.");
			createMove(0, 457, 485, 1, 180,308);
			analyze("ram_overlap_top0");	// file6 is base, file7 is witness
			analyze("ram_overlap_top1");	// file7 is base, file6 is witness
			removeMove(0, 457, 485, 1, 180,308);
			
			LogHeader("Create a move that overlaps the bottom of an insert.");
			createMove(0, 457, 485, 1, 566, 667);
			analyze("ram_overlap_bottom0");	// file6 is base, file7 is witness
			analyze("ram_overlap_bottom1");	// file7 is base, file6 is witness
			removeMove(0, 457, 485, 1, 566, 667);
			
			LogHeader("Create a move that is completely inside an insert.");
			createMove(0, 457, 485, 1, 281, 395);
			analyze("ram_inside_move0");	// file6 is base, file7 is witness
			analyze("ram_inside_move1");	// file7 is base, file6 is witness
			removeMove(0, 457, 485, 1, 281, 395);
			
			LogHeader("Create a move that is completely inside a change.");
			createMove(0, 121, 131, 1, 643, 655);
			analyze("ram_inside_change0");	// file6 is base, file7 is witness
			analyze("ram_inside_change1");	// file7 is base, file6 is witness
			removeMove(0, 121, 131, 1, 643, 655);

			LogHeader("Create a move that has an insert and a change");
			createMove(0, 280, 356, 1, 321, 350);
			analyze("ram_insert_change0");	// file6 is base, file7 is witness
			analyze("ram_insert_change1");	// file7 is base, file6 is witness
			removeMove(0, 280, 356, 1, 321, 350);

			analyze("ram_baseline0");	// file6 is base, file7 is witness
			analyze("ram_baseline1");	// file7 is base, file6 is witness
		}
		catch( ReportedException e )
		{
			e.printStackTrace();
			fail();            
		}
		catch( LoggedException e )
		{
			e.printStackTrace();
			fail();            
		}
	}
	
	public void testMove()
	{
		// The first two documents have been setup and collated by the time we get here.
		try
		{
			// get the collation for each document being the base first, before doing the move
			LogHeader("Baseline");
			analyze("baseline0");	// file1 is base, file2 is witness
			analyze("baseline00");	// file1 is both base and witness
			analyze("baseline1");	// file2 is base, file1 is witness
			analyze("baseline11");	// file2 is both base and witness
			
			// create the move and analyze the collation again
			LogHeader("Create normal move");
			createMove(0, 70, 94, 1, 14, 36);
			analyze("firstmove0");	// file1 is base, file2 is witness
			analyze("firstmove1");	// file2 is base, file1 is witness
		
			// add third document - shouldn't change the results; 3rd doc should not see move.
			LogHeader("Add third file");
	        addDoc(file3);
			analyze("firstmove0");	// file1 is base, file2 is witness
			analyze("firstmove1");	// file2 is base, file1 is witness
			analyze("compare13");	// file1 is base, file3 is witness
			analyze("compare23");	// file2 is base, file3 is witness
			analyze("compare12");	// file1 is base, file2 is witness
			analyze("compare21");	// file2 is base, file1 is witness
			
			// remove the second document -now file3 is the second doc 
			LogHeader("Remove file2.txt");
	        documentManager.removeDocument(getDoc(1));
			analyze("remove0");	// file1 is base, file3 is witness
			analyze("remove1");	// file3 is base, file1 is witness
			
			// add the second document back in - should not keep the move; file2 is now the third doc
			LogHeader("reload file2.txt");
			addDoc(file2);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness

			// create a move that doesn't match and analyze again
			LogHeader("Create a random, semantically meaningless move");
			createMove(0, 10, 20, 1, 18, 22);
			analyze("mismatch0");	// file1 is base, file3 is witness
			analyze("mismatch1");	// file3 is base, file1 is witness
			removeMove(0, 10, 20, 1, 18, 22);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness
			
			// create a move to the same place
			LogHeader("Create a move to the same place");
			createMove(0, 0, 10, 1, 0, 10);
			analyze("sameplace0");	// file1 is base, file3 is witness
			analyze("sameplace1");	// file3 is base, file1 is witness
			removeMove(0, 0, 10, 1, 0, 10);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness
			
			// create two moves
			LogHeader("create two moves");
			createMove(0, 10, 20, 1, 40, 62);
			createMove(0, 70, 92, 1, 14, 36);
			analyze("twomoves0");	// file1 is base, file3 is witness
			analyze("twomoves1");	// file3 is base, file1 is witness
			removeMove(0, 10, 20, 1, 40, 62);
			removeMove(0, 70, 92, 1, 14, 36);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness

			// move entire base doc to place in witness
			LogHeader("move entire base doc");
			createMove(0, 0, 93, 1, 14, 36);
			analyze("moveentire0");	// file1 is base, file2 is witness
			analyze("moveentire1");	// file2 is base, file1 is witness
			removeMove(0, 0, 93, 1, 14, 36);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness

			// create two moves that overlap
			LogHeader("create two moves that overlap");
			createMove(0, 10, 20, 1, 20, 30);
			createMove(0, 15, 25, 1, 25, 35);
			analyze("overlap0");	// file1 is base, file2 is witness
			analyze("overlap1");	// file2 is base, file1 is witness
			removeMove(0, 15, 25, 1, 25, 35);
			removeMove(0, 10, 20, 1, 20, 30);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness

			// create a zero-length base move
			LogHeader("create a zero-length base move");
			createMove(0, 10, 10, 1, 20, 30);
			analyze("zerolengthbase0");	// file1 is base, file2 is witness
			analyze("zerolengthbase1");	// file2 is base, file1 is witness
			removeMove(0, 10, 10, 1, 20, 30);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness
			
			// create a zero-length witness move
			LogHeader("create a zero-length witness move");
			createMove(0, 10, 20, 1, 20, 20);
			analyze("zerolengthwitness0");	// file1 is base, file2 is witness
			analyze("zerolengthwitness1");	// file2 is base, file1 is witness
			removeMove(0, 10, 20, 1, 20, 20);
			analyze("read2nd0");	// file1 is base, file3 is witness
			analyze("read2nd1");	// file3 is base, file1 is witness
			analyze("read2nd2");	// file1 is base, file2 is witness
			
			// Do moves between three documents
			LogHeader("Do moves between three documents");
			createMove(0, 70, 92, 1, 33, 55);
			createMove(0, 70, 92, 2, 14, 36);
			analyze("threeway12");	// file1 is base, file2 is witness
			analyze("threeway13");	// file1 is base, file3 is witness
			analyze("threeway21");	// file2 is base, file1 is witness
			analyze("threeway23");	// file2 is base, file3 is witness
			analyze("threeway31");	// file3 is base, file1 is witness
			analyze("threeway32");	// file3 is base, file2 is witness
		}
		catch( ReportedException e )
		{
			e.printStackTrace();
			fail();            
		}
		catch( LoggedException e )
		{
			e.printStackTrace();
			fail();            
		}
		
	}
	
	private void removeMove(int baseIndex, int baseOffset, int baseEnd, int witnessIndex, int witnessOffset, int witnessEnd) throws LoggedException
	{
		MovesManager.FragmentPair fp = createFragmentPair(baseIndex,
				baseOffset, baseEnd, witnessIndex, witnessOffset, witnessEnd);
		movesManager.deleteMove(fp);
	}

	private void createMove(int baseIndex, int baseOffset, int baseEnd, int witnessIndex, int witnessOffset, int witnessEnd) throws LoggedException
	{
		MovesManager.FragmentPair fp = createFragmentPair(baseIndex,
				baseOffset, baseEnd, witnessIndex, witnessOffset, witnessEnd);
		movesManager.createMove(fp.first, fp.second);
	}

	private MovesManager.FragmentPair createFragmentPair(int baseIndex,
			int baseOffset, int baseEnd, int witnessIndex, int witnessOffset,
			int witnessEnd)
	{
		JuxtaDocument doc = getDoc(baseIndex);
		JuxtaDocument doc2 =getDoc(witnessIndex);
		MovesManager.FragmentPair fp = MovesManager.newFragmentPair();
		fp.first.set(doc, baseOffset, baseEnd);
		fp.second.set(doc2, witnessOffset, witnessEnd);
		Log("MB: " + insertBrackets(doc.getDocumentText(), baseOffset, baseEnd)); 
		Log("MW: " + insertBrackets(doc2.getDocumentText(), witnessOffset, witnessEnd)); 
		return fp;
	}

	JuxtaDocument getDoc(int index)
	{
		LinkedList docs = documentManager.getDocumentList();
		return (JuxtaDocument)docs.get(index);
	}
	
	private void analyze(String strTestName) throws ReportedException
	{
  		String xmlFile = "src/edu/virginia/speclab/juxta/author/model/test/TestMoves.xml";
   		try
   		{
   			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
   			DocumentBuilder parser = factory.newDocumentBuilder();
   			Node document = parser.parse(xmlFile);
   			NodeList top = document.getChildNodes().item(0).getChildNodes();
   			if (top != null)
   			{
   				for (int i = 0; i < top.getLength(); i++)
   				{
   					Node node = top.item(i);

   					if ((node.getNodeType() == Node.ELEMENT_NODE)
   							&& (node.getNodeName().equals("test"))
   							&& getAttr(node, "id").equals(strTestName))
   					{
   						getDiffs(node);
   						return;
   					}
   				}
   			}
   		}
   		catch (SAXException e)
   		{
   			throw new ReportedException(e, xmlFile + " is not well formed.");
   		}
   		catch (IOException e)
   		{
   			throw new ReportedException( e, "IO Exception parsing document: " + xmlFile);
   		} 
   		catch (FactoryConfigurationError e)
   		{
   			throw new ReportedException( "Could not locate factory class.","Factory Configuration Error" );
   		} 
   		catch (ParserConfigurationException e)
   		{
   			throw new ReportedException( e, "Could not locate a JAXP Parser.");
   		}  	
 
	}

	static boolean createXml = false;	// set this to true to keep running the tests to the end so that the console will have all the results.
	static boolean debugXml = false;	// like createXml, except that it assumes the results have been created, so it tests them.
	void getDiffs(Node node) throws ReportedException
	{
		int baseIndex = Integer.parseInt(getAttr(node, "base"));
		int witnessIndex = Integer.parseInt(getAttr(node, "witness"));
		Log("<test id=\"" + getAttr(node, "id") + "\" base=\"" + baseIndex + "\" witness=\"" + witnessIndex + "\" >");

		LinkedList docs = documentManager.getDocumentList();
		JuxtaDocument baseDoc = (JuxtaDocument)docs.get(baseIndex);
		JuxtaDocument witnessDoc = (JuxtaDocument)docs.get(witnessIndex);
		String str = "";
		Collation collation = comparisonSet.getCollation(baseDoc);
		List list = collation.getDifferences(witnessDoc);
		if (list == null)
			list = new LinkedList();
		dumpAllDifferences(list, baseDoc.getDocumentText(), witnessDoc.getDocumentText());
		int count = 0;
		NodeList nlResult = node.getChildNodes(); 

		int numResults = (createXml?list.size():nlResult.getLength());
			
		for (int i = 0; i < numResults; ++i)
		{
			Node nodeResult = (createXml?null:nlResult.item(i));
			if (createXml || nodeResult.getNodeType() == Node.ELEMENT_NODE
					&& nodeResult.getNodeName().equals("result"))
			{
				if (count >= list.size())
				{
					DoAssert("Not enough difference entries returned", -1, list.size());
					break;
				}
				Difference difference = (Difference)list.get(count);
				++count;
				String strBaseFilename = documentManager.lookupDocument(difference.getBaseDocumentID()).getDocumentName();
				String strWitnessFilename = documentManager.lookupDocument(difference.getWitnessDocumentID()).getDocumentName();
				int iBaseOfs = difference.getOffset(Difference.BASE);
				int iBaseLen = difference.getLength(Difference.BASE);
				int iWitnessOfs = difference.getOffset(Difference.WITNESS);
				int iWitnessLen = difference.getLength(Difference.WITNESS);
				
				str = "\t<result ";
				str += "basename=\"" + strBaseFilename;
				str += "\" baseoffset=\"" + Integer.toString(iBaseOfs);
				str += "\" baselength=\"" + Integer.toString(iBaseLen);
				str += "\" witnessname=\"" + strWitnessFilename;
				str += "\" witnessoffset=\"" + Integer.toString(iWitnessOfs);
				str += "\" witnesslength=\"" + Integer.toString(iWitnessLen);
				str += "\" type=\"" +Difference.getTypeName(difference.getType());
				str += "\" distance=\"" + Integer.toString(difference.getDistance());
				str += "\" />";
				Log(str);

				if (!createXml)
				{
					DoAssert("", "", difference.testContents(Difference.getTypeValue(getAttr(nodeResult, "type")), getAttrInt(nodeResult, "baseoffset"), getAttrInt(nodeResult, "baselength"),
							getAttrInt(nodeResult, "witnessoffset"), getAttrInt(nodeResult, "witnesslength")));
					DoAssert("basename", getAttr(nodeResult, "basename"), strBaseFilename);
					DoAssert("witnessname", getAttr(nodeResult, "witnessname"), strWitnessFilename);
					DoAssert("distance", getAttr(nodeResult, "distance"), Integer.toString(difference.getDistance()));
				}
			}
		}
		DoAssert("count", count, (list == null)?0:list.size());

		Log("</test>");
	}

	public void dumpAllDifferences(List list, String baseText, String witnessText)
	{
		String str = "\n";
		for (Iterator i = list.iterator(); i.hasNext(); )
		{
			Difference difference = (Difference)i.next();
			str += difference.dumpContentsTruncated(baseText, witnessText);
		}
		Log(str);
	}
	
	class DebugLogger
	{
		private String fileName;
	   
	   public void initFileLogging(String fileName) throws IOException
	   {
		   this.fileName = fileName;
	      // purge old log on startup
	      File logFile = new File(fileName);
	      if (logFile.exists())
	      {
	         logFile.delete();
	      }
	   }
	   
	   private DebugLogger(String fileName)
	   {
		   try
		   {
		   if (fileName != null)
			   initFileLogging(fileName);
		   }
		   catch (IOException e)
		   {
			   System.out.println("Error creating log file");
		   }
		   
	   }
	   
	   private void append(String str)
	   {
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
				out.write(str + "\n");
				out.close();
				} catch (IOException e)
				{
					   System.out.println("Error creating log file");
				}
	   }
 	   public void log( String infoMsg)
	   {
 		   append(infoMsg);
 		   System.out.println(infoMsg);
	   }
	} 
	
	void LogHeader(String msg)
	{
		log.log("*******************"); 
		log.log(msg); 
		log.log("*******************"); 
	}
	
	void Log(String msg)
	{
		log.log(msg); 
	}
	
	void LogImportant(String msg)
	{
		log.log("###### " + msg); 
	}
	
	void DoAssert(String strMsg, int iExpected, int iFound)
	{
		DoAssert(strMsg, Integer.toString(iExpected), Integer.toString(iFound));
	}

	void DoAssert(String strMsg, String strExpected, String strFound)
	{
		if (debugXml)
		{
			if (!strExpected.equals(strFound))
				LogImportant("JUnit Failure: " + strMsg + " expected:" + strExpected + " but was: " + strFound);
		}
		else
			assertEquals(strMsg, strExpected, strFound);
			
	}
	String insertBrackets(String strSrc, int iStart, int iEnd)
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
   	String getAttr(Node node, String attr)
   	{
       	return node.getAttributes().getNamedItem(attr).getNodeValue();
   	}
   	int getAttrInt(Node node, String attr)
   	{
       	return Integer.parseInt(node.getAttributes().getNamedItem(attr).getNodeValue());
   	}
   	
   	public void tearDown()
	{
		(new File(file1)).delete();
		(new File(file2)).delete();
		(new File(file3)).delete();
		(new File(file4)).delete();
		(new File(file5)).delete();
		
		try
		{
			super.tearDown();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();            
		}
	}
	
	private void WriteFile(String filename, String contents)
	{
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write(contents);
            out.close();
        } catch (IOException e) {
			e.printStackTrace();
			fail();            
        }	
	}
	
	public void addDoc(String filename)
	{
        String name = filename.substring(filename.lastIndexOf('/') + 1);
        try
		{
        	JuxtaDocument document = documentManager.addDocument(name,filename,"UTF-8");
        	if( document != null )
        	{
        		comparisonSet.addCollation(document);            
        	}            
		}
		catch( LoggedException e )
		{
			e.printStackTrace();
			fail();            
		}
		
	}
}

