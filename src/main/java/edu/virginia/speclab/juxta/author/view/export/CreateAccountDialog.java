package edu.virginia.speclab.juxta.author.view.export;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.WindowConstants;

import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;

public class CreateAccountDialog extends JDialog {
    private WebServiceClient wsClient;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField passwordField;
    private JTextField confirmField;
    private boolean created;
    private JTextField emailConfField;
    
    CreateAccountDialog( JuxtaAuthorFrame frame, final WebServiceClient webServiceClient ) {
        super(frame);
        this.wsClient = webServiceClient;
        this.created = false;
        
        setTitle("Create Juxta Account");
        setResizable(false);
        setSize(420, 230);
        setLocationRelativeTo( getParent() );
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        getContentPane().setBackground(Color.white);
        getContentPane().setLayout( new BorderLayout(0,10) );
        
        getContentPane().add( createDataPanel(),BorderLayout.CENTER );
        getContentPane().add( createButtonPanel(),BorderLayout.SOUTH );
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    private JPanel createDataPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBackground(Color.white);

        JPanel names = new JPanel(new GridLayout(5, 1, 5, 5));
        names.setBackground(Color.white);
        names.add(new JLabel("Name:", JLabel.RIGHT));
        names.add(new JLabel("Email:", JLabel.RIGHT));
        names.add(new JLabel("Confirm Email:", JLabel.RIGHT));
        names.add(new JLabel("Password", JLabel.RIGHT));
        names.add(new JLabel("Confirm Password:", JLabel.RIGHT));
        panel.add(names, BorderLayout.WEST);

        JPanel edit = new JPanel(new GridLayout(5, 1, 5, 5));
        edit.setBackground(Color.white);
        this.nameField = new JTextField();
        this.emailField = new JTextField();
        this.emailConfField = new JTextField();
        this.passwordField = new JPasswordField();
        this.confirmField = new JPasswordField();
        edit.add(this.nameField);
        edit.add(this.emailField);
        edit.add(this.emailConfField);
        edit.add(this.passwordField);
        edit.add(this.confirmField);
        panel.add(edit, BorderLayout.CENTER);

        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.white);
        p.setBorder( BorderFactory.createEmptyBorder(5,0,0,0));
        p.setLayout( new BoxLayout(p, BoxLayout.X_AXIS));
        
        JButton createBtn = new JButton("Create");
        getRootPane().setDefaultButton(  createBtn );
        createBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCreateClicked();
            }
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancelClicked();
            }
        });
        
        p.add( Box.createHorizontalGlue());
        p.add( createBtn );
        p.add( cancelBtn);
        
        return p;
    }
    
    private void onCreateClicked() {
        final String name = this.nameField.getText();
        final String email = this.emailField.getText();
        final String emailConf = this.emailConfField.getText();
        final String pass = this.passwordField.getText();
        final String confirm = this.confirmField.getText();
        if ( name.length() == 0 || email.length() == 0 || pass.length() == 0 || 
             confirm.length() == 0 || emailConf.length() == 0 ) {
            JOptionPane.showMessageDialog(null, "Please enter data for all fields.", 
                "Error", JOptionPane.OK_OPTION);
            return;
        }
        if ( pass.equals( confirm) == false ) {
            JOptionPane.showMessageDialog(null, "Passwords do not match.", 
                "Error", JOptionPane.OK_OPTION);
            return;
        }
        if ( email.equals( emailConf ) == false ) {
            JOptionPane.showMessageDialog(null, "Emails do not match.", 
                "Error", JOptionPane.OK_OPTION);
            return;
        }
        try {
            this.wsClient.createAccount(name, email, pass, confirm);
            StringBuilder msg = new StringBuilder();
            msg.append("Thank you for signing up for a Juxta user account.\n\n");
            msg.append("An account activation email has been sent\n");
            msg.append("to '").append(email).append("'. ");
            msg.append("Follow the enclosed instructions\nto begin using your account.");
            JOptionPane.showMessageDialog(null,
                msg.toString(),
                "Account Created",
                JOptionPane.INFORMATION_MESSAGE);
            this.created = true;
            setVisible(false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), 
                "Error", JOptionPane.OK_OPTION);
        }
    }
    
    public boolean wasAccountCreated() {
        return this.created;
    }
    
    public String getEmail() {
        return this.emailField.getText();
    }
    
    private void onCancelClicked() {
        setVisible(false);
    }
}
