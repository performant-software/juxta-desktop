/*
 * Created on Jun 07, 2007
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
 * @author Cortlandt Schoonover
 */

package edu.virginia.speclab.juxta.author.view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
import com.Ostermiller.util.Browser;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;

public class ProgressDialog extends JDialog {
	
	private JFrame parent;
	
	private JProgressBar progressBar;

	private SpringLayout springLayout;
	
    public static void main(String args[])
    {
        try
        {
            ProgressDialog dialog = new ProgressDialog(null);
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e)
                {
                    System.exit(0);
                }
            });
            dialog.setBounds(0, 0, 335, 78);
            dialog.setVisible(true);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
	public ProgressDialog(JFrame parent)
	{
		this.parent = parent;
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setTitle("Generating Critical Apparatus Report");
		springLayout = new SpringLayout();
		getContentPane().setLayout(springLayout);
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		getContentPane().add(progressBar);
		springLayout.putConstraint(SpringLayout.SOUTH, progressBar, 35, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, progressBar, -12, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.NORTH, progressBar, 10, SpringLayout.NORTH, getContentPane());
 
		
		JLabel label = new JLabel();
		label.setText("Please Wait: ");
		getContentPane().add(label);
        springLayout.putConstraint(SpringLayout.SOUTH, label, 0, SpringLayout.SOUTH, progressBar);
        springLayout.putConstraint(SpringLayout.NORTH, label, -25, SpringLayout.SOUTH, progressBar);
        springLayout.putConstraint(SpringLayout.WEST, label, 10, SpringLayout.WEST, getContentPane());
        
        springLayout.putConstraint(SpringLayout.WEST, progressBar, 5, SpringLayout.EAST, label);
        
	}
	
	public void setMaximum(int max)
	{
		progressBar.setMaximum(max);
	}
	
	public void setIndeterminate(boolean newValue)
	{
		progressBar.setIndeterminate(newValue);
	}

    public void display()
    {
        setBounds(parent.getX()+(parent.getWidth()/4), 
                parent.getY()+(parent.getHeight()/4), 335, 78);
      
        setVisible(true);
        progressBar.paintImmediately(parent.getX()+(parent.getWidth()/4), 
                parent.getY()+(parent.getHeight()/4), 335, 78);
    }
    
    public void close(File selectedFile)
    {
        setVisible(false);
        updateProgress(0);
        displayReportResult(selectedFile);
    }

	private void displayReportResult(File selectedFile) {
		//right now this brings up a modal confirm box
		//but it would be nice if it brought up the resultant
		//report webpage in the default browser
		if(JOptionPane.showConfirmDialog(this.parent, "The generated report has been saved at " + selectedFile.getAbsolutePath() + " Open in browser?", 
				"Success", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			
			//this code brought up the report html file
			//in the default browser, but sometimes that was too slow
			Browser.init();
			try
	        {
				URL url = selectedFile.toURL();
				String s = "file://" + url.getPath();
	            Browser.displayURL(s);
	        } 
	        catch (IOException e1)
	        {
	            ReportedException re = new ReportedException(e1,"Problem with automatically opening the file in default browser.");
	            ErrorHandler.handleException(re);                
	        }
		}
	}

	public void updateProgress(int nv) {
		progressBar.setValue(nv);
	}
}
