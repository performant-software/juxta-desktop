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
 
/**
 * @author Cortlandt Schoonover
 */

package edu.virginia.speclab.juxta.author.view;

import java.io.File;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.view.reports.AnnotationExportReport;

public class AnnotationExportRunner extends Thread {

	private JuxtaSession juxtaSession;
	private File selectedFile;
	
	public AnnotationExportRunner(JuxtaSession juxtaSession, File selectedFile) {
		super("AnnotationExportRunner");
		this.juxtaSession = juxtaSession;
		this.selectedFile = selectedFile;
	}
	
	public String getFileName()
	{
		return selectedFile.getName();
	}
	
	public String getFilePath()
	{
		return selectedFile.getAbsolutePath();
	}
	
	public File getSelectedFile()
	{
		return selectedFile;
	}
	
	public void run(){
			try {
				AnnotationExportReport report = new AnnotationExportReport(juxtaSession);
				report.write(selectedFile);
			} catch (ReportedException e) {
				ErrorHandler.handleException(e);
			}
	}
	
}
