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
 

package edu.virginia.speclab.juxta.author.view;

import edu.virginia.speclab.exceptions.ReportedException;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import edu.virginia.speclab.juxta.author.Juxta;
import edu.virginia.speclab.juxta.author.model.JuxtaXMLParser;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.DataForm;
import java.nio.charset.Charset;

/**
 * A text entry form for displaying and editing bibliographic data. 
 * @author Nick
 *
 */
public class BiblioDataForm extends DataForm implements JuxtaUserInterfaceStyle
{
    private BiblioData biblioData;
    private boolean hasTitle;
    private final String noInfoAvailable = "Juxta XML file not selected";
    
    /**
     * Using this constructor hooks the form up to a file chooser as a read only preview
     * pane for juxta documents.
     * @param chooser
     */
    public BiblioDataForm( JFileChooser chooser )
    {
        this.biblioData = BiblioData.createNew(); 
        initUI( false, true );
        
        // hook the form up to the file chooser and size it appropriately
        chooser.addPropertyChangeListener( new FileChooserTracker() );
        setPreferredSize(new Dimension( 300, 215 ));
    }
    
    /**
     * Creates a form for editing and display bibliographic data. 
     * @param biblioData The initial values in the form.
     * @param editable Toggles whether the form is read only.
     */
    public BiblioDataForm( BiblioData biblioData, boolean editable )
    {
        this.biblioData = biblioData;
        initUI(editable, false);
    }

    private void initUI( boolean editable, boolean addTitle )
    {
        setFont( SMALL_FONT );
        setEnableFields(editable);
               
        hasTitle = addTitle;
        if (addTitle)
        	addLabel("dlg title", noInfoAvailable);
        addField("Short title", biblioData.getShortTitle() );
        addField("Title", biblioData.getTitle() );
        addField("Author", biblioData.getAuthor() );
        addField("Editor", biblioData.getEditor() );
        addField("Source", biblioData.getSource() );
        addField("Date", biblioData.getDate(), "This date is not used for sorting, and can be any text." );
        addSpacer(10);
        addMemoField("Notes", biblioData.getNotes(), 5 );
        addSpacer(10);
        addDateField("Sort date", biblioData.getSortDate(), "This date field is used for chronological sorting of texts.");
    }

    private void updateForm(BiblioData data, String title)
    {
    	if (hasTitle)
    		updateField("dlg title", title);
        updateField("Short title", data.getShortTitle() );
        updateField("Title", data.getTitle() );
        updateField("Author", data.getAuthor() );
        updateField("Editor", data.getEditor() );
        updateField("Source", data.getSource() );
        updateField("Date", data.getDate() );
        updateField("Notes", data.getNotes() );
        updateField("Sort date", data.getSortDate() );
    }
    
    public BiblioData getData()
    {

        return new BiblioData( getFieldContent("Title"),
                getFieldContent("Short title"),
                getFieldContent("Author"),
                getFieldContent("Editor"),
                getFieldContent("Source"),
                getFieldContent("Date"),
                getFieldContent("Notes"),
                getDateFieldContent("Sort date"));

    }
    
    private class FileChooserTracker implements PropertyChangeListener
    {
        public FileChooserTracker()
        {
        }
        
        public void propertyChange(PropertyChangeEvent e)
        {
            File file = null;
            boolean update = false;
            String prop = e.getPropertyName();

            //If the directory changed, don't show an image.
            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
                file = null;
                update = true;

            //If a file became selected, find out which one.
            } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
                file = (File) e.getNewValue();
                update = true;
            }

            //Update the preview accordingly.
            if (update) 
            {
                BiblioData data = null;
                
                String title = noInfoAvailable;
                if( file != null && file.isFile() )
                {					
                    try 
					{
                        JuxtaXMLParser xmlParser = new JuxtaXMLParser(file, Juxta.JUXTA_VERSION, Charset.forName("UTF-8"));
                        xmlParser.parse();

						data = xmlParser.getBiblioData();
						if (file.getName().endsWith("xml"))
							title = "Bibliographic data for: " + file.getName();
					} 
                    catch (ReportedException ex) {
                        Logger.getLogger(BiblioDataForm.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(BiblioDataForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else
                {
                    data = BiblioData.createNew();
                }

                if(data!=null) updateForm(data, title);
            }
        }
    }
}
