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

package edu.virginia.speclab.ui.filetree;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.virginia.speclab.util.RelativePath;

public class FileTreePanelModel extends DefaultMutableTreeNode {

	public class FileNode {
		private File file;
		
		public FileNode( File file ) {
			this.file = file;
		}
		
		public String toString() {
			return file.getName();
		}

		public File getFile() {
			return file;
		}

		public boolean isValid() {
			if( file.getName().startsWith(".") ) return false;
			if( file.isDirectory() ) return true;
			
			for( Iterator i = validFileExtensions.iterator(); i.hasNext(); ) {
				String extension = (String) i.next();
				if( file.getName().endsWith(extension) ) return true;
			}
			
			return false;		
		}
	}	
	
	private File baseDirectory;
	private LinkedList validFileExtensions;
	
	public FileTreePanelModel( File baseDirectory )  {
		super(baseDirectory.getAbsolutePath());
		this.baseDirectory = baseDirectory;		
		validFileExtensions = new LinkedList();
	}
	
	public void addValidFileExtenstion( String extension ) {
		this.validFileExtensions.add("."+extension);
	}
	
	public void scanDirectory() throws IOException {
		recursivelyScanDirectory(this.baseDirectory,this);
	}
	
	private int recursivelyScanDirectory( File dir, DefaultMutableTreeNode parentNode ) throws IOException {
		
		int count = 0;
		File fileList[] = dir.listFiles();
		for( int i=0; i < fileList.length; i++ ) {
			File entry = new File( RelativePath.appendToPath( dir.getPath(), fileList[i].getName() ) );
			FileNode node = new FileNode( entry );
			if (node.isValid())
			{
				if( entry.isDirectory() ) {
					// add folder to tree if there are any files under it
					DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(node);
					parentNode.add(folderNode);
					int numAdded = recursivelyScanDirectory(entry,folderNode);
					if (numAdded == 0)
						parentNode.remove(folderNode);
					else
						++count;
				}
				else {
					// add file to tree
					DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(node);
					parentNode.add(fileNode);
					++count;
				}
			}
		}
		return count;
	}
	
}
