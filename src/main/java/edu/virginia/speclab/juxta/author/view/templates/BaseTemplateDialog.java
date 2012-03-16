/*
 *  Copyright 2002-2011 The Rector and Visitors of the
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
package edu.virginia.speclab.juxta.author.view.templates;

import java.util.ArrayList;

import javax.swing.JDialog;

import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;

/**
 * Base class for template dialogs. It manages the current template
 * configuration, active template and target document (if there is one). 
 * It also provides accessor methods for retrieving the final 
 * template selection and tagsets
 * 
 * @author loufoster
 *
 */
public abstract class BaseTemplateDialog extends JDialog {

    protected String templateGuid = "";
    protected JuxtaDocument tgtDocument = null;
    protected JuxtaAuthorFrame juxta = null;
    protected boolean okClicked = false;
    protected ConfigType currConfig = null;
    
    public BaseTemplateDialog(JuxtaAuthorFrame juxtaFrame) {
        super(juxtaFrame);
        this.juxta = juxtaFrame;
        this.currConfig = ConfigType.SESSION;
    }
    
    /**
     * Setup the template dialog with the working document and
     * working template guid. Either of these may be null
     * @param doc
     * @param templateGuid
     */
    public void setup(JuxtaDocument doc, String templateGuid) {
        this.tgtDocument = doc;
        this.templateGuid = templateGuid;
    }
    
    /**
     * set the active parse template configuration
     * @param type
     */
    public void setConfigType( ConfigType type ) {
        this.currConfig = type;
    }
    
    /**
     * get the active parse tempate configuration
     * @return
     */
    protected TemplateConfig getConfig() {
        return TemplateConfigManager.getInstance().getConfig(this.currConfig);
    }
    
    /**
     * Get a list of templates that match the given root
     * @param docRoot
     * @return
     */
    protected ArrayList<ParseTemplate> getFilteredTemplateList( final String docRoot) {
        ArrayList<ParseTemplate> out = new ArrayList<ParseTemplate>();
        for ( ParseTemplate t : getConfig() ) {
            if ( t.getRootTagName().equals(docRoot)) {
                out.add(t);
            }
        }
        return out;
    }
    
    /**
     * Check if OK was clicked on this dialog.
     * @return
     */
    public final boolean wasOkClicked() {
        return this.okClicked;
    }
    
    /**
     * Get the setected parse template guid
     * @return
     */
    public final String getTemplateGuid() {
        return this.templateGuid;
    }    
 }
