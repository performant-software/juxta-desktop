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

public class ProgressDialogRunner extends Thread {
	
	private ProgressDialog progressDialog;
	private CriticalApparatusRunner criticalApparatusRunner;

	public ProgressDialogRunner(ProgressDialog progressDialog, CriticalApparatusRunner criticalApparatusRunner) {
		super("ProgressDialogRunner");
		this.progressDialog = progressDialog;
		this.criticalApparatusRunner = criticalApparatusRunner;
	}

	//override
	public void run() {
		progressDialog.display();
		progressDialog.setMaximum(100);
		criticalApparatusRunner.start();
		while(criticalApparatusRunner.isAlive())
		{
			try {
				//don't check all the time -- too slow
				sleep(250);
				//update the progress based on the critical apparatus loop
				progressDialog.updateProgress(Math.round(criticalApparatusRunner.getProgress() * 100));
				//keep in the foreground
				progressDialog.requestFocus();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		progressDialog.close(criticalApparatusRunner.getSelectedFile());
	}

}
