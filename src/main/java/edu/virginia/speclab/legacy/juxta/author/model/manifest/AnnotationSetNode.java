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

package edu.virginia.speclab.legacy.juxta.author.model.manifest;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.legacy.juxta.author.model.Annotation;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaSession_1_3_1;

public class AnnotationSetNode
{
    private List annotationNodeList;

    /**
     * Create a new Comparison object.
     */
    public AnnotationSetNode(Node annotationSetNode)
            throws ReportedException
    {
        annotationNodeList = new LinkedList();
        loadAnnotationSetNode(annotationSetNode);
    }

    public AnnotationSetNode(JuxtaSession_1_3_1 session)
    {        
        annotationNodeList = new LinkedList();
        List annotations = session.getAnnotationManager().getAnnotations();
        
		synchronized(annotations)
		{
	        for( Iterator i = annotations.iterator(); i.hasNext(); )
	        {
	            Annotation annotation = (Annotation) i.next();
	            AnnotationNode node = new AnnotationNode(annotation);
	            annotationNodeList.add(node);
	        }
		}
    }
    
    private void loadAnnotationSetNode(Node annotationSetNode)
            throws ReportedException
    {        
        // Traverse the child elements looking for comparisons and commentary
        NodeList progressionChildren = annotationSetNode.getChildNodes();

        if (progressionChildren != null)
        {
            for (int i = 0; i < progressionChildren.getLength(); i++)
            {
                Node currentNode = progressionChildren.item(i);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE
                        && currentNode.getNodeName().equals("annotation"))
                {
                    AnnotationNode node = new AnnotationNode(currentNode);
                    annotationNodeList.add(node);
                } 
            }
        }
    }

	public List getAnnotationNodeList() 
	{
		return annotationNodeList;
	}

    public LinkedList createAnnotationList()
    {
        LinkedList annotationList = new LinkedList();
        
        for( Iterator i = annotationNodeList.iterator(); i.hasNext(); )
        {
            AnnotationNode node = (AnnotationNode) i.next();
            Difference difference = new Difference( node.getBaseDocumentID(), node.getWitnessDocumentID(), node.getDifferenceType() );
            difference.setBaseOffset(node.getBaseOffset());
            difference.setBaseTextLength(node.getBaseLength());
            difference.setWitnessOffset(node.getWitnessOffset());
            difference.setWitnessTextLength(node.getWitnessLength());
            Annotation annotation = new Annotation( difference );
            annotation.setNotes(node.getNotes());
            annotation.setIncludeImage(node.includeImage());
            annotationList.add(annotation);
        }
        
        return annotationList;
    }

}
