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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.util.SimpleLogger;

public class FileTreePanel extends JPanel {

    private class TreeNodeClickHandler extends MouseAdapter
    {
       public void mousePressed(MouseEvent evt)
       {
          if ( (evt.getSource() instanceof JTree) &&
               (evt.getClickCount() > 1) )
          {
        	  DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
        	  if( node.getUserObject() instanceof FileTreePanelModel.FileNode ) {        		  
        		  FileTreePanelModel.FileNode nodeData = (FileTreePanelModel.FileNode) node.getUserObject();
        		  if( nodeData.isValid() && !nodeData.getFile().isDirectory() )
        			  fireSelected(nodeData.getFile());
        	  }
          }
       }
    }
    
    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    	@Override
    	public Component getTreeCellRendererComponent(JTree tree, Object value,
    			boolean sel, boolean expanded, boolean leaf, int row,
    			boolean hasFocus) {
    		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
    				row, hasFocus);
    		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    		if( node.getUserObject() instanceof FileTreePanelModel.FileNode ) {
        		FileTreePanelModel.FileNode treeNode = (FileTreePanelModel.FileNode) node.getUserObject();
        		if( !treeNode.isValid() ) setForeground(Color.GRAY);    			
    		}
    		return this;
    	}
    }
    
	private JTree fileTree;
	private LinkedList treeListeners;
	private File baseDirectory;
	
	public FileTreePanel( File baseDirectory ) {

        this.fileTree = new JTree( createFileTreePanelModel( baseDirectory ) ); 
		this.baseDirectory = baseDirectory;
		
        this.treeListeners = new LinkedList();
	    
        fileTree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
        fileTree.setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        // listen for double clicking on the tree   
        fileTree.addMouseListener(new TreeNodeClickHandler());
  
        //Create the scroll pane and add the tree to it. 
        JScrollPane treeView = new JScrollPane(fileTree);

        this.setLayout(new BorderLayout());
        
        //Add the split pane to this panel.
        add(treeView,BorderLayout.CENTER);		
	}
	
	private void fireSelected( File selectedFile ) {
		for( Iterator i = treeListeners.iterator(); i.hasNext(); ) {
			FileTreePanelSelectionListener listener = (FileTreePanelSelectionListener) i.next();
			listener.fileTreePanelFileSelected(selectedFile);
		}
	}
	
	public void addListener( FileTreePanelSelectionListener listener ) {
		treeListeners.add(listener);
	}

	public void removeListener( FileTreePanelSelectionListener listener ) {
		treeListeners.remove(listener);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleLogger.initConsoleLogging();
        SimpleLogger.setLoggingLevel(10);
		 
        JFrame testFrame = new JFrame("File Tree Panel");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setSize(300,400);
        testFrame.setLocation(500,300);
        
        FileTreePanel panel = new FileTreePanel( new File(".") );
        
        testFrame.getContentPane().setLayout( new BorderLayout() );
        testFrame.getContentPane().add(panel, BorderLayout.CENTER );
        testFrame.setVisible(true);
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}
	
	private FileTreePanelModel createFileTreePanelModel( File baseDirectory ) {
		FileTreePanelModel model = null;
		try {
			model = new FileTreePanelModel( baseDirectory );
			model.addValidFileExtenstion("xml");		
			model.addValidFileExtenstion("txt");		
			model.scanDirectory();
		}
		catch( IOException e ) {
			ErrorHandler.handleException(e);
		}
		return model;
	}

	public void setBaseDirectory(File baseDirectory) {
		DefaultMutableTreeNode rootNode = createFileTreePanelModel( baseDirectory );
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		fileTree.setModel(treeModel);
		this.baseDirectory = baseDirectory;
	}

}
