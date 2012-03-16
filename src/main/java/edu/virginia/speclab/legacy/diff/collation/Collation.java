/*
 * Created on Feb 24, 2005
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

package edu.virginia.speclab.legacy.diff.collation;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.legacy.diff.DifferenceSet;
import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.util.IntPair;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * <code>Collation</code> collates <code>DifferenceSet</code> objects into a single
 * collection. Builds histogram data as difference are added.
 * 
 * @author Nick
 */
public class Collation implements Serializable
{
    private byte[] histogramData;
    private int baseDocumentID; // serialized ID
    private LinkedList differenceMap;
    private LinkedList moves;
    
    private HashSet collationFilter;
    private HashSet characterCounts;
    
    private int minChangeDistance;
        
    public Collation() 
    {
    	differenceMap = new LinkedList();       
    	moves = new LinkedList();       
    	collationFilter = new HashSet();
    	characterCounts = new HashSet();
    	minChangeDistance = 0;
    }

    public Collation( DocumentModel baseDocument )
    {
        this();
        initBaseDocument(baseDocument);
    }
    
    // add the difference to the hash map
    private void addDifference( Difference difference )
    {               
        influenceHistogram(difference);
        countChangedCharacters(difference);
        differenceMap.add(difference);
    }
    
    public int getNumberOfDifferences()
    {
        if( differenceMap == null ) return 0;
        else return differenceMap.size();
    }
    
    private void countChangedCharacters(Difference difference)
    {
        if( difference == null ) return;
        
        int documentID = difference.getWitnessDocumentID();

        // get the existing character count record
        CharacterCount charCount = getCharacterCountRecord(documentID);
        
        // create a new record if one does not exist
        if( charCount == null )
        {
            charCount = new CharacterCount(documentID);
            characterCounts.add(charCount);
        }
        
        int length;
        // if this is a change, take the larger of the two versions as the size of the change
        if( difference.getType() == Difference.CHANGE )
        {
            int baseLength = difference.getLength(Difference.BASE);
            int witnessLength = difference.getLength(Difference.WITNESS);
                        
            length = (baseLength>witnessLength) ? baseLength : witnessLength;
        }
        // if this is an insertion, use the length of the inserted text
        else if( difference.getType() == Difference.INSERT )
        {
            length = difference.getLength(Difference.WITNESS);
        }
        // otherwise, use the length of the effected text in base
        else
        {
            length = difference.getLength(Difference.BASE);    
        }

        // add the number of characters changed
        charCount.addToCount(length);
    }
    
    private CharacterCount getCharacterCountRecord( int documentID )
    {
        for( Iterator i = characterCounts.iterator(); i.hasNext(); )
        {
            CharacterCount charCount = (CharacterCount) i.next();
            if( documentID == charCount.getDocumentID() )
            {
                return charCount;
            }            
        }
        
        return null;
    }
    
    /**
     * Returns the number of characters of text that have been changed between the two 
     * documents. This number may be larger then the length of the base document, as it 
     * counts inserted or expanded text.
     * @param document
     * @return
     */
    public int getCharacterCount( DocumentModel document )
    {
        CharacterCount charCount = getCharacterCountRecord(document.getID());
        
        if( charCount != null )
        {
            return charCount.getCount();
        }
        else
        {
            return 0;
        }        
    }

    private void influenceHistogram( Difference difference )
    {
        if( difference.getDistance() < minChangeDistance ||
            containsDocumentID( collationFilter, difference.getWitnessDocumentID()) ) return;
        
        int startPosition, endPosition;

        if( difference.getType() == Difference.INSERT )
        {
            int offset = difference.getOffset(Difference.BASE);
            
            // if this is an INSERT, give it a length of one so it is visible 
            if( offset+1 < histogramData.length )
            {
                startPosition = offset;
                endPosition = offset+1;
            }
            else if( offset != 0 )
            {
                // if we are up against the end of the document, go backwards
                startPosition = offset-1;
                endPosition = offset;
            }
            else
            {
                // zero length file? punt.
                startPosition=endPosition=0;
            }
        }
        else
        {
            startPosition = difference.getOffset(Difference.BASE);
            endPosition = difference.getOffset(Difference.BASE)+difference.getLength(Difference.BASE);
        }
        
        if ((startPosition < 0) || (endPosition < 0) || (histogramData == null) || (histogramData.length < endPosition))
        	return;	// prevent an out of bounds case
        
        for( int i = startPosition; i < endPosition; i++ )
        {
            histogramData[i]++;
        }       
    }
    
    private void influenceHistogram( IntPair range )
    {
        for( int i = range.x; i < range.y; i++ )
        {
            histogramData[i]++;
        }       
    }
    
    public int getDifferenceFrequency( int offset )
    {
        if( histogramData != null && offset < histogramData.length && offset >= 0 ) 
            return histogramData[offset];
        else 
        {
            return 0;
        }
            
    }

    /**
     * Returns a list of differences in this collation for which the specified document is the
     * witness text. 
     * @param witness The document to search for.
     * @return A list of <code>Difference</code> objects.
     */
    public List getDifferences( DocumentModel witness )
    {
        if( collationFilter.contains(witness) ) return null;
        
        LinkedList differenceList = new LinkedList();
        
        for( Iterator i = differenceMap.iterator(); i.hasNext(); )
        {
            Difference difference = (Difference) i.next(); 
            
            if( difference.getWitnessDocumentID() == witness.getID() )                
            {
                differenceList.add(difference);
            }
        }
        
        if( differenceList.size() > 0 ) return differenceList;
        else return null;
    }
	
	public static boolean containsOffset( Difference difference, int offset )
	{
        int startPosition, endPosition;
        
        if( difference.getType() == Difference.INSERT )
        {
            startPosition = difference.getOffset(Difference.BASE);
            endPosition = difference.getOffset(Difference.BASE)+1;                    
        }
        else
        {
            startPosition = difference.getOffset(Difference.BASE);
            endPosition = difference.getOffset(Difference.BASE)+difference.getLength(Difference.BASE);                
        }
        
        if( offset >= startPosition && offset < endPosition )
        {
            return true;
        }
        else
		{
			return false;
		}
	}
    
    public List getDifferences( int offset )
    {
        if( differenceMap == null ) return null;
        
        LinkedList differenceList = new LinkedList();
        
        for( Iterator i = differenceMap.iterator(); i.hasNext(); )
        {
            Difference difference = (Difference) i.next(); 
            
            if( difference.getDistance() >= minChangeDistance &&
                !containsDocumentID( collationFilter, difference.getWitnessDocumentID() ) &&
                containsOffset(difference,offset) )
            {
                differenceList.add(difference);
            }
        }
        
        if( differenceList.size() > 0 ) return differenceList;
        else return null;
    }
    
    private boolean containsDocumentID( Set documentList, int id )
    {
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            DocumentModel document = (DocumentModel) i.next();
            if( id == document.getID() ) 
                return true;            
        }
        
        return false;
    }
    
    
    /**
     * Add this set of differences to the collation, all difference sets 
     * must share the same base text.
     * @param differenceSet the DifferenceSet to add to the collation
     */
    public void addDifferenceSet( DifferenceSet differenceSet )
    {
       // if we don't already have a base text, this is it
       if( baseDocumentID == 0 )
       {
           DocumentModel baseDocument = differenceSet.getBaseDocument();
           initBaseDocument(baseDocument);
       }
       // if we do, make sure it matches this one
       else if( baseDocumentID != differenceSet.getBaseDocument().getID() )
       {
           SimpleLogger.logError( "Unable to add difference set to collation, base texts differ: "
                                   +baseDocumentID+","+differenceSet.getBaseDocument().getID());
           return;
       }
           
       // go through the list of differences and add them to the collation
       List setList = differenceSet.getDifferenceList();
       for( Iterator i = setList.iterator(); i.hasNext(); )
       {
           Difference difference = (Difference) i.next();
           addDifference(difference);           
       }
    }

    private void initBaseDocument(DocumentModel baseDocument)
    {
        if( baseDocument != null )
        {
            this.baseDocumentID = baseDocument.getID();            
            histogramData = new byte[baseDocument.getDocumentText().length()+1];
        }
    }
  
    public HashSet getCollationFilter()
    {
        return collationFilter;
    }
    
    public void setCollationFilter(HashSet collationFilter)
    {
        this.collationFilter = new HashSet();
        
        if( collationFilter != null )
        {
            this.collationFilter.addAll(collationFilter);            
        }
        
        regenerateHistogram();
    }
    
    public void removeWitness( DocumentModel witness )
    {
        LinkedList removeList = new LinkedList();
        
        for( Iterator i = differenceMap.iterator(); i.hasNext(); )
        {
            Difference difference = (Difference) i.next(); 
            
            if( difference.getWitnessDocumentID() == witness.getID() )
            {
                removeList.add(difference);
            }
        }

        for( Iterator i = removeList.iterator(); i.hasNext(); )
        {
            Difference difference = (Difference) i.next(); 
            differenceMap.remove(difference);
        }
    }

    private void regenerateHistogram()
    {
        clearHistogram();
        
        for( Iterator i = differenceMap.iterator(); i.hasNext(); )
        {
            Difference difference = (Difference) i.next();
            influenceHistogram(difference);
        }
        for( Iterator i = moves.iterator(); i.hasNext(); )
        {
            IntPair ip = (IntPair) i.next();
            influenceHistogram(ip);
        }
    }

    private void clearHistogram()
    {
        if( histogramData == null ) return;
        
        int length = histogramData.length;
        for( int i = 0; i < length; i++ )
        {
            histogramData[i] = 0;
        }            
    }
    
    private class CharacterCount implements Serializable
    {
        private int documentID;
        private int count;
        
        public CharacterCount( int documentID )
        {
            this.documentID = documentID;
        }
        
        public void addToCount( int added )
        {
            count += added;
        }

        public int getCount()
        {
            return count;
        }

        public int getDocumentID()
        {
            return documentID;
        }
    }

    public byte[] getHistogramData()
    {
        return histogramData;
    }
    
    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException
    {
        minChangeDistance = stream.readInt();
        baseDocumentID = stream.readInt();
        histogramData = (byte[]) stream.readObject();
        differenceMap = (LinkedList) stream.readObject();
        characterCounts = (HashSet) stream.readObject();
        moves = (LinkedList) stream.readObject();
        
        collationFilter = new HashSet(); 
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException
    {
        stream.writeInt(minChangeDistance);
        stream.writeInt(baseDocumentID);
        stream.writeObject(histogramData);
        stream.writeObject(differenceMap);
        stream.writeObject(characterCounts);
        stream.writeObject(moves);
    }

    public int getBaseDocumentID()
    {
        return baseDocumentID;
    }

    public int getMinChangeDistance()
    {
        return minChangeDistance;
    }
    

    public void setMinChangeDistance(int minChangeDistance)
    {
        this.minChangeDistance = minChangeDistance;
        regenerateHistogram();
    }

    // This saves the set of moves so that the histogram works correctly. The moves aren't needed at this point otherwise.
	public void addMove(int startIndex, int endIndex)
	{
		IntPair ip = new IntPair(startIndex, endIndex);
		moves.add(ip);
		influenceHistogram(ip);
	}
    

}
