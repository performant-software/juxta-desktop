/*
 *  Copyright 2002-2011 The Rector and Visitors of the
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
package edu.virginia.speclab.juxta.author.view.templates;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate.Action;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate.Behavior;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;

/**
 * Customized JTable that handles display and edit of juxta parse template data
 * 
 * @author loufoster
 *
 */
public class TemplateConfigTable extends JTable {
    public enum  Mode {
        READ_ONLY, EDIT;
    }
    private final Mode mode;
    private TableModel model;
    
    public TemplateConfigTable( final Mode mode ) {
        super();
        
        this.mode = mode;
        this.model = new TableModel();
        setModel( this.model );
 
        setSelectionBackground(JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE[3]);
        setSelectionForeground(Color.white);
        
        // setup table controls mased on mode
        if ( this.mode.equals(Mode.READ_ONLY)) {
            
            getColumnModel().getColumn(1).setCellRenderer( new DefaultTableCellRenderer() );
            getColumnModel().getColumn(2).setCellRenderer( new DefaultTableCellRenderer());
        } else {

            // custom combo editor/renderer for tag actions column
            CustomComboBoxRenderer comboRenderer = new CustomComboBoxRenderer(ParseTemplate.Action.values());
            JComboBox comboBox = new JComboBox(ParseTemplate.Action.values());
            getColumnModel().getColumn(1).setCellEditor(
                    new DefaultCellEditor(comboBox));    
            getColumnModel().getColumn(1).setCellRenderer( comboRenderer );   
      
            // Custom checkbox renderer for default setting
            CheckboxRenderer checkRenderer = new CheckboxRenderer();
            getColumnModel().getColumn(2).setCellRenderer( checkRenderer );
        }
        
        setRowHeight(25);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getColumnModel().getColumn(0).setPreferredWidth(125);
        getColumnModel().getColumn(1).setPreferredWidth(150);
        getColumnModel().getColumn(2).setPreferredWidth(75);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }
    
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
        Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
        if (rowIndex % 2 == 0 && !isCellSelected(rowIndex, vColIndex)) {
            c.setBackground(JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE[1]);
        } else if (!isCellSelected(rowIndex, vColIndex)) {
            // If not shaded, match the table's background
            c.setBackground(getBackground());
        }
        return c;
    }
    
    public void setTemplate(final ParseTemplate template ) {
        this.model.setTemplate(template);
    }
    
    public ParseTemplate getTemplate() {
        return this.model.template;
    }
    
    /**
     * Custom renderer used to show check box for new line
     */
    private class CheckboxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckboxRenderer() {
            super();
            this.setOpaque(true);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setEnabled( true );
            setSelected( (Boolean)value );
            return this;
        }
    }

    /**
     * Custom renderer used to show combo box for tag action
     */
    class CustomComboBoxRenderer extends JComboBox implements TableCellRenderer {
        public CustomComboBoxRenderer(Object[] items) {
            super(items);
            this.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                if (row % 2 == 0) {
                    setBackground(JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE[1]);
                } else {
                    setBackground(table.getBackground());
                }
                setForeground(table.getForeground());
            }

            // Select the current value
            setSelectedItem(value);
            setEnabled( true );
            return this;
        }
    }

    /**
     * Table model for the parse action configuration
     */
    class TableModel extends AbstractTableModel {
        private ParseTemplate template;
        
        public void setTemplate(ParseTemplate template ) {
            this.template = template;
            Collections.sort(this.template.getBehaviors());
            fireTableDataChanged();
        }

        public int getRowCount() {
            return this.template.getBehaviors().size();
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Tag";
            }
            if (columnIndex == 1) {
                return "Behavior";
            }
            return "Newline?";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return ( TemplateConfigTable.this.mode.equals(Mode.EDIT) && columnIndex > 0 );
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return this.template.getBehaviors().get(rowIndex).getTagName();
            }

            if (columnIndex == 1) {
                return this.template.getBehaviors().get(rowIndex).getAction();
            }

            return this.template.getBehaviors().get(rowIndex).getNewLine();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex >= this.template.getBehaviors().size()) {
                return;
            }

            Behavior old = this.template.getBehaviors().get(rowIndex);

            if (columnIndex == 1) {
                this.template.getBehaviors().set(rowIndex, new Behavior(old.getTagName(), (Action) aValue, old.getNewLine()));
                this.fireTableCellUpdated(rowIndex, columnIndex);

            } else {
                this.template.getBehaviors().set(rowIndex, new Behavior(old.getTagName(), old.getAction(), (Boolean) aValue));
                this.fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
