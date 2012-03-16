
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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.legacy.diff.document.DocumentModelFactory;
import edu.virginia.speclab.legacy.diff.token.Token;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * <code>DiffAlgorithm</code> provides an interface to the difference algorithm 
 * described below. Strings, text files, and <code>DocumentModel</code> objects
 * can be compared. The comparison results in a <code>DifferenceSet</code> object. 
 * 
 * diff Text file difference utility. ---- Copyright 1987, 1989 by Donald C.
 * Lindsay, School of Computer Science, Carnegie Mellon University. Copyright
 * 1982 by Symbionics. Use without fee is permitted when not for direct
 * commercial advantage, and when credit to the source is given. Other uses
 * require specific permission.
 * 
 * Converted from C to Java by Ian F. Darwin, http://www.darwinsys.com/,
 * January, 1997. Copyright 1997, Ian F. Darwin.
 * 
 * Modernized and extended by Nick Laiacona, http://www.patacriticism.org/, March, 2005. 
 * 
 * This program assumes that "oldfile" and "newfile" are text files. This module
 * create a DiferenceSet object which contains the results of the difference operation. 
 * 
 * About the Algorithm:
 * 
 * The algorithm is taken from Communications of the ACM, Apr78 (21, 4, 264-), "A Technique
 * for Isolating Differences Between Files." Ignoring I/O, and ignoring the
 * symbol table, it should take O(N) time. This implementation takes fixed
 * space, plus O(U) space for the symbol table (where U is the number of unique
 * lines). Methods exist to change the fixed space to O(N) space. Note that this
 * is not the only interesting file-difference algorithm. In general, different
 * algorithms draw different conclusions about the changes that have been made
 * to the oldfile. This algorithm is sometimes "more right", particularly since
 * it does not consider a block move to be an insertion and a (separate)
 * deletion. However, on some files it will be "less right". This is a
 * consequence of the fact that files may contain many identical lines
 * (particularly if they are program source). Each algorithm resolves the
 * ambiguity in its own way, and the resolution is never guaranteed to be
 * "right". However, it is often excellent. 
 * 
 * Notes from Ian F. Darwin:
 * 
 * This program is intended to be pedagogic. Specifically, this program was 
 * the basis of the Literate Programming column which appeared in the Communications 
 * of the ACM (CACM), in the June 1989 issue (32, 6, 740-755). By "pedagogic", I do 
 * not mean that the program is gracefully worded, or that it showcases language features 
 * or its algorithm. I also do not mean that it is highly accessible to beginners, or
 * that it is intended to be read in full, or in a particular order. Rather,
 * this program is an example of one professional's style of keeping things
 * organized and maintainable. The program would be better if the "print"
 * variables were wrapped into a struct. In general, grouping related variables
 * in this way improves documentation, and adds the ability to pass the group in
 * argument lists. This program is a de-engineered version of a program which
 * uses less memory and less time. The article points out that the "symbol"
 * arrays can be implemented as arrays of pointers to arrays, with dynamic
 * allocation of the subarrays. (In C, macros are very useful for hiding the
 * two-level accesses.) In Java, a Vector would be used. This allows an
 * extremely large value for MAXLINECOUNT, without dedicating fixed arrays. (The
 * "other" array can be allocated after the input phase, when the exact sizes
 * are known.) The only slow piece of code is the "strcmp" in the tree descent:
 * it can be speeded up by keeping a hash in the tree node, and only using
 * "strcmp" when two hashes happen to be equal.
 * 
 * Notes from Nick Laiacona:
 * 
 * This version was built as the comparison engine for the Juxta project at Applied 
 * Research in Patacriticism in the Media Studies Department of the University of Virginia.
 * I have refactored the orginal port to java to make this into a reusable 
 * library class that outputs a results object, rather than printing to the console. Tokenization
 * concerns have been seperated into an optional <code>DocumentModel</code> object, so that repeated difference
 * operations on the same file no longer require re-tokenizing the file. In addition, the output 
 * process has been made non-destructive to the file correlation data. The algorithm can now 
 * perform differences on a word-by-word basis in addition to a line-by-line basis. 
 * This allows us to compare large spans of prose text. Strings in memory can now be compared as
 * well as files. Unit tests are also now provided and logging has been added.
 *  
 * Change Log:
 *
 * 1Mar05 Nick Laiacona, modernized java version.
 * 1Jan97 Ian F. Darwin: first working rewrite in Java,
 * based entirely on D.C.Lindsay's reasonable C version. Changed comments from
 * /***************** to /**, shortened, added whitespace, used tabs more, etc.
 * 6jul89 D.C.Lindsay, CMU: fixed portability bug. Thanks, Gregg Wonderly. Just
 * changed "char ch" to "int ch". Also added comment about way to improve code.
 * 10jun89 D.C.Lindsay, CMU: posted version created. Copyright notice changed to
 * ACM style, and Dept. is now School. ACM article referenced in docn. 26sep87
 * D.C.Lindsay, CMU: publication version created. Condensed all 1982/83 change
 * log entries. Removed all command line options, and supporting code. This
 * simplified the input code (no case reduction etc). It also simplified the
 * symbol table, which was capable of remembering offsets into files (instead of
 * strings), and trusting (!) hash values to be unique. Removed dynamic
 * allocation of arrays: now fixed static arrays. Removed speed optimizations in
 * symtab package. Removed string compression/decompression code. Recoded to
 * Unix standards from old Lattice/MSDOS standards. (This affected only the
 * #include's and the IO.) Some renaming of variables, and rewording of
 * comments. 1982/83 D.C.Lindsay, Symbionics: created.
 * 
 * @author Nicholas C. Laiacona, modern Java version 
 * @author Ian F. Darwin, original Java version
 * @author D. C. Lindsay, C version (1982-1987)
 * @version Java version 1.0, 2005
 * 
 */
public class DiffAlgorithm
{
    // XXX: Caution, enabling logging significantly impacts performance
    public static final int VERBOSE_LOGGING = 1;
    
    private DocumentModel baseModel, witnessModel;
    private TokenizerSettings settings;

    private SymbolTable symbolTable;
    private Correlator correlator;
    private DifferenceCollector collector;
    private DifferenceSet differenceSet;
    
    public DiffAlgorithm()
    {
        settings = TokenizerSettings.getDefaultSettings();
    }
    
    public DiffAlgorithm( TokenizerSettings settings )
    {
        this.settings = settings;
    }
    
    public DifferenceSet diffDocuments( DocumentModel baseModel, DocumentModel witnessModel )
    {
        if( baseModel.getTokenizerSettings().equals(witnessModel.getTokenizerSettings()) == false ) 
        {
            SimpleLogger.logError("Documents must be tokenized the same way to be comparable.");
            return null;
        }

        this.baseModel = baseModel;
        this.witnessModel = witnessModel;

        return performDiff();
    }
    
    public DifferenceSet diffStrings( String oldString, String newString )
    {
        // create the document models
        baseModel = DocumentModelFactory.createFromString(oldString);        
        witnessModel = DocumentModelFactory.createFromString(newString);
        
        // tokenize the documents
        baseModel.tokenize(settings);
        witnessModel.tokenize(settings);
        
        return performDiff();
    }
    
    public DifferenceSet diffFiles( String oldFileName, String newFileName )
    {      
		DocumentModelFactory factory = new DocumentModelFactory("UTF-8"); 
        // create the document models
        baseModel = factory.createFromFile( new File(oldFileName) );
        witnessModel = factory.createFromFile( new File(newFileName) );

        return performDiff();
    }

    //  perform the actual diff algorithm 
    private DifferenceSet performDiff()
    {
        // create a common symbol table based on the two documents
        symbolTable = new SymbolTable(baseModel,witnessModel);

        // Correlate the contents of the two documents
        correlator = new Correlator(symbolTable);

        // Collect difference information from the correlation data
        collector = new DifferenceCollector(correlator);
        differenceSet = collector.getDifferenceSet();
        
        return differenceSet;
    }
    
    public void updateDifferenceSet(DifferenceSet differenceSet)
    {
    	this.differenceSet = differenceSet;
    }
    
	// Given the offset in one document, this finds the offset of that character in the other document.
	// If there is a perfect match, then it is easy to know what to return.
	// If it doesn't appear in the other document, then return it's insert point.
	// If it is part of a change, return -1.

	public int getCorrespondingWitnessOffset( int baseOffset, boolean getEnd )
	{
		int baseIndex = convertOffsetToIndex(baseOffset, Difference.BASE, !getEnd);
		
		FileInfo witnessFileInfo = correlator.getNewInfo();
		
		// The token didn't have an exact match, so it must be in the differenceSet. Look for it there.
		// The offset might appear between tokens. In that case, we need to move it a little to be on a token.
		// converting to a token and back will do that.
		baseOffset = convertIndexToOffset(convertOffsetToIndex(baseOffset, Difference.BASE, !getEnd), Difference.BASE);
		for (Iterator i = differenceSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = (Difference)i.next();
			if ((difference.getOffset(Difference.BASE) <= baseOffset) && (baseOffset <= difference.getOffset(Difference.BASE)+difference.getLength(Difference.BASE)))
			{
				if (getEnd)
					return difference.getOffset(Difference.WITNESS) + difference.getLength(Difference.WITNESS);
				else
					return difference.getOffset(Difference.WITNESS);
			}
		}
		
		// First, see if there is an exact match for the token. If so, return it.
		for( int witnessIndex=1; witnessIndex < witnessFileInfo.getSymbolCount()+1; witnessIndex++ ) {
			if( witnessFileInfo.getCrossIndex(witnessIndex) == baseIndex ) {
				if(getEnd)
					return convertIndexToOffset( witnessIndex, Difference.WITNESS ) + witnessFileInfo.getSymbol(witnessIndex+1).getSymbolLength();
				else
					return convertIndexToOffset( witnessIndex, Difference.WITNESS );
			}			
		}
		
		return -1;
	}

	public int getCorrespondingBaseOffset( int witnessOffset, boolean getEnd )
	{
		int witnessIndex = convertOffsetToIndex(witnessOffset, Difference.WITNESS, !getEnd);
		
		FileInfo baseFileInfo = correlator.getOldInfo();
		 
		// The token didn't have an exact match, so it must be in the differenceSet. Look for it there.
		// The offset might appear between tokens. In that case, we need to move it a little to be on a token.
		// converting to a token and back will do that.
		witnessOffset = convertIndexToOffset(convertOffsetToIndex(witnessOffset, Difference.WITNESS, !getEnd), Difference.WITNESS);
		for (Iterator i = differenceSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = (Difference)i.next();
			if ((difference.getOffset(Difference.WITNESS) <= witnessOffset) && (witnessOffset <= difference.getOffset(Difference.WITNESS)+difference.getLength(Difference.WITNESS)))
			{
				if (getEnd)
					return difference.getOffset(Difference.BASE) + difference.getLength(Difference.BASE);
				else
					return difference.getOffset(Difference.BASE);
			}
		}
		
		// First, see if there is an exact match for the token. If so, return it.
		for( int baseIndex=1; baseIndex < baseFileInfo.getSymbolCount()+1; baseIndex++ ) {
			if( baseFileInfo.getCrossIndex(baseIndex) == witnessIndex ) {
				if (getEnd)
					return convertIndexToOffset( baseIndex, Difference.BASE ) + baseFileInfo.getSymbol(baseIndex+1).getSymbolLength();
				else
					return convertIndexToOffset( baseIndex, Difference.BASE );
			}			
		}
		
		return -1;
	}
	
	private int convertIndexToOffset(int index, int type) {
		// For some reason, getDocumentOffset immediately subtracts one from the index, so we'll compensate by adding one first.
		if( type == Difference.BASE ) {
			return symbolTable.getOldInfo().getDocumentOffset(index+1);
		}
		else {
			return symbolTable.getNewInfo().getDocumentOffset(index+1);
		}
	}

	// Given an offset into the document, we want to scan the token table, seeing which token that falls in.
	// There is some space that doesn't belong to any token (white space and punctuation when that is ignored),
	// so in that case we need to know if we're interested in the token before that or the token after that.
	private int convertOffsetToIndex(int offset, int type, boolean useNext) {
		DocumentModel targetDocument = (type == Difference.BASE) ? baseModel : witnessModel;

		ArrayList tokenList = targetDocument.getTokenList();
		
		for( int i = 0; i < tokenList.size();  i++ ) {
			Token token = (Token) tokenList.get(i);
			int tokenEnd = token.getOffset() + token.getToken().length();
			// If we are looking earlier, then we don't want to match the offset itself, because that will look to the user as the next token.
			// Therefore, we'll trick it out by subtracting 1 so the test will appear as < instead of <=
			int fudge = (useNext?0:1);
			if((token.getOffset() <= offset-fudge) &&  (offset <= tokenEnd))
					return i;	// Exact match
			if (token.getOffset() >= offset) // We've passed the spot, so we must be in the crack between tokens
			{
				if (useNext)
				{
					if (i == tokenList.size()-1) return i;
					else return i;
				}
				else
					return i-1;
			}		
		}
		
		return tokenList.size()-1;
	}

}
