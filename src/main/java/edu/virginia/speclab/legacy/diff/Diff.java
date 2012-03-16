/*
 * Created on Feb 3, 2005
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

import edu.virginia.speclab.legacy.diff.document.DocumentModel;


/**
 * @author Nick
 * 
 * A simple testbed application for diff package.
 */
public class Diff
{
    public static void viewDifferenceList( DifferenceSet differenceSet )
    {
        DocumentModel baseDocument = differenceSet.getBaseDocument();
        DocumentModel witnessDocument = differenceSet.getWitnessDocument();
        LinkedList diffList = differenceSet.getDifferenceList();

        println(">>>> Difference of file \"" + baseDocument.getDocumentName() + "\" and file \""
                + witnessDocument.getDocumentName() + "\".\n");

        for( Iterator i = diffList.iterator(); i.hasNext(); )
        {
           Difference difference = (Difference) i.next();
           
           switch( difference.getType() )
           {           
               case Difference.CHANGE:
                   println( ">>>>>> CHANGED FROM:" );
                   printLine( difference, baseDocument, true );
                   println( ">>>>>> CHANGED TO:" );
                   printLine( difference, witnessDocument, false );
                   println( "distance= "+difference.getDistance() );
                   break;
                   
               case Difference.DELETE:
                   println( ">>>>>> DELETED:" );
               	   printLine( difference, baseDocument, true );
                   break;
                   
               case Difference.INSERT:
                   println( ">>>>>> INSERTED:" );
                   printLine( difference, witnessDocument, false );                   
                   break;
                   
               case Difference.MOVE:
                   int witnessOffset = difference.getOffset(Difference.WITNESS);
                   println( ">>>>>> MOVED TO OFFSET "+witnessOffset+":" );
                   printLine( difference, baseDocument, true );
                   break;
                                
               case Difference.NONE:
               default:
                   println(" >>>>> ERROR: Unidentified change type id:"+ difference.getType() );
                   break;
           }           
                      
        }   

        if( diffList.size() > 0 )
            println(">>>> End of differences.");
        else
            println(">>>> Files are identical.");

    }
    
    private static void printLine( Difference difference, DocumentModel document, boolean isBaseDocument )
    {
        String text;
        if( isBaseDocument )
        {
            text = document.getSubString( difference.getOffset(Difference.BASE), difference.getLength(Difference.BASE) );
        }
        else
        {
            text = document.getSubString( difference.getOffset(Difference.WITNESS), difference.getLength(Difference.WITNESS) );
        }

        println(text);
    }
    
    private static void println( String string )
    {
        System.out.println( string );
    }

    public static void main(String[] args)
    {
        // TODO: fix and unstub
        
//        SimpleLogger.initConsoleLogging();
//        
//        // perform the diff calculation 
//        DiffAlgorithm diff = new DiffAlgorithm(); 
//        //DifferenceSet differenceSet = diff.diffFiles( "data//tomsawyer.ch1.etext.txt", "data//tomsawyer.ch1.gutten.txt", false );             
//        DifferenceSet differenceSet = diff.diffFiles( "data//test1.txt", "data//test2.txt", true );
//        viewDifferenceList( differenceSet );        
    }
}
