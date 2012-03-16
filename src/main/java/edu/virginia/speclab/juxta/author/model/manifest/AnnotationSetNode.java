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
 

package edu.virginia.speclab.juxta.author.model.manifest;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;

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

    public AnnotationSetNode(JuxtaSession session)
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
            difference.setBaseOffset(node.getBaseOffset(), node.getBaseSpace());
            difference.setBaseTextLength(node.getBaseLength(), node.getBaseSpace());
            difference.setWitnessOffset(node.getWitnessOffset(), node.getWitnessSpace());
            difference.setWitnessTextLength(node.getWitnessLength(), node.getWitnessSpace());
            Annotation annotation = new Annotation( difference );
            annotation.setNotes(node.getNotes());
            annotation.setIncludeImage(node.includeImage());
            annotation.setFromOldVersion(node.isFromOldVersion());
            annotationList.add(annotation);
        }
        
        return annotationList;
    }

}
