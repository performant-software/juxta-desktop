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

import java.util.Date;

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
    private Date sortDate;

    public BiblioData( String title,
                       String shortTitle,
                       String author,
                       String editor,
                       String source,
                       String date,
                       String notes,
                       Date sortDate)
    {
        this.title = title;
        this.shortTitle = shortTitle;
        this.author = author;
        this.editor = editor;
        this.source = source;
        this.date = date;
        this.notes = notes;
        this.sortDate = sortDate;
    }
    
    public BiblioData( BiblioData that ) {
        this.title = that.title;
        this.shortTitle = that.shortTitle;
        this.author = that.author;
        this.editor = that.editor;
        this.source = that.source;
        this.date = that.date;
        this.notes = that.notes;
        this.sortDate = that.sortDate;
    }

    public static BiblioData createNew()
    {
        return new BiblioData("","","","","","","",null);
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
    
    public void setShortTitle(String newTitle) {
        this.shortTitle = newTitle;
    }
    

    public String getSource()
    {
        return source;
    }
    

    public String getTitle()
    {
        return title;
    }

    public Date getSortDate()
    {
        return sortDate;
    }

}
