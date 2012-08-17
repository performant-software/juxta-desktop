/*
 *  Copyright 2002-2012 The Rector and Visitors of the
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
package edu.virginia.speclab.juxta.author.view.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.apache.commons.io.IOUtils;

import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.export.WebServiceClient.RequestStatus;
import edu.virginia.speclab.juxta.author.view.export.WebServiceClient.RequestStatus.Status;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

/**
 * Dialog to setup, init and monitor export of current juxta session
 * to the web service
 * 
 * @author loufoster
 *
 */
@SuppressWarnings("serial")
public class WebServiceExportDialog extends JDialog {
    private final JuxtaAuthorFrame juxtaFrame;
    private JButton exportBtn;
    private JButton closeBtn;
    private JTextField nameEdit;
    private JTextArea descriptionEdit;
    private String exportTaskId = null;
    private WebServiceClient wsClient;
    private JPanel setupPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JLabel workAnimation;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> statusTask;
    private boolean exported = false;
    
    public WebServiceExportDialog(JuxtaAuthorFrame frame) {
        this(frame, null);
    }
    
    public WebServiceExportDialog(JuxtaAuthorFrame frame, WebServiceClient wsClient) {
        super(frame);
        this.juxtaFrame = frame;
        
        if ( wsClient == null ) {
            this.wsClient = new WebServiceClient( frame.getWebServiceUrl());
        } else {
            this.wsClient = wsClient;
        }
        
        // size and title the main dialog body
        setTitle("Juxta Web Export");
        setResizable(false);
        setSize(495, 345);
        setLocationRelativeTo( getParent() );
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        getContentPane().setLayout( new BorderLayout(15,15) );
        getContentPane().setBackground( Color.white );
        
        // add the logo to the top left
        JPanel logoPnl = new JPanel();
        logoPnl.setBackground( Color.white );
        logoPnl.setLayout( new BoxLayout(logoPnl, BoxLayout.Y_AXIS));
        logoPnl.add( new JLabel(JuxtaUserInterfaceStyle.JUXTA_LOGO) );
        logoPnl.add( Box.createVerticalGlue() );
        getContentPane().add(logoPnl, BorderLayout.WEST);

        createSetupPane();
        createStatusPane();
        getContentPane().add( this.setupPanel, BorderLayout.CENTER );
        getContentPane().add( createButtonBar(), BorderLayout.SOUTH );
        
        // create a single scheduled executor to periodically
        // check for export status. There can only be one at any
        // give time, so a pool seemed unnecessary
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    private void authenticateUser() {
        try {
            // validate login credentials and get workspace
            if ( this.wsClient.authenticate( this.juxtaFrame.getWebUserEmail(), 
                // if this somehow fails, blow away the saved credentials
                // show a warning and bail. Next time export is cliced
                // the user will land on the login page.
                this.juxtaFrame.getWebUserPassword()) == false ) {
                this.juxtaFrame.setWebUserEmail("");
                this.juxtaFrame.setWebUserPassword("");
                throw new IOException("Invalid email and/or password");
            }
        } catch ( IOException e ) {
            JOptionPane.showMessageDialog(this.juxtaFrame, 
                "An error occurred during login,\nplease try again later.\n\nError: "+e.getMessage(), 
                "System Error", JOptionPane.ERROR_MESSAGE);   
            setVisible( false );
        }
    }

    /**
     * Create a UI panel to show export progress
     */
    private void createStatusPane() {
        this.statusPanel = new JPanel();
        this.statusPanel.setBackground(Color.white);
        this.statusPanel.setLayout( new BorderLayout(5,5));
        this.statusPanel.setBorder( BorderFactory.createEmptyBorder(15,0,0,0));
        
        JPanel content = new JPanel();
        content.setBackground(Color.white);
        content.setLayout( new BoxLayout(content, BoxLayout.Y_AXIS) );
        content.add( Box.createVerticalStrut(30));
        JLabel l = new JLabel("Status", SwingConstants.CENTER);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setFont(JuxtaUserInterfaceStyle.LARGE_FONT);
        content.add(l  );
        content.add( Box.createVerticalStrut(10));
        this.statusLabel = new JLabel("", SwingConstants.CENTER);
        this.statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.setFont(JuxtaUserInterfaceStyle.NORMAL_FONT);
        content.add( this.statusLabel);
        this.statusPanel.add(content, BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.setBackground(Color.white);
        p.setLayout( new BoxLayout(p, BoxLayout.X_AXIS));
        p.add( Box.createHorizontalGlue());
        this.workAnimation = new JLabel();
        this.workAnimation.setIcon(JuxtaUserInterfaceStyle.WORKING_ANIMATION);
        p.add( this.workAnimation);
        p.add( Box.createHorizontalGlue());
        this.statusPanel.add(p, BorderLayout.NORTH);
    }

    /**
     * Creates the UI panel containing all of the fields necessary to upload
     * the current session data to the web service
     */
    private void createSetupPane() {
        this.setupPanel = new JPanel();
        this.setupPanel.setLayout( new BorderLayout() );
        this.setupPanel.setBackground( Color.white );
        
        try {
            String data = IOUtils.toString( LoginDialog.class.getResourceAsStream("/export2.html") );
            data = data.replace("{LIST}", formatWitnessList());
            JLabel txt = new JLabel( data );
            this.setupPanel.add(txt, BorderLayout.NORTH);
        } catch (IOException e) {
            // dunno. not much that can be done!
        }
        
        // ugly layout code to follow. avert your eyes
        JPanel data = new JPanel();
        data.setLayout(new BorderLayout());
        data.setBackground( Color.white);
        
        JPanel names = new JPanel();
        names.setLayout( new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setBackground(Color.white);
        JLabel l = new JLabel("Name:", SwingConstants.RIGHT);
        l.setPreferredSize(new Dimension(100,20) );
        l.setMaximumSize(new Dimension(100,20) );
        l.setAlignmentX(RIGHT_ALIGNMENT);
        names.add( l);
        names.add( Box.createVerticalStrut(5));
        
        JLabel l2 = new JLabel("Description:", SwingConstants.RIGHT);
        l2.setPreferredSize(new Dimension(100,20) );
        l2.setMaximumSize(new Dimension(100,20) );
        l2.setAlignmentX(RIGHT_ALIGNMENT);
        names.add( l2 );
        data.add( names, BorderLayout.WEST );
        
        JPanel edits = new JPanel();
        edits.setBackground(Color.white);
        edits.setLayout( new BoxLayout(edits,BoxLayout.Y_AXIS));
        this.nameEdit = new JTextField();
        this.nameEdit.setPreferredSize( new Dimension(200,22) );
        this.nameEdit.setMaximumSize( new Dimension(200,22) );
        File saveFile = this.juxtaFrame.getSession().getSaveFile();
        if (saveFile == null) {
            this.nameEdit.setText("new_session");
        } else {
            String name = saveFile.getName();
            this.nameEdit.setText(name.substring(0, name.lastIndexOf('.')));
        } 
        edits.add( this.nameEdit );
        
        this.descriptionEdit = new JTextArea(3,0);
        this.descriptionEdit.setLineWrap(true);
        this.descriptionEdit.setWrapStyleWord(true);
        
        JScrollPane sp = new JScrollPane(this.descriptionEdit);
        sp.setPreferredSize( new Dimension(194, 60));
        sp.setMaximumSize( new Dimension(194, 300));
        edits.add( Box.createVerticalStrut(4));
        edits.add( sp );
        
        data.add( edits, BorderLayout.CENTER);
        
        this.setupPanel.add(data, BorderLayout.SOUTH);
    }
    
    private String formatWitnessList() {
        StringBuffer out = new StringBuffer();
        for (JuxtaDocument d : this.juxtaFrame.getSession().getDocumentManager().getDocumentList() ) {
            if ( out.length()> 0) {
                out.append(", ");
            }
            out.append(d.getDocumentName());
        }
        return out.toString();
    }

    private Component createButtonBar() {
        JPanel p = new JPanel();
        p.setBackground(Color.white);
        p.setBorder( BorderFactory.createEmptyBorder(5,0,0,0));
        p.setLayout( new BoxLayout(p, BoxLayout.X_AXIS));
        
        this.exportBtn = new JButton("Export");
        this.exportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( exportBtn.getText().equals("View")) {
                    viewWebServiceData();
                } else {
                    onExportClicked();
                }
            }
        });
        
        this.closeBtn = new JButton("Cancel");
        getRootPane().setDefaultButton(  this.exportBtn );
        this.closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancelClicked();
            }
        });
       
        p.add( Box.createHorizontalGlue() );
        p.add( this.closeBtn );
        p.add( this.exportBtn);
        
        return p;
    }
    
    private void viewWebServiceData() {
      String urlStr = this.wsClient.getBaseUrl();
      urlStr += "/sets/"+this.exportTaskId+"/view?mode=heatmap";
      try {
          URI uri = new URI( urlStr );
          java.awt.Desktop.getDesktop().browse(uri);
      } catch (Exception e ) {
          String msg = "Unable to view comparison set. You can verify the export by\n"+
              "opening a browser and going to :\n\n"+urlStr;
          JOptionPane.showMessageDialog(this.juxtaFrame, msg, 
                                        "Error", JOptionPane.OK_OPTION);
      }
    }
    
    private void onExportClicked() {       
        File jxt = null;
        boolean errorHandled = false;
        try {
            // make sure all data has been specified
            String setName = this.nameEdit.getText();
            if (setName.length() == 0 ) {
                JOptionPane.showMessageDialog(this.juxtaFrame, 
                    "Please enter an export name for this session", 
                    "Missing Data", JOptionPane.ERROR_MESSAGE);   
                return;
            }
            
            // dump the session to a temporary jxt file 
            // and send this to the web service. Note that the
            // false param on the save marks this as a temporary one
            // that does not alter the session itself
            jxt = File.createTempFile("export", ".jxt");
            this.juxtaFrame.getSession().saveSessionForExport( jxt );   
            final String desc = this.descriptionEdit.getText();

            try {
                // do the export
                showStatusUI();
                authenticateUser();
                this.exportTaskId = this.wsClient.beginExport( jxt, setName, desc );
            } catch (Exception e) {
                // catch naming conflict errors and prompt to overwrite
                if ( e.getMessage().contains("Conflict")) {
                    int resp = JOptionPane.showConfirmDialog(this.juxtaFrame, 
                        "A comparison set with this name already exists. Overwrite it?", 
                        "Overwrite", JOptionPane.YES_NO_OPTION);
                    if ( resp == JOptionPane.YES_OPTION ) {
                        this.statusLabel.setText("Re-Exporting JXT");
                        this.exportTaskId = this.wsClient.beginExport( jxt, setName, desc, true );
                    } else {
                        errorHandled = true;
                        showSetupUI();
                        return;
                    }
                } else {
                    RequestStatus status = new RequestStatus( Status.FAILED, e.getMessage());
                    displayStatus( status );
                    return;
                }
            }
            
            // kick off a thread that will do the
            // status check requests.
            this.statusTask = this.scheduler.scheduleAtFixedRate( 
                new Runnable() {
                    public void run() {
                        final WebServiceClient client = WebServiceExportDialog.this.wsClient;
                        final String id = WebServiceExportDialog.this.exportTaskId;
                        try {
                            RequestStatus status = client.getExportStatus( id );  
                            displayStatus( status );
                            if ( status.isTerminated() ) {
                                WebServiceExportDialog.this.statusTask.cancel(false);
                            }
                        } catch (IOException e) {
                            RequestStatus status = new RequestStatus( Status.FAILED, e.getMessage());
                            displayStatus( status );
                            WebServiceExportDialog.this.statusTask.cancel(false);
                        }
                    }
                }, 2, 2, TimeUnit.SECONDS);
            
        } catch (Exception e ) {           
            if ( errorHandled == false ) {
                JOptionPane.showMessageDialog(this.juxtaFrame, 
                    "Unable to export session:\n   "+e.getMessage(), 
                    "Failure", JOptionPane.ERROR_MESSAGE);  
                showSetupUI();
            }
        } finally {
            if ( jxt != null ) {
                jxt.delete();
            }
        }
    }

    private void displayStatus( RequestStatus status ) {
        if ( status.getStatus().equals( RequestStatus.Status.PROCESSING )) {
            this.statusLabel.setText( status.getMessage() ); 
        } else { 
            
            if ( status.getStatus().equals(RequestStatus.Status.CANCELED)) {
                this.workAnimation.setIcon(JuxtaUserInterfaceStyle.EXPORT_FAIL);
                this.statusLabel.setText( "Export was canceled" );
            } else {
                this.workAnimation.setIcon(JuxtaUserInterfaceStyle.EXPORT_FAIL);
                String msg = status.getMessage();
                if ( msg.contains("<html")) {
                    msg = extractHtmlError(msg);
                }
                this.statusLabel.setText( "<html><body><center><p><b>"+status.getStatus().toString()
                    +"</b></p><p>" + msg+"</p></center></body></html>");
                this.exportBtn.setText("Retry");
            }
            this.closeBtn.setText("Done");
            this.exportBtn.setEnabled(true);
            if (status.getStatus().equals(RequestStatus.Status.COMPLETE)) {
                this.workAnimation.setIcon(JuxtaUserInterfaceStyle.EXPORT_SUCCESS);
                this.exportBtn.setText("View");
                this.exported = true;
                this.juxtaFrame.getSession().setExported( true );
            }
        }
    }    
    
    private String extractHtmlError(String msg) {
        int pos = msg.indexOf("<pre>");
        if ( pos > -1 ) {
            int end = msg.indexOf("</pre>");
            return msg.substring(pos+5, end);
        }
        return msg;
    }

    public final boolean wasExported() {
        return this.exported;
    }
    
    private void showStatusUI() {
        getContentPane().remove(this.setupPanel);
        getContentPane().add(this.statusPanel, BorderLayout.CENTER);
        this.workAnimation.setVisible(true);
        this.statusLabel.setText("Uploading JXT file...");
        this.exportBtn.setEnabled(false);
        this.repaint();
    }
    
    private void showSetupUI() {
        getContentPane().remove(this.statusPanel);
        getContentPane().add(this.setupPanel, BorderLayout.CENTER);
        this.exportBtn.setEnabled(true);
        this.repaint();
    }

    private void onCancelClicked() {
               
        // if the export is done, just close the window
        if ( this.exported ) {
            setVisible(false);
            return;
        }
        
        // if the export button is enabled,
        // an export is not in-process. just kill the dialog.
        this.scheduler.shutdownNow();
        if ( this.exportBtn.isEnabled() ) {
            setVisible(false);
        } else {
            // export running, abort it
            try {
                // send one request and kill the dislog.
                // The request may take a bit to be processed, but
                // no need to keep non-responsive dialog up
                this.wsClient.cancelExport( this.exportTaskId );
                setVisible(false);
            } catch (IOException e) {
                e.printStackTrace();
                setVisible(false);
            }
        }
    }
}
