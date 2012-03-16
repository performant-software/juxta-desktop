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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.legacy.diff.Difference;


public class AnnotationManager
{
	private List annotations;
	private LinkedList listeners;
	
	public static final char PARAGRAPH_MARKER = '/';
	private JuxtaSession_1_3_1 session; 
	
	public AnnotationManager( JuxtaSession_1_3_1 session )
	{
		this.session = session;
		
		DocumentManager documentManager = session.getDocumentManager();
		
        List storedAnnotations = documentManager.getStoredAnnotationList();
        
        if( storedAnnotations != null )
        {
            annotations = Collections.synchronizedList(storedAnnotations);
        }
        else
        {
            annotations = Collections.synchronizedList(new LinkedList());
        }
        
		listeners = new LinkedList();
	}
	
	public static String filterText( String text )
	{
        char[] textArray = text.toCharArray();
		
        boolean skipFlag = false;
        for( int i=0; i < textArray.length; i++)
        {
            // emits a single PARAGRAPH_MARKER when it discovers runs of '\n' or '\r'
            if( textArray[i] == '\n' || textArray[i] == '\r' )
            {
                if( skipFlag == false ) 
                {
                    textArray[i] = PARAGRAPH_MARKER;
                }
                
                skipFlag = true;
            }
            else skipFlag = false;            
        }
		
		return new String(textArray);
	}
	
	public void addListener( AnnotationListener listener )
	{
		listeners.add(listener);		
	}
	
	public void removeListener( AnnotationListener listener )
	{
		listeners.remove(listener);
	}
	
	private void fireAnnotationAdded( Annotation annotation )
	{
		for( Iterator i = listeners.iterator(); i.hasNext(); )
		{
			AnnotationListener listener = (AnnotationListener) i.next();
			listener.annotationAdded(annotation);
		}		
	}
	
	public Annotation addAnnotation( Difference difference )
	{
		Annotation annotation = new Annotation(difference);
        
        synchronized( annotations )
        {
            annotations.add(annotation);
        }
        
		fireAnnotationAdded(annotation);
		session.markAsModified();
        return annotation;
	}
    
    /**
     * Returns an annotation which is associate with a difference that is either the 
     * same difference as the parameter or overlaps the difference specified.
     * @param difference The difference to which the annotation is related.
     * @param marked If <code>true</code>, only return annotation with comments in them, if <code>false</code>,
     * it will return the first related annotation encountered. 
     * @return An <code>Annotation</code> object or null if one is not found.
     */
    public Annotation getRelatedAnnotation( Difference difference, boolean marked )
    {
        synchronized( annotations )
        {
            for( Iterator i = annotations.iterator(); i.hasNext(); )
            {
                Annotation annotation = (Annotation) i.next();

                // skip over annotations without comments if marked is set true
                if( marked && !annotation.isMarked() ) continue;
                
                Difference otherDifference = annotation.getDifference();
                
                if( difference.same(otherDifference) )
                {
                    return annotation;
                }
                else if( difference.getWitnessDocumentID() == otherDifference.getBaseDocumentID() &&
                         difference.getBaseDocumentID() == otherDifference.getWitnessDocumentID()     )
                {                    
                    int otherStart = otherDifference.getOffset(Difference.BASE);
                    int otherEnd = otherStart + otherDifference.getLength(Difference.BASE);
                    int diffStart = difference.getOffset(Difference.WITNESS);
                    int diffEnd = diffStart + difference.getLength(Difference.WITNESS);
                    
                    // if the two differences overlap and refer to the same document, then they
                    // must be equivalent, pass on the annotation
                    if( ( diffStart >= otherStart && diffStart < otherEnd ) ||
                        ( otherStart >= diffStart && otherStart < diffEnd )     )
                    {
                        return annotation;
                    }                   
                }
            }
        }
        
        return null;
    }
	
	public Annotation getAnnotation( Difference difference )
	{
        synchronized( annotations )
        {
            for( Iterator i = annotations.iterator(); i.hasNext(); )
            {
                Annotation annotation = (Annotation) i.next();
                
                if( annotation.getDifference().same(difference) )
                {
                    return annotation;
                }
            }
        }
		
		return null;
	}

	public List getAnnotations() 
	{
		return annotations;
	}

	public void removeAnnotation(Annotation annotation) 
	{
        synchronized( annotations )
        {
            annotations.remove(annotation);
        }
		fireAnnotationRemoved(annotation);
		session.markAsModified();
	}
	
	/**
	 * Remove annotations which reference the specified document.
	 * @param document The document to key off of.
	 */
	public void removeAnnotations( JuxtaDocument document )
	{
		LinkedList deadAnnotations = new LinkedList();
		
		for( Iterator i = annotations.iterator(); i.hasNext(); )
		{
			Annotation annotation = (Annotation) i.next();
            JuxtaDocument baseDocument = annotation.getBaseDocument(session.getDocumentManager());
            JuxtaDocument witnessDocument = annotation.getWitnessDocument(session.getDocumentManager());
			
			if( (baseDocument != null && document.getID() == baseDocument.getID()) ||
				(witnessDocument != null && document.getID() == witnessDocument.getID()) )
			{
				deadAnnotations.add(annotation);
			}
		}
		
		for( Iterator j = deadAnnotations.iterator(); j.hasNext(); )
		{
			Annotation deadAnnotation = (Annotation) j.next();
			removeAnnotation(deadAnnotation);
		}		
	}
	
	private void fireAnnotationRemoved(Annotation annotation) 
	{
		for( Iterator i = listeners.iterator(); i.hasNext(); )
		{
			AnnotationListener listener = (AnnotationListener) i.next();
			listener.annotationRemoved(annotation);
		}		
	}

	public void markAnnotation(Annotation annotation, String notes ) 
	{
        annotation.setNotes(notes);
        
		for( Iterator i = listeners.iterator(); i.hasNext(); )
		{
			AnnotationListener listener = (AnnotationListener) i.next();
			listener.annotationMarked(annotation);
		}		
		
		session.markAsModified();
	}
}
