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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.CriticalApparatus;
import edu.virginia.speclab.util.FileUtilities;

public class CriticalApparartusReport {
    private VelocityEngine engine;
    private VelocityContext formContext;

    private String title;

    private static final String CRITICAL_APPARATUS_FORM = "forms/ca.vm";
    private HashSet imageSet;

    public CriticalApparartusReport(CriticalApparatus apparatus, String title) throws ReportedException {
        try {
            this.engine = new VelocityEngine();
            this.engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            this.engine.setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            this.engine.init();
        } catch (Exception e) {
            throw new ReportedException(e, "Error Initializing Report Engine.");
        }
        this.title = title;
        formContext = createContext(apparatus);
    }

    /**
     * Generates the critical apparatus at the location specified. If there are images associated with 
     * this apparatus, they will be places in a subdirectory called "images"
     * beneath the specified file's location.
     * @param File The file to write to.  
     * @throws ReportedException Any problems writing the file or copying the images
     */
    public void write(File file) throws ReportedException {
        if (engine == null || formContext == null)
            return;

        Template template;

        try {
            template = engine.getTemplate(CRITICAL_APPARATUS_FORM);
        } catch (Exception e) {
            throw new ReportedException(e, "An error occured loading the report template: " + CRITICAL_APPARATUS_FORM);
        }

        try {
            FileWriter writer = new FileWriter(file);
            template.merge(formContext, writer);
            writer.close();
        } catch (Exception e) {
            throw new ReportedException(e, "An error occured attempting to write the file: " + file.getPath());
        }

        writeImageFiles(file);
    }

    private void writeImageFiles(File file) throws ReportedException {
        // if there are no images, we're done
        if (imageSet.isEmpty())
            return;

        ReportedException error = null;
        File imageDestDir = new File(file.getParent() + "/images");

        // create the image sub-directory if it does not exist
        if (!imageDestDir.exists()) {
            if (!imageDestDir.mkdir()) {
                // can't create directory, abort.
                throw new ReportedException("Unable to create directory for images.",
                    "mkdir failed creating image sub directory");
            }
        }

        for (Iterator i = imageSet.iterator(); i.hasNext();) {
            Image image = (Image) i.next();
            File srcFile = image.getImageFile();
            File destFile = new File(imageDestDir.getAbsoluteFile() + File.separator + srcFile.getName());

            try {
                // copy the file to the image sub-directory
                FileUtilities.copyFile(srcFile, destFile, false);
            } catch (IOException e) {
                // problem copying file, store for later
                error = new ReportedException(e, "Unable to copy one or more image files to target directory.");
            }
        }

        // all done, throw the last error that occurred if any 
        if (error != null)
            throw error;
    }

    private VelocityContext createContext(CriticalApparatus apparatus) {
        VelocityContext context = new VelocityContext();

        // obtain the images referenced in this apparatus
        this.imageSet = apparatus.getImageSet();

        if (title == null)
            title = apparatus.getBaseBiblioData().getTitle() + " Collation";

        HashMap criticalApparatus = new HashMap();
        criticalApparatus.put("lemmas", apparatus.getLemmas());
        criticalApparatus.put("witnesses", apparatus.getWitnesses());
        criticalApparatus.put("base", apparatus.getBaseBiblioData());
        criticalApparatus.put("title", title);
        criticalApparatus.put("basedoc", apparatus.getBase());
        criticalApparatus.put("annotations", apparatus.getAnnotations());
        criticalApparatus.put("hasNotes", new Boolean(apparatus.hasAnnotations()));
        context.put("critical", criticalApparatus);
        context.put("formatter", new Formatter());
        return context;
    }

}
