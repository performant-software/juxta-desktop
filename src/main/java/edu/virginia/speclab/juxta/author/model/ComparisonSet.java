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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.MultiPassDiff;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.MovesManager.MoveList;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class represents the set of documents under comparison in the current
 * session. It manages the collation process and is the appropriate
 * access point for restoring collations from disk. It can calculate 
 * collation data synchronously or asynchronously. 
 *  
 * @author Nick
 */
public class ComparisonSet implements MovesManagerListener
{
    private DocumentManager documentManager;
    private TokenizerSettings tokenizerSettings;
    
    // background loading
    private BackgroundLoader backgroundLoader; 
    private boolean backgroundLoad;
    private LinkedList progressListeners;    
    private boolean loadComplete;
    
    private LinkedList loaderCallbacks;
    private static final JsonFactory JSON = new JsonFactory();

	/**
	 * Create a comparison set for the documents managed by the specified 
	 * <code>DocumentManager</code>
	 * @param documentManager The initialized <code>DocumentManager</code>
	 * @param backgroundLoad If true, load collations on another thread.
	 */
    public ComparisonSet( DocumentManager documentManager, boolean backgroundLoad ) 
    {
        this.documentManager = documentManager;
        this.backgroundLoad = backgroundLoad;
        progressListeners = new LinkedList();
        loaderCallbacks = new LinkedList();
        this.tokenizerSettings = TokenizerSettings.getDefaultSettings();
    }
    
	/**
	 * Starts up the collation loader, working on the set of documents provided. This
	 * call returns immediately in asynchronous mode and returns when loading
	 * is complete in asynchronous mode. Add a <code>LoaderListener</code> to 
	 * this object to be notified when asynchronous loading is complete.
	 * @param documentList A list of <code>JuxtaDocument</code> objects.
	 * @throws ReportedException If a problem is encountered during collation. 
	 */
    public void startLoader( LinkedList documentList ) throws ReportedException
    {
        if( backgroundLoader != null ) 
        {
            SimpleLogger.logError("attempted to start loader when already running.");
            return; 
        }

        // if there aren't any, we are done
        if( documentList == null || documentList.size() == 0)
        {
            loadComplete = true;
            fireLoadingComplete();
            return;
        }        
        
        // clear the complete flag
        loadComplete = false;
        
        // start collating in background 
        if( backgroundLoad )
        {            
            backgroundLoader = new BackgroundLoader(documentList);            
            backgroundLoader.start();            
        }
        // or just do it now
        else
        {
            for( Iterator i = documentList.iterator(); i.hasNext(); )
            {
                JuxtaDocument baseText = (JuxtaDocument) i.next();
                
                if( !documentManager.collationCacheExists(baseText) )
                {
                    addDocument(baseText);
                }
            }
     
            loadComplete = true;
            fireLoadingComplete();
        }
    }
    
	/**
	 * Add a listener to track the progress of background collation.
	 * @param listener The listener to callback to.
	 */
    public void addProgressListener( ProgressListener listener )
    {
		// inform this listener of documents that have already loaded 
		List documentList = documentManager.getDocumentList();
		for( Iterator i = documentList.iterator(); i.hasNext(); )
		{
			JuxtaDocument document = (JuxtaDocument)i.next();
			
			if( documentManager.collationCacheExists(document) )
			{
				listener.collationCompleted(document);
			}
		}
		
		progressListeners.add(listener);
    }
    
	/**
	 * Removes the specified progress listener from the callback list. 
	 * @param listener The listener to remove.
	 */
    public void removeProgressListener( ProgressListener listener )
    {
        progressListeners.remove(listener);
    }
    
    private class ProgressUpdater
    {
    	private float increment = 0f;
    	private float progress = 0f;
    	private JuxtaDocument baseText = null;
    	
    	public ProgressUpdater(JuxtaDocument baseText, int numWitnesses)
    	{
    		this.baseText = baseText;
    		if (numWitnesses > 0)
                increment = 1.0f / (float)numWitnesses;
    	}
    	public void update()
    	{
            progress += increment;
            updateProgressListeners();			            
    	}
    	public void finished()
    	{
    		progress = 1.0f;
    		updateProgressListeners();
    	}
    	public void reset()
    	{
    		progress = 0f;
    		updateProgressListeners();
    	}
    	private void updateProgressListeners()
    	{
    		for( Iterator i = progressListeners.iterator(); i.hasNext(); )
    		{
    			ProgressListener progressListener = (ProgressListener) i.next();
    			if( progress < 1.0f )
    			{
    				if( progress <= 0f ) progressListener.collationStarted(baseText); 
    				else progressListener.updateProgress(baseText,progress);
    			}
    			else progressListener.collationCompleted(baseText);
    		}
    	}
    }

    private void fireLoadingComplete()
    {
        for( Iterator i = loaderCallbacks.iterator(); i.hasNext(); )
        {
            LoaderCallBack listener = (LoaderCallBack) i.next();
            listener.loadingComplete();
        }
		
		loaderCallbacks.clear();
    }
    
//    private Collation performCollation( JuxtaDocument baseText )
//    {
//        Collation collation = new Collation(baseText);
//        
//        List documentList = new LinkedList( documentManager.getDocumentList() );
//        ProgressUpdater progressUpdater = new ProgressUpdater(baseText, documentList.size());
//        
//        // tokenize the text using the current settings
//        baseText.tokenize(tokenizerSettings);
//        
//        // compare this document to every other
//        for( Iterator i = documentList.iterator(); i.hasNext(); )
//        {            
//            JuxtaDocument witnessText = (JuxtaDocument) i.next();
//        
//            addWitnessToCollation(collation, baseText, witnessText);
//
//            // update progress listeners
//            progressUpdater.update();
//			
//			// interrupt on stop command 
//			if( backgroundLoader != null && backgroundLoader.isStopping() ) break;
//        }
//        
//        // we're done
//        progressUpdater.finished();
//
//        // release the token cache if there is one
//		baseText.releaseTokenTable();
//        
//        return collation;        
//    }
    
	public void movesChanged(MovesManager movesManager)
	{
		// TODO Optimize this by only re-collating the particular document pair that changed.
		try {
			reset();
		} catch (LoggedException e) {
			SimpleLogger.logError(e.getMessage());
		}
	}

	private Collation performMovesCollation( JuxtaDocument baseText )
    {
    	// The user may have created a series of moves where they match up sections of the documents.
    	// They will have left a number of sections unmatched and there is some interpretation of where the
    	// unmatched sections go.
        Collation collation = new Collation(baseText);
    	MovesManager movesManager = documentManager.getMovesManager();
        List documentList = new LinkedList( documentManager.getDocumentList() );        
        ProgressUpdater progressUpdater = new ProgressUpdater(baseText, documentList.size());

    	// First stab at the problem: Only collate the moves, the unblocked text are all inserts and deletes.
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {   
        	JuxtaDocument witnessText = (JuxtaDocument) i.next();
        	
        	// Don't collate the base to the base
        	if (baseText.getID() == witnessText.getID())
        		continue;
        	
        	MovesManager.MoveList moveList = movesManager.getAllMoves(baseText.getID(), witnessText.getID());
     		DifferenceSet differenceSet = new DifferenceSet();
    		differenceSet.setBaseDocument(baseText);
    		differenceSet.setWitnessDocument(witnessText);
    		differenceSet.setNumberOfSymbols(0);
      	
       		/// This call may have added inserts and deletes that overlap a move. So, we have to do the step of weeding out the moves again.
       		docsAreDiffedNormallyThenBlocksAreRemoved(differenceSet, baseText, witnessText, moveList);
    		DifferenceSet fullDifferenceSet = differenceSet;
    		differenceSet = new DifferenceSet();
    		differenceSet.setBaseDocument(baseText);
    		differenceSet.setWitnessDocument(witnessText);
    		differenceSet.setNumberOfSymbols(0);
    	    truncateDifferencesIntersectingMoves(fullDifferenceSet, differenceSet, moveList);
        	
        	for (int j = 0; j < moveList.size(); ++j)
        	{
        		MovesManager.FragmentPair fp = moveList.get(j);
        		DocumentModel baseBlockDocument = new DocumentModel(baseText, fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
        		DocumentModel witnessBlockDocument = new DocumentModel(witnessText, fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
        		addBlockToDifferenceSet( differenceSet,baseBlockDocument, witnessBlockDocument );
        		collation.addMove(fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
        	}
            collation.addDifferenceSet(differenceSet);
        	progressUpdater.update();
        }

        progressUpdater.finished();
        return collation;     
    }

	private boolean isBetween(int x, int lower, int upper)
	{
		if((lower <= x) && (x <= upper))
			return true;
		return false;
	}
	
	private void docsAreDiffedNormallyThenBlocksAreRemoved(DifferenceSet differenceSet, JuxtaDocument baseText,
			JuxtaDocument witnessText, MoveList moveList)
	{
		baseText.tokenize(tokenizerSettings);
		witnessText.tokenize(tokenizerSettings);
	    MultiPassDiff mpd = new MultiPassDiff(baseText, witnessText);
	    DifferenceSet fullDifferenceSet = mpd.getDifferenceSet();

	    // fullDifferenceSet has all the differences as if there are no moves. We want to delete all the differences
	    // that are inside a move, and we want to truncate all differences that overlap a move.
	    truncateDifferencesIntersectingMoves(fullDifferenceSet, differenceSet, moveList);
		
		for (int j = 0; j < moveList.size(); ++j)
		{
			MovesManager.FragmentPair fp = moveList.get(j);
			// Now, we need to find the area that was originally matched with the move and add some inserts for that.
			int ofs = mpd.getBaseOffset(fp.second.getStartOffset(OffsetRange.Space.ACTIVE), false);
			int end = mpd.getBaseOffset(fp.second.getEndOffset(OffsetRange.Space.ACTIVE), true);
			if ((ofs >= 0) && (end >= 0) && (end > ofs))
			{
				Difference deleteDiff = new Difference(baseText.getID(), witnessText.getID(), Difference.DELETE);
				deleteDiff.setBaseOffset(ofs);
				deleteDiff.setBaseTextLength(end-ofs);
				deleteDiff.setWitnessOffset(fp.second.getStartOffset(OffsetRange.Space.ACTIVE));
				deleteDiff.setWitnessTextLength(0);
				deleteDiff.setDistance(Integer.MAX_VALUE);
				differenceSet.addDifference(deleteDiff);
			}

			ofs = mpd.getWitnessOffset(fp.first.getStartOffset(OffsetRange.Space.ACTIVE), false);
			end = mpd.getWitnessOffset(fp.first.getEndOffset(OffsetRange.Space.ACTIVE), true);
			if ((ofs >= 0) && (end >= 0) && (end > ofs))
			{
				Difference insertDiff = new Difference(baseText.getID(), witnessText.getID(), Difference.INSERT);
				insertDiff.setWitnessOffset(ofs);
				insertDiff.setWitnessTextLength(end-ofs);
				insertDiff.setBaseOffset(fp.first.getStartOffset(OffsetRange.Space.ACTIVE));
				insertDiff.setBaseTextLength(0);
				insertDiff.setDistance(Integer.MAX_VALUE);
				differenceSet.addDifference(insertDiff);
			}
		}
		
	    baseText.releaseTokenTable();
	    witnessText.releaseTokenTable();
	}

	private void truncateDifferencesIntersectingMoves(
			DifferenceSet fullDifferenceSet, DifferenceSet differenceSet,
			MoveList moveList) {
		// If a move is totally inside a difference, then we truncate it, and create an INSERT/DELETE difference for the portion below the move.
	    // Note that there could be two moves inside a single difference.
		for( Iterator i = fullDifferenceSet.getDifferenceList().iterator(); i.hasNext(); )
		{
			Difference difference = ((Difference) i.next()).duplicate();

			int baseStart = difference.getOffset(Difference.BASE);
			int baseEnd = baseStart + difference.getLength(Difference.BASE);
			int witnessStart = difference.getOffset(Difference.WITNESS);
			int witnessEnd = witnessStart + difference.getLength(Difference.WITNESS);

			boolean skip = false;
			for (int j = 0; j < moveList.size(); ++j)
			{
				MovesManager.FragmentPair fp = moveList.get(j);
				
				// is the difference of either of the sides completely inside a move? If so, we just want to ignore it.
				if ((difference.getType() != Difference.INSERT) && isBetween(baseStart, fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE)) && isBetween(baseEnd, fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE)))
					skip = true;
				if ((difference.getType() != Difference.DELETE) && isBetween(witnessStart, fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE)) && isBetween(witnessEnd, fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE)))
					skip = true;
				
				if (skip)
					break;

				// if there is an overlap, adjust the end points. We ignore the base on INSERT types, and ignore the witness on DELETE types, but CHANGE types use both.
				if (difference.getType() != Difference.INSERT)
				{
					adjustDiffAroundMove(differenceSet, difference, true,
							baseStart, baseEnd, difference.getLength(Difference.BASE), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
				}
				
				if (difference.getType() != Difference.DELETE)
				{
					adjustDiffAroundMove(differenceSet, difference, false,
							witnessStart, witnessEnd, difference.getLength(Difference.WITNESS),  fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
				}
			}
			if (!skip)
			{
				SimpleLogger.logInfo("DIFF: orig(" + baseStart + "," + baseEnd + "," + witnessStart + "," + witnessEnd + ") new(" +
						difference.getOffset(Difference.BASE) + "," + difference.getLength(Difference.BASE) + "," + 
						difference.getOffset(Difference.WITNESS) + "," + difference.getLength(Difference.WITNESS) + ")");
				differenceSet.addDifference(difference);
			}
		}
	}

	private void adjustDiffAroundMove(DifferenceSet differenceSet,
			Difference difference, boolean isBase, int diffStart, int diffEnd,
			int diffLen, int moveStart, int moveEnd) {
		if (isBetween(diffStart, moveStart, moveEnd))	// the difference starts inside the move, so change the difference to start after the move
		{
			int amount = moveEnd - diffStart;
			if (isBase)
			{
				difference.setBaseOffset(moveEnd);
				difference.setBaseTextLength(diffLen - amount);
			}
			else
			{
				difference.setWitnessOffset(moveEnd);
				difference.setWitnessTextLength(diffLen - amount);
			}
		}
		else if (isBetween(diffEnd, moveStart, moveEnd))	// the difference ends inside the move, so change the difference to end before the move
		{
			if (isBase)
				difference.setBaseTextLength(moveStart-diffStart);
			else
				difference.setWitnessTextLength(moveStart-diffStart);
		}
		else if (isBetween(moveStart, diffStart, diffEnd))	// the move is completely inside the difference, create an extra insert
		{
			if (isBase)
			{
				if (difference.getType() == Difference.CHANGE)
				{
					difference.setType(Difference.DELETE);
					difference.setWitnessTextLength(0);
					difference.setDistance(Integer.MAX_VALUE);
				}
			}
			else
			{
				if (difference.getType() == Difference.CHANGE)
				{
					difference.setType(Difference.INSERT);
					difference.setBaseTextLength(0);
					difference.setDistance(Integer.MAX_VALUE);
				}
				
			}

			Difference diff2 = difference.duplicate();
			if (isBase)
				diff2.setBaseTextLength(moveStart-diffStart);
			else
				diff2.setWitnessTextLength(moveStart-diffStart);
			differenceSet.addDifference(diff2);

			int amount = moveEnd - diffStart;
			if (isBase)
			{
				difference.setBaseOffset(moveEnd);
				difference.setBaseTextLength(diffLen - amount);
			}
			else
			{
				difference.setWitnessOffset(moveEnd);
				difference.setWitnessTextLength(diffLen - amount);
			}
		}
	}

	private void addBlockToDifferenceSet(DifferenceSet differenceSet, DocumentModel baseBlockDocument, DocumentModel witnessBlockDocument) {
		baseBlockDocument.tokenize(tokenizerSettings);
		witnessBlockDocument.tokenize(tokenizerSettings);
		
	    MultiPassDiff mpd = new MultiPassDiff( baseBlockDocument, witnessBlockDocument);
		DifferenceSet blockDifferenceSet = mpd.getDifferenceSet();
		//adjust for the blocks being in different offsets

		for( Iterator k = blockDifferenceSet.getDifferenceList().iterator(); k.hasNext(); )
		{
			Difference difference = (Difference) k.next();
			difference.setBaseOffset(difference.getOffset(Difference.BASE));
			difference.setWitnessOffset(difference.getOffset(Difference.WITNESS));
			//add these differences to the total document difference set
			differenceSet.addDifference(difference);
		}
		differenceSet.setNumberOfSymbols(differenceSet.getNumberOfSymbols() + blockDifferenceSet.getNumberOfSymbols() );
		baseBlockDocument.releaseTokenTable();
		witnessBlockDocument.releaseTokenTable();
	}
    
    /**
     * Loads the existing collation cache for which the supplied document is the base document.      
     * @param JuxtaDocument The <code>JuxtaDocument</code> object of the base document.
     * @return A <code>Collation</code> object if successful, or null if the cache is 
     * not found, out of date, or corrupted.
     */
    public Collation getExistingCollation( JuxtaDocument baseText )
    {
        return documentManager.loadCollation(baseText);
    }
	
	/**
	 * Synchronously loads the cache for which the supplied document is the base document, regenerating
	 * it if necessary. Note this might take a while.   
	 * @param baseText The <code>JuxtaDocument</code> object of the base document.
	 * @return A <code>Collation</code> object.
	 * @throws ReportedException If collation of this text fails or we fail to write the cache.
	 */
	public Collation getCollation( JuxtaDocument baseText ) throws ReportedException
    {
        Collation collation = documentManager.loadCollation(baseText);
				
        // if we are unable to load the collation, regenerate it
		if( collation == null )
		{
            regenerateCollation(baseText);
            
            // now load the collation
            collation = documentManager.loadCollation(baseText);
		}
		
		return collation;
    }
	
    
	/**
	 * Terminates background collation, does not return until loader is terminated.
	 */
    public void stopLoader()
    {
        if( backgroundLoader != null )
            backgroundLoader.stopLoader();

        while( backgroundLoader != null && !backgroundLoader.isStopped() )
        {
            try
            {
                // sleep till loader terminates
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
    }
    
   private void pauseBackgroundLoader( boolean state )
    {
        if( backgroundLoader != null && !backgroundLoader.isStopped() )
        {
            backgroundLoader.pause(state);
            
            // sleep till loader changes state
            while( backgroundLoader != null && backgroundLoader.isPaused() != state )
            {
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}                
            }
        }
    }

   /**
    * Add the specified document to the comparison set, collating it and updating
    * all existing collations to include it. Collation is done asynchronously if
    * the backgroundLoad flag is set.
    * @param baseDocument The document to collate.
    * @throws ReportedException If there is a problem during collation.
    */
    public void addCollation( JuxtaDocument baseDocument ) throws ReportedException
    {
        // first, update the existing collations
        pauseBackgroundLoader(true);
        addToExistingCollations(baseDocument);
        
        // then add this document to the background loader queue
        if( backgroundLoader != null )
        {            
            backgroundLoader.loadDocument(baseDocument);
        }
        else
        {
            // start the loader up and load this doc
                        
            LinkedList loadList = new LinkedList();
            loadList.add(baseDocument);
            startLoader(loadList);                    
        }
        
        pauseBackgroundLoader(false);
    }
    
    private Collation addDocument( JuxtaDocument document ) throws ReportedException
    {
        SimpleLogger.logInfo("adding document "+document.getDocumentName()+" to comparison set.");
        //Collation collation = performBlockCollation(document);            
        Collation collation = performMovesCollation(document);            
        documentManager.cacheCollation(document,collation);
        return collation;
    }

    // This adds a single document to all of the other documents. It doesn't get collated as a base in here.
    private void addToExistingCollations(JuxtaDocument witnessDocument) throws ReportedException
    {   
        witnessDocument.tokenize(tokenizerSettings);
        
        for( Iterator i = documentManager.getDocumentList().iterator(); i.hasNext(); )
        {
            JuxtaDocument baseDocument = (JuxtaDocument) i.next();
            
            if( baseDocument != witnessDocument )
            {
                Collation collation = documentManager.loadCollation(baseDocument);

                // if the collation has been prepared and exists
                if( collation != null )
                {
                	baseDocument.tokenize(tokenizerSettings);
                    
                    // compare the documents and record the differences
            	    MultiPassDiff mpd = new MultiPassDiff(baseDocument, witnessDocument);
                    DifferenceSet differenceSet = mpd.getDifferenceSet();  
                    
                    collation.addDifferenceSet(differenceSet);

                    // release the token cache
                    baseDocument.releaseTokenTable();
                    
                    // write the collation with the new diffs to the cache
                    documentManager.cacheCollation(baseDocument,collation);
                }
            }
        }
        
        // release the token cache
        witnessDocument.releaseTokenTable();
    }

    private class BackgroundLoader extends Thread
    {
        private LinkedList documentList;
		
		// desired state 
        private boolean stopFlag, pauseFlag;
		
		// actual state
		private boolean stopped, paused;
        
        public BackgroundLoader( LinkedList documentList )
        {
        	super("BackgroundLoader");
            this.documentList = new LinkedList(documentList);
            setPriority(Thread.NORM_PRIORITY-1);
        }
        
        public void loadDocument( JuxtaDocument document )
        {
            this.documentList.addFirst(document);
        }
        
		public void pause(boolean state) 
		{
			pauseFlag = state;			
		}

		public void stopLoader()
        {
            stopFlag = true;
        }
                
        public void run()
        {
            SimpleLogger.logInfo("Collating "+documentList.size()+" documents.");
            
            while( !documentList.isEmpty() )
            {
				// check for pause command 
				pauseState();

				// check for stop command
                if( stopFlag ) 
				{
					SimpleLogger.logInfo("Terminating background loading.");
					break;
				}

                // pop the next document off the list
				JuxtaDocument baseText = (JuxtaDocument) documentList.getFirst();
                documentList.remove(baseText);
                
                if( !documentManager.collationCacheExists(baseText) )
                {
                    try
                    {
                        addDocument(baseText);
                    } 
                    catch (ReportedException e)
                    {
                        SimpleLogger.logError("Error adding document: "+baseText.getDocumentName());
                        stopLoader();                       
                    } 
                }
            }
            
            SimpleLogger.logInfo("Background loading complete!");
            stopped = true;
            loadComplete = true;
            backgroundLoader = null;
            fireLoadingComplete();
        }

        private void pauseState() 
		{
			while( pauseFlag )
			{
				// we are paused 
				paused = true;
				
				// just hang out
                try { Thread.sleep(10);} 
				catch (InterruptedException e) {}
				
				// if we are terminating, automatically unpause
				if( stopFlag || stopped ) pauseFlag = false;
			}
			
			paused = false;
		}

		public boolean isStopped()
        {
            return stopped;
        }

		public boolean isPaused() 
		{
			return paused;
		}

        public void removeDocument(JuxtaDocument document)
        {
            documentList.remove(document);            
        }
		
    }

    //TODO pass through name
    public String getName()
    {
        return "Comparison Set";
    }
    
    public boolean isLoadComplete()
    {
        return loadComplete;
    }

    public void removeCollation(JuxtaDocument document) throws ReportedException 
    {
        pauseBackgroundLoader(true);
        removeDocument(document);
        pauseBackgroundLoader(false);           
    }

    private void removeDocument(JuxtaDocument deadDocument ) throws ReportedException
    {
        if( backgroundLoader != null )
        {
            // take this document off the background loader queue
            backgroundLoader.removeDocument(deadDocument);
        }
        
        // remove the document and its cache file 
        documentManager.removeDocument(deadDocument);
        
        List documentList = documentManager.getDocumentList();

        // remove references to this document from every other collation
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {            
            JuxtaDocument baseDocument = (JuxtaDocument) i.next();
            Collation collation = documentManager.loadCollation(baseDocument);
            if( collation != null )
            {
                collation.removeWitness(deadDocument);
                documentManager.cacheCollation(baseDocument,collation);
            }            
        }
    }

	/**
	 * Add a listener to detect when background loading is complete. 
	 * @param loaderListener The listener to callback to.
	 */
    public void addLoaderCallBack(LoaderCallBack loaderListener)
    {
        loaderCallbacks.add(loaderListener);
    }

	/**
	 * Re-collates the specified document. 
	 * @param document The document to regenerate.
	 * @throws ReportedException If there is a problem during collation.
	 */
    public void regenerateCollation(JuxtaDocument document) throws ReportedException 
    {
    	ProgressUpdater progressUpdater = new ProgressUpdater(document, 0);
    	progressUpdater.reset();
        
        pauseBackgroundLoader(true);
        addDocument(document);
        pauseBackgroundLoader(false);
    }

	/**
	 * Resets all collation data and recollates all documents.
	 * @throws LoggedException If there is a problem during collation.
	 */
	public void reset() throws LoggedException
	{
	    stopLoader();		
		documentManager.clearCollationData();
		LinkedList documentList = documentManager.getDocumentList(); 
		resetProgressListeners(documentList);
		startLoader(documentList);
    }

	private void resetProgressListeners(List documentList) 
	{
		for( Iterator i = documentList.iterator(); i.hasNext(); )
		{
			JuxtaDocument document = (JuxtaDocument) i.next();
	    	ProgressUpdater progressUpdater = new ProgressUpdater(document, 0);
	    	progressUpdater.reset();
		}		
	}       
    
	/**
	 * Obtain the tokenizer settings currently used during collation.
	 * @return
	 */
    public TokenizerSettings getTokenizerSettings()
    {
        return tokenizerSettings;
    }
    

	/**
	 * Update the tokenizer settings used during collation.
	 * @param tokenizerSettings
	 */
    public void setTokenizerSettings(TokenizerSettings tokenizerSettings)
    {
        this.tokenizerSettings = tokenizerSettings;
    }

public String toJSON( JuxtaSession session, int baseID ) throws ReportedException {
        
        try {
            StringWriter mainBuffer = new StringWriter();
            JsonGenerator jg = JSON.createJsonGenerator(mainBuffer);

            StringWriter secondaryBuffer = new StringWriter();
            JsonGenerator jg2 = JSON.createJsonGenerator(secondaryBuffer); 

            // JSON Object Start
            jg.writeStartObject();
            
            jg.writeStringField("sessionID","GUID");
            
            // Difference Set Start
            jg.writeFieldName( "differenceSet" );
            jg.writeStartObject();          
            
            JuxtaDocument base = session.getDocumentManager().lookupDocument(baseID);
                        
            jg.writeStringField("baseID",Integer.toString(baseID));             

            // Witnesses Start
            jg.writeArrayFieldStart("witnesses");
            
            for( DocumentModel witness : session.getDocumentManager().getDocumentList() ) {
                
                // skip the comparison of the base text to itself TODO why is this being returned?
                if( witness.getID() == baseID ) continue;
                                
                // Witness Start
                jg.writeStartObject();
                jg.writeStringField("witnessID",Integer.toString(witness.getID()));         
                jg.writeNumberField("witnessStartOffset", 0);
                jg.writeNumberField("witnessEndOffset", witness.getDocumentLength());

                // Ranges Start
                jg.writeArrayFieldStart("ranges");

                // Base Start
                jg2.writeStartObject();
                jg2.writeStringField("witnessID",Integer.toString(base.getID()));           
                jg2.writeNumberField("witnessStartOffset", 0);
                jg2.writeNumberField("witnessEndOffset", base.getDocumentLength());

                // Ranges Start
                jg2.writeArrayFieldStart("ranges");
                
                Collation collation = session.getComparisonSet().getCollation(base);
                
                List<Difference> differences = collation.getDifferences(witness);
                    
                if( differences != null ) {
                    for( Difference difference : differences ) {
                        // don't report moves for now
                        if( difference.getType() != Difference.MOVE ) {
                                
                            // report a text range in the witness
                            jg.writeStartObject();
                            jg.writeStringField("rangeID", difference.getWitnessDocumentID() + "_" + difference.getOffset(Difference.WITNESS));
                            jg.writeStringField("rangeType", Difference.getTypeName( difference.getType()));
                            jg.writeNumberField("offset", difference.getOffset(Difference.WITNESS));
                            jg.writeNumberField("length", difference.getLength(Difference.WITNESS));
                            jg.writeStringField("relatedID", difference.getBaseDocumentID() + "_" + difference.getOffset(Difference.BASE));                                                 
                            jg.writeEndObject();                
                        
                            // accumulate the ranges for the base text in secondary buffer
                            jg2.writeStartObject();                             
                            jg2.writeStringField("rangeID", difference.getBaseDocumentID() + "_" + difference.getOffset(Difference.BASE));
                            jg2.writeStringField("rangeType",Difference.getTypeName( difference.getType()));
                            jg2.writeNumberField("offset", difference.getOffset(Difference.BASE));
                            jg2.writeNumberField("length", difference.getLength(Difference.BASE));
                            jg2.writeStringField("relatedID", difference.getWitnessDocumentID() + "_" + difference.getOffset(Difference.WITNESS));                                              
                            jg2.writeEndObject();                                               
                        }
                    }                                       
                }
                
                // Range End
                jg.writeEndArray();
                
                // Witness End
                jg.writeEndObject();

            }
            
            // Base Ranges End
            jg2.writeEndArray();

            // Base End
            jg2.writeEndObject();               

            // insert material from secondary buffer
            jg2.flush();
            jg.writeRaw( "," + secondaryBuffer.toString() );

            // Witnesses End
            jg.writeEndArray();

            // Difference Set End
            jg.writeEndObject();            

            // JSON Object End
            jg.writeEndObject();            

            jg.flush();
            
            return mainBuffer.toString();
        
        } catch (IOException e) {
            throw new ReportedException(e, "IO Error serializing comparison set to JSON");
        } 
    }

}
