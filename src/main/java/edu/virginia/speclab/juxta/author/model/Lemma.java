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

import java.util.Iterator;
import java.util.LinkedList;

public class Lemma
{
    private String lemmaText,locationMarker;
    private LinkedList witnessSigla;
    
    public Lemma()
    {
        witnessSigla = new LinkedList();
    }
    
    public void addWitnessSigla( JuxtaDocument witnessDocument, NumberedAnnotation annotation, String locationMarker )
    {
        int id = witnessDocument.getID();
       	if( hasSiglaForDocument(id) ) return;
        String siglaText = witnessDocument.getBiblioData().getShortTitle();            
        Sigla sigla = new Sigla(id,siglaText,annotation, locationMarker);
        witnessSigla.add(sigla);
    }
    
    public boolean hasSiglaForDocument( int documentID ) {
    	for( Iterator i = witnessSigla.iterator(); i.hasNext(); ) {
    		Sigla testSigla = (Sigla) i.next();
    		if( testSigla.getID() == documentID ) return true;
    	}
    	return false;
    }

    public class Sigla
    {
        private int documentID;
        private String sigla;
        private NumberedAnnotation annotation;
        private String locationMarker;
        
        public Sigla( int documentID, String sigla, NumberedAnnotation annotation, String locationMarker )
        {
            this.annotation = annotation;
            this.documentID = documentID;
            this.sigla = sigla;
            this.locationMarker = locationMarker;
        }

        public int getID()
        {
            return documentID;
        }

        public String getSigla()
        {
            return sigla;
        }

        public String getLocationMarker()
        {
            return locationMarker;
        }

        public NumberedAnnotation getAnnotation()
        {
            return annotation;
        }
        
    }

    public String getLemmaText()
    {
        return lemmaText;
    }

    public LinkedList getWitnessSigla()
    {
        return witnessSigla;
    }

    public void setLemmaText(String lemmaText)
    {
        this.lemmaText = lemmaText;
    }

    public String getLocationMarker()
    {
        return locationMarker;
    }
    

    public void setLocationMarker(String locationMarker)
    {
        this.locationMarker = locationMarker;
    }
    
}
