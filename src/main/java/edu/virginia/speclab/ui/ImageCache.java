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
 * this class caches images for a document for more fluid scrolling with images
 * 
 * MAX_IMAGE_SIZE regulates what the disk size of all the files in memory
 * can be. If there are out of memory problems, it may need to be lowered.
 */

package edu.virginia.speclab.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import edu.virginia.speclab.util.ImageLoader;

public class ImageCache 
{
	private static final int INITIAL_MAP_SIZE = 10;
    private static final ImageLoader imageLoader = new ImageLoader(null);
	private static final long MAX_IMAGE_SIZE = 750000;
    private long totalImageSize;
	
	private HashMap cache;

	public ImageCache()
	{
		totalImageSize = 0;
		cache = new HashMap(INITIAL_MAP_SIZE);
	}
	
	public BufferedImage getImage(String imageFile)
	{
		BufferedImage image = null;
		if(cache.containsKey(imageFile))
		{
			image = (BufferedImage) cache.get(imageFile);
		}
		else
		{
			File f = new File(imageFile);
			totalImageSize+=f.length();
			if(totalImageSize > MAX_IMAGE_SIZE)
			{
				clear();
				totalImageSize+=f.length();
			}
			
			image = imageLoader.loadImageFile(imageFile);
			cache.put(imageFile, image);
		}
		return image;	
	}
	
	public void clear()
	{
		totalImageSize=0;
		cache.clear();
	}
}
