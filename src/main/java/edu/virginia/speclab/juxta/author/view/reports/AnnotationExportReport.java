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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;

public class AnnotationExportReport {
    private VelocityEngine engine;
    private VelocityContext formContext;

    private static final String ANNOTATIONS_EXPORT_FORM = "/forms/annotations.vm";

    public AnnotationExportReport(JuxtaSession juxtaSession) throws ReportedException {
        try {
            this.engine = new VelocityEngine();
            this.engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            this.engine.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            this.engine.init();
        } catch (Exception e) {
            throw new ReportedException(e, "Error Initializing Report Engine.");
        }
        formContext = createContext(juxtaSession);
    }

    /**
     * Generates the annotation export report at the location specified. 
     * @param File The file to write to.  
     * @throws ReportedException Any problems writing the file
     */
    public void write(File file) throws ReportedException {
        if (engine == null || formContext == null)
            return;

        Template template;

        try {
            template = engine.getTemplate(ANNOTATIONS_EXPORT_FORM);
        } catch (Exception e) {
            throw new ReportedException(e, "An error occured loading the report template: " + ANNOTATIONS_EXPORT_FORM);
        }

        try {
            FileWriter writer = new FileWriter(file);
            template.merge(formContext, writer);
            writer.close();
        } catch (Exception e) {
            throw new ReportedException(e, "An error occured attempting to write the file: " + file.getPath());
        }

    }

    private VelocityContext createContext(JuxtaSession juxtaSession) {
        VelocityContext context = new VelocityContext();
        DocumentManager documentManager = juxtaSession.getDocumentManager();

        ArrayList annotationEntries = new ArrayList();

        for (Iterator i = juxtaSession.getAnnotationManager().getAnnotations().iterator(); i.hasNext();) {
            Annotation annotation = (Annotation) i.next();
            if (annotation.isFromOldVersion()) {
                JuxtaDocument baseText = annotation.getBaseDocument(documentManager);
                JuxtaDocument witnessText = annotation.getWitnessDocument(documentManager);
                String entry = "<p><b>base:</b> " + baseText.getDocumentName() + "</p><p><b>witness:</b> "
                    + witnessText.getDocumentName() + "</p><p>notes:" + annotation.getNotes() + "</p><br/>";
                annotationEntries.add(entry);
            }
        }

        if (annotationEntries.isEmpty()) {
            annotationEntries.add("<p>There are no orphaned annotations in this comparison set.</p>");
        }

        HashMap annotationReportData = new HashMap();
        annotationReportData.put("annotations", annotationEntries);
        context.put("data", annotationReportData);
        return context;
    }

}
