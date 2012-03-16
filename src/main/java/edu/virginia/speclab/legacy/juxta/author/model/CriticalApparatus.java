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

package edu.virginia.speclab.legacy.juxta.author.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.legacy.diff.collation.Collation;
import edu.virginia.speclab.legacy.diff.document.Image;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.legacy.diff.token.ConfigurableReader;
import edu.virginia.speclab.legacy.diff.token.Token;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.legacy.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.util.IntegerCouple;
import edu.virginia.speclab.util.SimpleLogger;

public class CriticalApparatus
{
    private LinkedList lemmas;
    private LinkedList witnessList;
    private LinkedList annotationList;
    private BiblioData baseBiblioData;
    private HashSet imageSet;
    private JuxtaSession_1_3_1 session;
	private int currentTokenIndex;
	private List tokens;
	private boolean baseAFragment;
	private JuxtaDocument baseDocument;
	
	private static final String INSERTION_MARK = "<span class=\"mark\">^</span>";
	private static final String DELETION_MARK = "<span class=\"mark\">] ~</span>";
	private static final String MOVE_MARK = "<span class=\"mark\"> -> </span>";
	private static final String CHANGE_MARK = "<span class=\"mark\">] </span>";
    
    public CriticalApparatus( JuxtaSession_1_3_1 session ) throws ReportedException
    {
    	this.session = session;
    }
    
    public void runCriticalApparatus()
    {
    	recordBiblioData(session);
    	generateLemmas(session); 
    }

    private void recordBiblioData( JuxtaSession_1_3_1 session )
    {
          DocumentManager documentManager = session.getDocumentManager();
          Collation currentCollation = session.getCurrentCollation();
          JuxtaDocument baseDocument = (JuxtaDocument) documentManager.lookupDocument(currentCollation.getBaseDocumentID());

          this.baseDocument=baseDocument;
          baseAFragment = baseDocument.isFragment();
          LinkedList documentList = new LinkedList( documentManager.getDocumentList() );
          HashSet collationFilter = currentCollation.getCollationFilter();        
          if( collationFilter != null ) documentList.removeAll(collationFilter);
          documentList.remove(baseDocument);
          
          baseBiblioData = baseDocument.getBiblioData();        
          witnessList = new LinkedList();
          
          for( Iterator i = documentList.iterator(); i.hasNext(); )
          {
              JuxtaDocument document = (JuxtaDocument) i.next();
              witnessList.add(document);
          }          
    }
    
    public static Lemma generateLemma( Difference difference, JuxtaDocument baseDocument, JuxtaDocument witnessDocument )
    {
        String lemmaText = generateLemmaText(difference,witnessDocument);
        String baseQuote = AnnotationManager.filterText(baseDocument.getSubString(difference.getOffset(Difference.BASE),difference.getLength(Difference.BASE)));
        Lemma lemma = createLemma("",baseQuote,lemmaText,difference.getType());
        return lemma;
    }

    private void generateLemmas( JuxtaSession_1_3_1 session )
    {
        Collation collation = session.getCurrentCollation();
        DocumentManager documentManager = session.getDocumentManager();
        JuxtaDocument baseDocument = documentManager.lookupDocument(collation.getBaseDocumentID());        
        AnnotationManager annotationManager = session.getAnnotationManager();
        TokenizerSettings tokenizerSettings = session.getComparisonSet().getTokenizerSettings();
        
        this.lemmas = new LinkedList();        
        this.annotationList = new LinkedList();
        this.imageSet = new HashSet();
        
        baseDocument.tokenize(tokenizerSettings);
        tokens = baseDocument.getTokenList();
        if( tokens == null ) return;

        HashMap lemmaMap = new HashMap();
        LinkedList watchList = new LinkedList();  // the differences currently in our window of inspection
        LinkedList goneList = new LinkedList();   // differences that have past out of the window that need to be removed                
        int nextAnnotationID = 1;
		
        // step through the base document, a token at a time, pulling out the difference at each location
        for(currentTokenIndex = 0; currentTokenIndex < tokens.size(); currentTokenIndex++ )
        {
            Token token = (Token) tokens.get(currentTokenIndex);

            // remove differences from the watch list that have passed by 
            goneList.clear();
            for( Iterator j = watchList.iterator(); j.hasNext(); )
            {
                Difference difference = (Difference) j.next();
                int endOffset = difference.getOffset(Difference.BASE) + difference.getLength(Difference.BASE);
                if(( token.getOffset() > endOffset ) && ( difference.getType() != Difference.MOVE ))	// Don't remove MOVE type, because they may not be in order.
                    goneList.add(difference);
            }
            watchList.removeAll(goneList);
            
            // get the differences at the current location
            List differences = collation.getDifferences(token.getOffset());
            differences = documentManager.getMovesManager().addMoves(differences, collation.getBaseDocumentID(), token.getOffset());
            
            //test for differences in whitespace before token
            //also checks in punctuation before the token
            int offset = 1;
            int targetOffset = (token.getOffset()-offset>=0) ? token.getOffset()-offset : 0;
            char targetChar = baseDocument.getSubString(targetOffset, 1).charAt(0);
            
            while(Character.isWhitespace(targetChar)||!Character.isLetter(targetChar))
            {
	            List noTokenDifferences = collation.getDifferences(targetOffset);
	            noTokenDifferences = documentManager.getMovesManager().addMoves(noTokenDifferences, collation.getBaseDocumentID(), targetOffset);
	            if(noTokenDifferences != null)
	            {
	            	if( differences == null )
	            		differences = noTokenDifferences;
	            	else
	            		differences.addAll(noTokenDifferences);
	            }
	            offset++;
	            //if the offset is looking beyond 0, break, because
	            //we are already at the beginning of the document
	            if (token.getOffset()-offset>=0)
	            	targetOffset = token.getOffset()-offset;
	            else
	            	break;
	            targetChar = baseDocument.getSubString(targetOffset, 1).charAt(0);
            }

            // no differences here
            if( differences == null ) continue;
                                        
            // iterate through all of the differences in our current inspection window
            for( Iterator j = differences.iterator(); j.hasNext(); )
            {
                Difference difference = (Difference) j.next();                
    
                // if we already have processed this difference, skip it
                if( !watchList.contains(difference) )
                {                    
                    // create a unique key for this text range in the base text
                    IntegerCouple baseKey = new IntegerCouple( difference.getOffset(Difference.BASE), 
                                                               difference.getLength(Difference.BASE) );
                    
                    // look up the set of lemmas which exactly match this base text range
                    HashMap lemmaSubMap = (HashMap) lemmaMap.get(baseKey);        
                    if( lemmaSubMap == null )
                    {
                        // if there isn't a set for lemmas that match, create one
                        lemmaSubMap = new HashMap();
                        lemmaMap.put(baseKey,lemmaSubMap);
                    }

                    // of the lemmas which match the base text range, look up the one that also matches the 
                    // lemma text in its tokenized form
                    
                    JuxtaDocument witnessDocument = null;
                    if( difference.getType() != Difference.DELETE )
                    {
                        witnessDocument = documentManager.lookupDocument(difference.getWitnessDocumentID());
                    }
                    
                    String lemmaText = generateLemmaText(difference,witnessDocument);
                    String tokenizedText = tokenizeText(lemmaText,tokenizerSettings);
                    Lemma lemma = (Lemma) lemmaSubMap.get(tokenizedText);
                    
                    // if there is no such lemma, create it
                    if( lemma == null )
                    {
                        String baseQuote = generateQuote( tokens, currentTokenIndex, difference, documentManager);
                        String locationMarker = createLocationMarker(baseDocument,token);
                        lemma = createLemma(locationMarker,baseQuote,lemmaText,difference.getType());

                        // add the lemma to the set matching the base quote                        
                        lemmaSubMap.put(tokenizedText,lemma);

                        // add the lemma to the document order list of lemmas
                        lemmas.add(lemma);                                    
                    }
										
                    JuxtaDocument witness = (JuxtaDocument) documentManager.lookupDocument(difference.getWitnessDocumentID());
                    Annotation annotation = annotationManager.getRelatedAnnotation(difference,true);
                    
                    NumberedAnnotation numberedAnnotation;
                    
                    if( annotation != null )
                    {
                        // if there is an image associated with this annotation, include it
                        Image image = null;
                        if( annotation.includeImage() )
                        {
                            image = baseDocument.getImageAt(token.getOffset());
                            if( image != null ) imageSet.add(image);
                        }
                     
                        numberedAnnotation = new NumberedAnnotation( nextAnnotationID++, annotation, image );
                    }
                    else
                        numberedAnnotation = null;
                    
                    // record a new witness and annotation for this lemma
                    String marker = createLocationMarker(witness.getLocationMarker(difference.getOffset(Difference.WITNESS)));
                    if (lemma.getLocationMarker().equals(marker))	// suppress the location marker on the witness if it is the same as the base
                    	marker = "";
                    lemma.addWitnessSigla(witness,numberedAnnotation, marker);
                    
                    // add this annotation to the list of annotations
                    if( numberedAnnotation != null ) annotationList.add(numberedAnnotation);
                    
                    // add this difference to the list of differences already processed
					watchList.add(difference);
                }
            }
        }        
    }
    
    private static Lemma createLemma( String locationMarker, String baseQuote, String lemmaText, int differenceType )
    {
        Lemma lemma = new Lemma();
        StringBuffer lemmaBuffer = new StringBuffer(); 
        
        // add the location marker
        lemma.setLocationMarker(locationMarker);
        
        // if there is a quote, add it
        if( baseQuote.length() > 0 )
        {
            if( differenceType == Difference.CHANGE )
                baseQuote += CHANGE_MARK;
            lemmaBuffer.append( baseQuote );    
        }                        
        
        // add the lemma text
        lemmaBuffer.append( lemmaText );
        
        lemma.setLemmaText(lemmaBuffer.toString());
        return lemma;                            
    }
    
    private String tokenizeText( String text, TokenizerSettings settings )
    {
        ConfigurableReader tokenizer = new ConfigurableReader(settings);        
        tokenizer.openString(text);
        
        String tokenizedText = null, nextToken;
        
        try
        {
            while( (nextToken = tokenizer.readSymbol()) != null )
            {
                tokenizedText = nextToken + " ";
            }
        } 
        catch (IOException e)
        {
            SimpleLogger.logError("Error occurred tokenizing text: "+text);
        }
        
        return tokenizedText;
    }

	private String generateQuote(List tokens, int currentTokenIndex, Difference difference, DocumentManager documentManager ) 
	{
		String quote = "";
		JuxtaDocument baseDocument = documentManager.lookupDocument(difference.getBaseDocumentID());

		quote = baseDocument.getSubString(difference.getOffset(Difference.BASE), difference.getLength(Difference.BASE));
		
		return AnnotationManager.filterText(quote);
	}

	private static String generateLemmaText( Difference difference, JuxtaDocument witnessDocument )
    {        
		String lemmaText = "";
		
        if( difference.getType() == Difference.CHANGE )
        {
			lemmaText = createChangeLemma(difference,witnessDocument);
        }
        else if( difference.getType() == Difference.DELETE )
        {
			lemmaText = createDeleteLemma();
        }
        else if( difference.getType() == Difference.INSERT )
        {
			lemmaText = createInsertLemma(difference,witnessDocument);
        }
        else if ( difference.getType() == Difference.MOVE )
        {
			lemmaText = createMoveLemma(difference,witnessDocument);
        }
		
        return lemmaText;
    }
    
    private static String createMoveLemma(Difference difference, JuxtaDocument witnessDocument)
    {
        String witnessText = witnessDocument.getSubString( difference.getOffset(Difference.WITNESS), 
                difference.getLength(Difference.WITNESS)  );
		return MOVE_MARK+witnessText;
	}

	private static String createInsertLemma( Difference difference, JuxtaDocument witnessDocument )
    {
        String witnessText = witnessDocument.getSubString( difference.getOffset(Difference.WITNESS), 
                                                           difference.getLength(Difference.WITNESS)  );

        return INSERTION_MARK+AnnotationManager.filterText(witnessText) + " ";
    }
    
    private static String createLocationMarker( JuxtaDocument baseDocument, Token token )
    {
        LocationMarker marker = baseDocument.getLocationMarker(token.getOffset());
        return createLocationMarker(marker);
    }
    
    private static String createLocationMarker( LocationMarker marker )
    {
        if( marker != null )
        {
            return marker.getLocationType()+String.valueOf(marker.getNumber());
        }
        
        return "";
    }

    private static String createDeleteLemma()
    {
        return DELETION_MARK + " ";
    }

    private static String createChangeLemma( Difference difference, JuxtaDocument witnessDocument )
    {        
        String witnessText = witnessDocument.getSubString( difference.getOffset(Difference.WITNESS), 
                                                           difference.getLength(Difference.WITNESS)  );
        
        return AnnotationManager.filterText(witnessText) + " ";       
    }

    public LinkedList getLemmas()
    {
        return lemmas;
    }
    
    public BiblioData getBaseBiblioData()
    {
        return baseBiblioData;
    }
    
    public boolean hasAnnotations()
    {
        if( annotationList == null || annotationList.isEmpty() ) return false;
        else return true;        
    }
    
    public LinkedList getWitnesses()
    {          
        return witnessList;
    }

    public LinkedList getAnnotations()
    {
        return annotationList;
    }

    public HashSet getImageSet()
    {
        return imageSet;
    }
    
    public float getProgress()
    {
    	if (tokens == null)
    		return 0.0f;
    	return (float)currentTokenIndex / tokens.size();
    }
    
    public Boolean isBaseAFragment()
    {
    	return new Boolean(baseAFragment);
    }
    
    public JuxtaDocument getBase()
    {
    	return baseDocument;
    }
}
