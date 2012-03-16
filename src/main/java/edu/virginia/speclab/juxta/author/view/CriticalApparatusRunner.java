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
import edu.virginia.speclab.juxta.author.model.CriticalApparatus;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.view.reports.CriticalApparartusReport;

public class CriticalApparatusRunner extends Thread {

	private JuxtaSession session;
	private File selectedFile;
	private CriticalApparatus ca;
	private String title;
	
	public CriticalApparatusRunner(JuxtaSession session, File selectedFile, String title) {
		super("CriticalApparatusRunner");
		this.session = session;
		this.selectedFile = selectedFile;
		this.title = title;
	}

	public float getProgress()
	{
		return ca.getProgress();
	}
	
	public String getFileName()
	{
		return selectedFile.getName();
	}
	
	public String getFilePath()
	{
		String s = null;
		//when this was returning a URL the following block was used
		/*try {
			URL url = selectedFile.toURL();
			s = url.getPath();
		} catch (MalformedURLException e) {
            ReportedException re = new ReportedException(e,e.getMessage());
            ErrorHandler.handleException(re);  
		}*/
		s = selectedFile.getAbsolutePath();
		return s;
	}
	
	public File getSelectedFile()
	{
		return selectedFile;
	}
	
	//override
	public void run(){
			try {
				ca = new CriticalApparatus(session);
				ca.runCriticalApparatus();
				CriticalApparartusReport report = new CriticalApparartusReport(ca,title);
				report.write(selectedFile);
			} catch (ReportedException e) {
				ErrorHandler.handleException(e);
			}
	}
	
	

}
