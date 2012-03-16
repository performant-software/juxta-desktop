package edu.virginia.speclab.juxta.author.model;

import java.io.File;
import java.util.Iterator;

import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaSession_1_3_1;
import edu.virginia.speclab.legacy.juxta.author.model.MovesManager.Fragment;
import edu.virginia.speclab.legacy.juxta.author.model.MovesManager.FragmentPair;
import edu.virginia.speclab.legacy.juxta.author.model.MovesManager.MoveList;


/**
 * Because of the major changes to the way Juxta parses XML in Juxta v1.4, we need to convert the offsets given for 
 * moves in pre v1.4 data files into offsets which are compatible with v1.4. We accomplish this by encoding the moves
 * into special location markers which are then parsed in the correct offset space by the v1.4 XML parser. We then
 * convert these markers back into moves and remove the markers.
 * @author nick
 *
 */
public class LegacyMoveDataConverter {
	
	public static String MOVE_MARKER = "mm";	
	public static String LEGACY_VERSION = "1.3.1";
	private static String TEMP_DIRECTORY = System.getProperty("java.io.tmpdir");
	
	// converts 1.3.1 JXT file to the most recent version, saves the resulting file in the temp dir, then 
	// loads the data from that file, setting the save target as the original file. This makes the operation
	// non-destructive, since the user may wish to view the file without saving it.
	public static JuxtaSession convertMoveData( File juxtaSessionFile, JuxtaAuthorFrame frame ) throws LoggedException {

		// load the file using the Juxta v1.3.1 code 
		JuxtaSession_1_3_1 session = JuxtaSession_1_3_1.createSession(juxtaSessionFile);
		MoveList moves = session.getDocumentManager().getMovesManager().getAllMoves();
		//List annotations = session.getAnnotationManager().getAnnotations(); 
		
		// convert moves to location markers
		convertMoves( session.getDocumentManager(), moves );	
		//convertAnnotations( session.getDocumentManager(), annotations );
		
    	// save the modified documents to a temp file 
    	File tempFileWithLocationMarkers = new File(TEMP_DIRECTORY+"/"+juxtaSessionFile.getName());
    	session.saveSession(tempFileWithLocationMarkers);
  
    	// load that file using v1.4 code, which picks up the move location markers
    	// and uses them to update the move list offsets
    	JuxtaSession convertedSession = JuxtaSession.createSession(tempFileWithLocationMarkers, frame, false);

    	// change the save file to point at the original data file
    	convertedSession.setSaveFile(juxtaSessionFile);
    
    	// return the converted juxta session 
    	return convertedSession;
	}	
	
	// go through the moves and convert them into location markers
	private static void convertMoves( edu.virginia.speclab.legacy.juxta.author.model.DocumentManager documentManager, MoveList moves ) {
			
		for (int i = 0; i < moves.size(); ++i)
		{
    		FragmentPair fragmentPair = moves.get(i);

    		LocationMarker firstLocationMarker = convertToLocationMarker( fragmentPair.first, i );    		    
    		LocationMarker secondLocationMarker = convertToLocationMarker( fragmentPair.second, i );    	

    		JuxtaDocument firstDoc = documentManager.lookupDocument(fragmentPair.first.docId);   
    		JuxtaDocument secondDoc = documentManager.lookupDocument(fragmentPair.second.docId);   

    		firstDoc.getLocationMarkerList().add(firstLocationMarker);
    		secondDoc.getLocationMarkerList().add(secondLocationMarker);
		}
	}
	
	private static LocationMarker convertToLocationMarker( Fragment fragment, int id ) {
		LocationMarker locationMarker = new LocationMarker( "move"+id, "", MOVE_MARKER, id, null, fragment.startIndex );
		locationMarker.setLength( fragment.endIndex - fragment.startIndex );
		return locationMarker;
	}
	
	public static boolean isLegacyVersion( String version ) {
		return version.equals(LEGACY_VERSION); 
	}

	// converts the move markers inserted by LegacyMoveDataConverter.convertMoveData() back into Moves with proper offsets in v1.4 land
	public static void convertMoveMarkers( JuxtaSession session ) throws LoggedException {

		edu.virginia.speclab.juxta.author.model.MovesManager.MoveList moves = session.getDocumentManager().getMovesManager().getMoves();
		
		for( Iterator i = session.getDocumentManager().getDocumentList().iterator(); i.hasNext(); ) {
			edu.virginia.speclab.juxta.author.model.JuxtaDocument document = (edu.virginia.speclab.juxta.author.model.JuxtaDocument) i.next();
			
			for( Iterator j = document.getLocationMarkerList().iterator(); j.hasNext(); ) {
				edu.virginia.speclab.diff.document.LocationMarker locationMarker = (edu.virginia.speclab.diff.document.LocationMarker) j.next();
				if( locationMarker.getLocationType().equals(MOVE_MARKER) ) {
					edu.virginia.speclab.juxta.author.model.MovesManager.FragmentPair fragmentPair = moves.get(locationMarker.getNumber());
					
					// determine which fragment pertains to the current document 
					edu.virginia.speclab.juxta.author.model.MovesManager.Fragment currentFragment = null;
					if( fragmentPair.first.getDocumentID() == document.getID() ) {
						currentFragment = fragmentPair.first;
					} else if( fragmentPair.second.getDocumentID() == document.getID() ) {
						currentFragment = fragmentPair.second;
					}

					// if we have found a fragment that pertains to the current document, update the move the fragment describes
					// to use the offsets from the move location marker.
					if( currentFragment != null ) {
						currentFragment.set(locationMarker.getStartOffset(Space.ACTIVE), locationMarker.getEndOffset(Space.ACTIVE), Space.ACTIVE);
					} else {
						throw new LoggedException("convertMoveMarkers: Unable to match fragment to move for document: "+document.getDocumentName());						
					}										
				}
			}
			
		}
	}

}
