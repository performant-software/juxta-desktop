/*
 * Created on Jun 11, 2007
 */
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
 * @author Cortlandt
 */
package edu.virginia.speclab.util;

import edu.virginia.speclab.juxta.author.view.ImageDisplay;
import edu.virginia.speclab.ui.ImagePanel;

public class BackgroundImageLoader extends Thread {
	private String imageFile;
	private ImagePanel imagePanel;
	
	private boolean shouldLoad = true;

	public BackgroundImageLoader() {
		imageFile = null;
		imagePanel = null;
		shouldLoad = true;
	}

	public BackgroundImageLoader(String imageFile) {
		setImageFile(imageFile);
	}

	public BackgroundImageLoader(String imageFile, ImageDisplay display) {
		imagePanel = display;
		this.imageFile = imageFile;
	}

	public void run() {
		//wait to make sure another load hasn't been called
		try {
			sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//if another load has been called due to scrolling skip loading this one
		if(shouldLoad == true)
		{
			shouldLoad = false;
			try
			{
				imagePanel.setImage(imageFile);
			}
			catch (OutOfMemoryError e)
			{
				// TODO: Is there a way to recover from this? Now it just skips the load.
			}
			shouldLoad = true;
		}
	}

	public void setImageFile(String imageFile) {
		this.imageFile = imageFile;
	}

	public void setImagePanel(ImagePanel imagePanel) {
		this.imagePanel = imagePanel;
	}

}
