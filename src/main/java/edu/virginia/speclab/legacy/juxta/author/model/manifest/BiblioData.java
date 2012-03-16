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

/**
 * Stores the bibliographic information for the associated document.
 * @author Nick
 *
 */
public class BiblioData
{
    private String title, shortTitle;
    private String author;
    private String editor;
    private String source;
    private String date;
    private String notes;

    public BiblioData( String title, 
                       String shortTitle,
                       String author,
                       String editor,
                       String source,
                       String date,
                       String notes ) 
    {
        this.title = title; 
        this.shortTitle = shortTitle;  
        this.author = author;
        this.editor = editor;
        this.source = source; 
        this.date = date; 
        this.notes = notes; 
    }
    
    public static BiblioData createNew()
    {
        return new BiblioData("","","","","","","");        
    }
    
	public static BiblioData createNew(String title) {
        return new BiblioData(title,"","","","","","");        
	}

    public String getAuthor()
    {
        return author;
    }
    

    public String getDate()
    {
        return date;
    }
    

    public String getEditor()
    {
        return editor;
    }
    

    public String getNotes()
    {
        return notes;
    }
    

    public String getShortTitle()
    {
        return shortTitle;
    }
    

    public String getSource()
    {
        return source;
    }
    

    public String getTitle()
    {
        return title;
    }

    
}
