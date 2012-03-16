package edu.virginia.speclab.juxta.author.view.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.apache.commons.io.IOUtils;

import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

public class LoginDialog extends JDialog {
    private JuxtaAuthorFrame juxtaFrame;
    private JTextField emailField;
    private JTextField passwordField;
    private WebServiceClient wsClient;
    
    public LoginDialog(JuxtaAuthorFrame frame) {
        super(frame);
        this.juxtaFrame = frame;
        this.wsClient = new WebServiceClient( frame.getWebServiceUrl() );
        
        setTitle("Juxta Web Export");
        setResizable(false);
        setSize(545, 385);
        setLocationRelativeTo( getParent() );
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        getContentPane().setBackground( Color.white );
        getContentPane().setLayout( new BorderLayout(15,15) );
        
        // add the logo to the top left
        JPanel logoPnl = new JPanel();
        logoPnl.setBackground( Color.white );
        logoPnl.setLayout( new BoxLayout(logoPnl, BoxLayout.Y_AXIS));
        logoPnl.add( new JLabel(JuxtaUserInterfaceStyle.JUXTA_LOGO) );
        logoPnl.add( Box.createVerticalGlue() );
        getContentPane().add(logoPnl, BorderLayout.WEST);
        
        getContentPane().add(createMainPanel(), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private Component createButtonPanel() {
        JPanel p = new JPanel();
        p.setBackground( Color.white );
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        JButton login = new JButton("Login");
        getRootPane().setDefaultButton(login);
        login.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onLoginClicked();
            }
        });

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancelClicked();
            }
        });

        p.add(Box.createHorizontalGlue());
        p.add( cancel );
        p.add( login );

        return p;
    }

    private Component createMainPanel() {
        JPanel p = new JPanel();
        p.setBackground( Color.white );
        p.setLayout( new BorderLayout(15,15) );
        
        try {
            String data = IOUtils.toString( LoginDialog.class.getResourceAsStream("/export.html") );
            JLabel txt = new JLabel( data );
            p.add(txt, BorderLayout.NORTH);
        } catch (IOException e) {
            // dunno. not much that can be done!
        }
        
        JPanel labels = new JPanel();
        labels.setBackground( Color.white );
        labels.setLayout(  new GridLayout(2,1));
        labels.add( new JLabel("Email:", SwingConstants.RIGHT ) );
        labels.add( new JLabel("Password:", SwingConstants.RIGHT ) );
        
        JPanel edits = new JPanel();
        edits.setBackground( Color.white );
        edits.setLayout( new GridLayout(2,1));
        this.emailField = new JTextField();
        this.passwordField = new JPasswordField();
        edits.add( this.emailField );
        edits.add( this.passwordField );
        JButton createBtn = new JButton("Create Free Account...");
        createBtn.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                onCreateAccountClicked();
            }
        });
        
        JPanel entry = new JPanel();
        entry.setBackground( Color.white );
        entry.setLayout( new BorderLayout(10,10) );
        entry.add( new JLabel("Login to juxtacommons.org:"), BorderLayout.NORTH);
        entry.add(labels, BorderLayout.WEST);
        entry.add(edits, BorderLayout.CENTER);
        entry.add(Box.createHorizontalStrut(50), BorderLayout.EAST);
        
        JPanel zz = new JPanel();
        zz.setBackground( Color.white );
        zz.setLayout( new BoxLayout(zz, BoxLayout.X_AXIS));
        zz.add( createBtn);
        zz.add( Box.createHorizontalGlue());
        entry.add(zz, BorderLayout.SOUTH);
        p.add(entry, BorderLayout.CENTER);

        return p;
    }
    
    private void onCreateAccountClicked() {
        CreateAccountDialog dlg = new CreateAccountDialog(this.juxtaFrame, this.wsClient);
        dlg.setModal(true);
        dlg.setVisible(true);
        if ( dlg.wasAccountCreated() ) {
            this.emailField.setText( dlg.getEmail() );
        }
    }
    
    private void onCancelClicked() {
        setVisible(false);
    }
    
    private void onLoginClicked() {
        // make sure all data has been specified
        String email = this.emailField.getText();
        String pass = this.passwordField.getText();
        if ( email.length()==0 || pass.length() == 0) {
            JOptionPane.showMessageDialog(this.juxtaFrame, 
                "An email address and password are required to login", 
                "Missing Data", JOptionPane.ERROR_MESSAGE);   
            return;
        }
        
        try {
            // validate login credentials and get workspace
            if (  this.wsClient.authenticate( email, pass) == false ) {
                JOptionPane.showMessageDialog(this.juxtaFrame, 
                    "Invalid email and/or password.", 
                    "Authentication Failed", JOptionPane.ERROR_MESSAGE);   
                return;
            }
        } catch ( IOException e ) {
            JOptionPane.showMessageDialog(this.juxtaFrame, 
                "An error occurred during login,\nplease try again later.\n\nError: "+e.getMessage(), 
                "System Error", JOptionPane.ERROR_MESSAGE);   
            return;
        }
        
        // save email / pass in frame for future exports
        // in this same session.
        this.juxtaFrame.setWebUserEmail(email);
        this.juxtaFrame.setWebUserPassword(pass);
        
        // launch the export dialog
        WebServiceExportDialog dlg = new WebServiceExportDialog(this.juxtaFrame, this.wsClient);
        dlg.setModal(true);
        setVisible(false);
        dlg.setVisible(true);
    }
}
