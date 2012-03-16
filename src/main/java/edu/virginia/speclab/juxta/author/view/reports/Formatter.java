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
 

package edu.virginia.speclab.juxta.author.view.reports;

import java.util.Iterator;

import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.juxta.author.model.Lemma;
import edu.virginia.speclab.juxta.author.model.NumberedAnnotation;

public class Formatter
{        
    public String bibtag( int id )
    {
        return "ID"+Integer.toHexString(id);                    
    }
    
    public String annotation( NumberedAnnotation annotation )
    {        
        if( annotation == null ) return "";
        
        String divID = "id=\"note"+annotation.getId()+"\"";
        String IDLink = "<a href=\"#ann"+annotation.getId()+"\">"+annotation.getId()+".</a>";
        
        Image image = annotation.getImage();
        
        String imageText;        
        if( annotation.includeImage() && image != null ) 
            imageText = " (see <a href=\"images\\"+image.getImageFile().getName()+"\" target=\"_blank\">image</a>)";
        else
            imageText = "";
        
        return "<div class=\"note\" "+divID+">"+IDLink+" "+annotation.getNotes()+imageText+"</div>" ;
    }
    
    public String siglaList( Lemma lemma )
    {
        StringBuffer buffer = new StringBuffer();

        boolean first = true;
        for( Iterator i = lemma.getWitnessSigla().iterator(); i.hasNext(); )
        {
            Lemma.Sigla sigla = (Lemma.Sigla) i.next();
            
            if( !first )
            {
                buffer.append( ", " );
            }
            
            String annotationMark = "";
            NumberedAnnotation annotation = sigla.getAnnotation();
            if( annotation != null )
            {                
                annotationMark = "<sup id=\"ann"+annotation.getId()+"\"><a href=\"#note"+annotation.getId()+"\">"+annotation.getId()+"</a></sup>";
            }
            String location = sigla.getLocationMarker();
            if (location != "")
            	location = " (" + location + ")";
            buffer.append( "<a href=\"#"+bibtag(sigla.getID())+"\">"+sigla.getSigla()+location +"</a>"+annotationMark );
            first = false;
        }
        
        return buffer.toString();        
    }
    
    public String escapeEntities( String text )
    {
        StringBuffer output = new StringBuffer();
        char textArray[] = text.toCharArray();
        
        for( int i=0; i < textArray.length; i++ )
        {
            char c = textArray[i];
            int value = (int)c;
            
            // escape all non ASCII chars
            if( value > 127 )
            {
                output.append("&#"+value+";");
            }               
            else
            {
                output.append(c);
            }
        }
        
        return output.toString();
    }
  
}