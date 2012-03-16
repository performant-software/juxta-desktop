package edu.virginia.speclab.juxta.author.view.templates;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

/**
 * This dialog displayes a list of avaiable templates and
 * provides buttons to create edit and delete them.
 * 
 * @author loufoster
 *
 */
public final class ListTemplatesDialog extends JDialog {
    
    private JButton moveBtn;
    private JComboBox configCombo;
    private JTable templateTable;
    private TemplateTableModel model;
    private JuxtaDocument tgtDocument;
    private JuxtaAuthorFrame juxta;
    private JButton deleteBtn;
    private Set<ParseTemplate> changedTemplates = new HashSet<ParseTemplate>();
    
    public ListTemplatesDialog(JuxtaAuthorFrame juxta ) {
        super(juxta);
        this.juxta = juxta;
        initUI();
    }
    
    public void setTargetDocument( final JuxtaDocument doc) {
        this.tgtDocument = doc;
        ParseTemplate t = getConfig().get( doc.getParseTemplateGuid());
        if ( t != null ) {
            int idx = getConfig().indexOf(t);
            this.templateTable.setRowSelectionInterval(idx, idx);
        } else {
            this.templateTable.setRowSelectionInterval(0, 0);
        }
    }
    
    private TemplateConfig getConfig() {
        ConfigType cfgType = getConfigType();
        return TemplateConfigManager.getInstance().getConfig( cfgType );
    }
    
    private ConfigType getConfigType() {
        return  (ConfigType)this.configCombo.getSelectedItem();
    }
    
    private void initUI() {
        setTitle("Edit Templates");
        setResizable(false);
        setSize(430, 250);
        setLocationRelativeTo( getParent() );
        ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        getContentPane().setLayout( new BoxLayout(getContentPane(), BoxLayout.Y_AXIS) );
       
        getContentPane().add( createTemplatePickPanel() );
        getContentPane().add( Box.createVerticalStrut(10) );
        getContentPane().add( createConfigPanel() );
        getContentPane().add( Box.createVerticalStrut(10) );
        getContentPane().add( createButtonBar());
        
        templateSelectionChanged();
    }
    
    private JPanel createConfigPanel() {
        this.model = new TemplateTableModel( getConfig() );
        this.templateTable = new JTable( this.model ){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
                Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
                if (rowIndex % 2 == 0 && !isCellSelected(rowIndex, vColIndex)) {
                    c.setBackground(JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE[1]);
                } else if (!isCellSelected(rowIndex, vColIndex)) {
                    c.setBackground(getBackground());
                }
                return c;
            }
        };
        
        this.templateTable.setSelectionForeground(Color.white);
        this.templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.templateTable.setRowSelectionAllowed(true);
        this.templateTable.setColumnSelectionAllowed(false);
        this.templateTable.setRowSelectionInterval(0, 0);
        this.templateTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {    
            public void valueChanged(ListSelectionEvent e) {
                if ( e.getValueIsAdjusting() == false ) {
                    templateSelectionChanged();
                }
            }
        });
        
        JPanel p = new JPanel( new BorderLayout(10,10));
        p.add( new JLabel("Templates:"), BorderLayout.NORTH);
        p.add(new JScrollPane(this.templateTable), BorderLayout.CENTER);
        return p;
    }

    protected void templateSelectionChanged() {
        // The default cannot be deleted in any case
        ParseTemplate template = getSelectedTemplate();
        if ( template != null ) { 
            if ( template.getName().equals("juxta-default")) {
                this.deleteBtn.setEnabled(false);
                return;
            }
            
            // master templates are not in active use
            // and can be deleted at any time (except above)
            if ( getConfigType().equals( ConfigType.MASTER) ) {
                this.deleteBtn.setEnabled(true);
                return;
            }
            
            // active templates cannot be deleted
            this.deleteBtn.setEnabled(true);
            for ( JuxtaDocument doc : this.juxta.getSession().getDocumentManager().getDocumentList() ) {
                if ( doc.getParseTemplateGuid().equals( template.getGuid() )) {
                    this.deleteBtn.setEnabled(false);
                }
            }
        }
    }

    /**
     * Create a panel that contains controls for picking the active
     * template configuration set
     * @return
     */
    private JPanel createTemplatePickPanel() {
        JPanel p = new JPanel(new BorderLayout(5,5));
        p.add(new JLabel("Configuration: "), BorderLayout.WEST);
        this.configCombo = new JComboBox( ConfigType.values() );
        this.configCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onConfigSelected();
            }
        });
        p.add( this.configCombo, BorderLayout.CENTER);
        return p;
    }

    private Component createButtonBar() {
        JPanel p = new JPanel();
        p.setLayout( new BoxLayout(p, BoxLayout.X_AXIS));
        
        JButton add = new JButton("New");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNewTemplate();
            }
        });
        
        this.deleteBtn = new JButton("Delete");
        this.deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDeleteTemplate();
            }
        });
        
        JButton edit = new JButton("Edit");
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onEditTemplate();
            }
        });
        
        this.moveBtn = new JButton("Archive");
        this.moveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCopyTemplate();
            }
        });
        
        JButton close = new JButton("Close");
        getRootPane().setDefaultButton( close );
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        p.add( add );
        p.add( this.deleteBtn );
        p.add( edit );
        p.add( this.moveBtn );
        p.add( Box.createGlue() );
        p.add( close );
        
        return p;
    }
    
    private ParseTemplate getSelectedTemplate() {
        int row = this.templateTable.getSelectedRow();
        if ( row != -1) {
            return getConfig().get(row);
        }
        return null;
    }
    
    /**
     * Notification that a new template config was selected
     */
    private void onConfigSelected() {
        this.model.setConfig( getConfig() );
        this.templateTable.setRowSelectionInterval(0, 0);
        if ( getConfigType().equals(ConfigType.MASTER) ) {
            this.moveBtn.setText("Restore");
        } else {
            this.moveBtn.setText("Archive");
        }
    }
    
    /**
     * Move template from session to master or master to session
     * based upon active configuration selection
     */
    private void onCopyTemplate() {
        ConfigType from = getConfigType();
        ConfigType to = from.equals(ConfigType.MASTER) ? ConfigType.SESSION : ConfigType.MASTER;
        ParseTemplate template = getSelectedTemplate();
        String msg = "Copy template \"" + template.getName()
            + "\"\n     from \"" + from 
            + "\"\n     to \"" + to +"\"?";
        
        // confirm the template copy....
        int resp = JOptionPane.showConfirmDialog(this, msg, 
                "Confirm Copy", JOptionPane.YES_NO_OPTION);
        if ( resp == JOptionPane.YES_OPTION ) {
            TemplateConfig toCfg = TemplateConfigManager.getInstance().getConfig(to);
            boolean doCopy = true;
            
            // If the copy is about to blow away existing data, prompt to continue
            if ( toCfg.contains(template) ) {
                int r2 = JOptionPane.showConfirmDialog(this, 
                    to+" already contains a template named \""
                    +template+"\". Overwrite it?", "Overwrite Existing", 
                    JOptionPane.YES_NO_OPTION);
                doCopy = (r2 == JOptionPane.YES_OPTION);
            }
            
            // if all is well, do the copy ad save the cfg
            if ( doCopy ) {
                toCfg.add(template);
                try {
                    toCfg.save();
                } catch (ReportedException e) {
                    ErrorHandler.handleException(e);
                }
            }
        }
    }
    
    /**
     * Create a new template based on the active template. Open
     * the edit template view to update default settings
     */
    private void onNewTemplate() {
        ParseTemplate t = getSelectedTemplate();
        if ( t == null) {
            JOptionPane.showMessageDialog(this, 
                    "Please select a source template", 
                    "Select Source Template", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String msg = "This template will be based on \""
            + t.getName()
            + "\".\n\nEnter the name for the new template:";
        String val = JOptionPane.showInputDialog(this, msg, "Template Name", JOptionPane.OK_CANCEL_OPTION);
        if ( val != null) {
            ParseTemplate newTemplate = getConfig().createTemplate(val,t);
            this.model.fireTableDataChanged();

            EditTemplateDialog dlg = new EditTemplateDialog(this.juxta);
            dlg.setConfigType( getConfigType() );
            dlg.setup(this.tgtDocument, newTemplate.getGuid() );
            dlg.setModal(true);
            dlg.setVisible(true);
            this.model.fireTableDataChanged();
            int idx = getConfig().indexOf(newTemplate);
            this.templateTable.setRowSelectionInterval(idx, idx);
        }
    }
    
    /**
     * Delete the currently selected template - exception -
     * don't delete the basic juxta template
     */
    private void onDeleteTemplate() {
        ParseTemplate template = getSelectedTemplate();
        int resp =  JOptionPane.showConfirmDialog(this, 
                "Delete template \""+template.getName()+"\"?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if ( resp == JOptionPane.YES_OPTION ) {
            getConfig().remove(template);
            this.model.fireTableDataChanged();
            this.templateTable.setRowSelectionInterval(0,0);
            saveTemplateConfig();
        }   
    }
    
    /**
     * Open the edit template view for the currently seleted template
     */
    private void onEditTemplate() {
        EditTemplateDialog dlg = new EditTemplateDialog( this.juxta );
        ParseTemplate currTemplate = getSelectedTemplate();
        int currIndex = getConfig().indexOf( currTemplate );
        dlg.setConfigType( getConfigType() );
        dlg.setup( this.tgtDocument, currTemplate.getGuid() );
        dlg.setModal(true);
        dlg.setVisible(true);
        if ( dlg.wasOkClicked() ) {
            this.changedTemplates.add( getSelectedTemplate() );
            this.model.fireTableDataChanged();
            this.templateTable.setRowSelectionInterval(currIndex, currIndex);
        }
    }
    
    /**
     * Check if the user of this dialog has made changes to any of the
     * parsing templates it manages
     * @return
     */
    public boolean hasTemplateChanges() {
        return (this.changedTemplates.size() > 0);
    }
    
    /**
     * Get a set of all templates that have been modified by this dialog
     * @return
     */
    public Set<ParseTemplate> getModifiedTemplates() {
        return this.changedTemplates;
    }
    
    /**
     * Save the current config and pop an error box if something goes south
     */
    private void saveTemplateConfig() {
        try {
            getConfig().save();
        } catch (ReportedException e ) {
            ErrorHandler.handleException(e);
        }
    }
    
    /**
     * Table mode for the parse action configuration
     */
    class TemplateTableModel extends AbstractTableModel {
        private TemplateConfig templateCfg;

        public TemplateTableModel( TemplateConfig cfg ) {
            this.templateCfg = cfg;
        }
        
        public void setConfig( TemplateConfig cfg )
        {
            this.templateCfg = cfg;
            this.fireTableDataChanged();
        }
        public int getRowCount() {
            return this.templateCfg.size();
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Name";
            }
            if (columnIndex == 1) {
                return "Schema";
            }
            return "Default for Schema";
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ParseTemplate t = this.templateCfg.get(rowIndex);
            if (columnIndex == 0) {
                return t.getName();
            }

            if (columnIndex == 1) {
                return t.getRootTagName();
            }

            return Boolean.toString(t.isDefault());
        }
    }


}
