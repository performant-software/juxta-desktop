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
 
package edu.virginia.speclab.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

public class DataForm extends JPanel implements JuxtaUserInterfaceStyle
{
    private boolean enableFields;
    private GridBagLayout gridbag;        
    private HashMap fieldMap;
    private Font font;
    private boolean showLabel;
	
    public DataForm()
    {
        this.font = new Font("Verdana",Font.PLAIN,10);
        fieldMap = new HashMap();
        gridbag = new GridBagLayout();
        enableFields = true;
        showLabel = true;
        setLayout(gridbag);
        setBorder( new EmptyBorder(5,5,5,5));            
    }
    
    public void setEnableFields( boolean value )
    {
        this.enableFields = value;
    }
    
    public String getFieldContent( String fieldName )
    {
		Object field = fieldMap.get(fieldName);
		
		if( field == null ) return null;
		
		if( field instanceof JTextComponent )
		{
	        JTextComponent textField = (JTextComponent) field;
            return textField.getText();
		}
		else if( field instanceof JComboBox )
		{
			JComboBox comboBox = (JComboBox) field;
			return comboBox.getSelectedItem().toString();
		}
        else if( field instanceof JLabel )
        {
            JLabel label = (JLabel) field;
            return label.getText();
        }
        else if( field instanceof JCheckBox )
        {
            JCheckBox checkBox = (JCheckBox) field;
            if( checkBox.isSelected() ) return "true";
            else return "false";
        }
		
		return null;
    }

    public Date getDateFieldContent( String fieldName )
    {
        Object field = fieldMap.get(fieldName);
        if( field instanceof JSpinner )
        {
            JSpinner spinner = (JSpinner)field;
            return (Date)spinner.getValue();
        }
        return null;
    }

    public void setFont( Font font )
    {
        this.font = font;
    }
    
    public void setShowLabel( boolean showLabel )
    {
        this.showLabel = showLabel;
    }

    public void updateField( String fieldName, Date value )
    {
        try
        {
            JSpinner spinner = (JSpinner) fieldMap.get(fieldName);
            if (value != null)
                spinner.setValue(value);
        }
        catch (ClassCastException e)
        {
            // nothin
        }
    }
    
    public void updateField( String fieldName, String value )
    {
    	try
    	{
	        JTextComponent textField = (JTextComponent) fieldMap.get(fieldName);        
	        textField.setText(value);        
    	}
    	catch (ClassCastException e)
    	{
	        JLabel textField = (JLabel) fieldMap.get(fieldName);        
	        textField.setText(value);        
    	}
    }

    public void addMemoField( String fieldName, String initialValue, int numberOfLines )
    {
        JTextArea textField = new JTextArea(initialValue);
        GridBagConstraints c = new GridBagConstraints();
        
        textField.setFont( font );        
        textField.setLineWrap(true);
        textField.setWrapStyleWord(true);
        textField.setEditable(enableFields);
        textField.setEnabled(enableFields);
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(textField);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // calculate height of the memo field
        int linesHigh = textField.getFontMetrics(font).getHeight() * numberOfLines;
        scrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE,linesHigh));        

        c.fill = GridBagConstraints.BOTH;                    
        
        if( showLabel )
        {
            String displayText = fieldName + ":";
            JLabel textLabel = new JLabel(displayText);
            textLabel.setFont( font );
            textLabel.setBorder( new EmptyBorder(2,2,2,2));
            textLabel.setVerticalAlignment(JLabel.TOP);
            gridbag.setConstraints(textLabel, c);
            add(textLabel);
        }
        
        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.weightx = 1.0;
        c.weighty = 5.0;
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);
        
        fieldMap.put( fieldName, textField );
    }

    public void addSpacer( int height )
    {
        GridBagConstraints c = new GridBagConstraints();
        
        Component spacer = Box.createRigidArea( new Dimension(1,height) );
        
        c.fill = GridBagConstraints.BOTH;                    
        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.weightx = 1.0;
        gridbag.setConstraints(spacer, c);
        add(spacer);
    }

    public void addLabel( String fieldName, String initialValue )
    {
        JLabel textLabel = new JLabel(initialValue);
        GridBagConstraints c = new GridBagConstraints();
        
        textLabel.setFont( font );
        textLabel.setBorder( new EmptyBorder(2,2,2,2));
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.weightx = 1.0;
        gridbag.setConstraints(textLabel, c);
        add(textLabel);
        
        fieldMap.put( fieldName, textLabel );
    }

    public void addField( String fieldName, String initialValue, String toolTip )
    {
        JTextField textField = new JTextField(initialValue);
        addTextFieldImpl(fieldName, textField, toolTip);
    }

    public void addField( String fieldName, String initialValue )
    {       
        JTextField textField = new JTextField(initialValue);
        addTextFieldImpl(fieldName, textField, null);
    }

    public void addDateField( String fieldName, Date initialValue, String toolTip )
    {
        if (initialValue == null) initialValue = new Date(); // now
        JSpinner field = new JSpinner(new SpinnerDateModel(initialValue, null, null, Calendar.YEAR));
        JSpinner.DateEditor de = new NonChristianJSpinnerDateEditor(field, "MM/dd/yyyy G");
        field.setEditor(de);

        GridBagConstraints c = new GridBagConstraints();

        field.setFont( font );
        if (toolTip != null) field.setToolTipText(toolTip);
        field.setEnabled(enableFields);

        c.fill = GridBagConstraints.HORIZONTAL;

        if( showLabel)
        {
            String displayText = fieldName + ":";
            JLabel textLabel = new JLabel(displayText);
            textLabel.setFont( font );
            textLabel.setBorder( new EmptyBorder(2,2,2,2));
            if (toolTip != null) textLabel.setToolTipText(toolTip);
            gridbag.setConstraints(textLabel, c);
            add(textLabel);
        }

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(field, c);
        add(field);
        fieldMap.put( fieldName, field );
    }

    private void addTextFieldImpl( String fieldName, JTextField field, String toolTip )
    {
        GridBagConstraints c = new GridBagConstraints();

        field.setFont( font );
        field.setEditable(enableFields);
        field.setEnabled(enableFields);
        if (toolTip != null) field.setToolTipText(toolTip);

        c.fill = GridBagConstraints.HORIZONTAL;

        if( showLabel)
        {
            String displayText = fieldName + ":";
            JLabel textLabel = new JLabel(displayText);
            textLabel.setFont( font );
            textLabel.setBorder( new EmptyBorder(2,2,2,2));
            if (toolTip != null) textLabel.setToolTipText(toolTip);
            gridbag.setConstraints(textLabel, c);
            add(textLabel);
        }

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(field, c);
        add(field);

        fieldMap.put( fieldName, field );
    }

    public void addCheckBox( String fieldName, boolean initialValue )
    {
        JLabel textLabel = new JLabel(fieldName);
        JCheckBox checkBox = new JCheckBox();
        GridBagConstraints c = new GridBagConstraints();
        
        textLabel.setFont( font );
        if( !enableFields ) textLabel.setForeground(Color.GRAY);
        textLabel.setBorder( new EmptyBorder(2,2,2,2));
        
        checkBox.setSelected(initialValue);
        checkBox.setEnabled(enableFields);
        
        c.fill = GridBagConstraints.HORIZONTAL;                    
        gridbag.setConstraints(checkBox, c);
        add(checkBox);

        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.weightx = 1.0;
        c.weighty = 2.0;
        gridbag.setConstraints(textLabel, c);
        add(textLabel);
        
        fieldMap.put( fieldName, checkBox );
    }
  
    public void addComboBox( String fieldName, Object values[] )
    {
        JComboBox comboField = new JComboBox(values);
        GridBagConstraints c = new GridBagConstraints();

        comboField.setFont( font );
        comboField.setEditable(enableFields);
        
        c.fill = GridBagConstraints.HORIZONTAL;                    

        if( showLabel )
        {
            String displayText = fieldName + ":";
            JLabel textLabel = new JLabel(displayText);
            textLabel.setFont( font );
            textLabel.setBorder( new EmptyBorder(2,2,2,2));
            gridbag.setConstraints(textLabel, c);
            add(textLabel);
        }

        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.weightx = 1.0;
        c.weighty = 2.0;
        gridbag.setConstraints(comboField, c);
        add(comboField);
        
        fieldMap.put( fieldName, comboField );
    }


    // If JSpinner.DateEditor(JSpinner, DateFormat) were public, I
    // wouldn't need to do this horribleness. I don't even see why
    // it needs to be private.  But it is, and it does all of its work
    // inside the constructor (uggghhhh) so I had to subclass it
    // and copy its code here so I could use a custom DateFormat
    // that remaps BC/AD to BCE/CE for the era names. The
    // weird thing is that all of these APIs for modifying things like the
    // era format symbols are public and useful but for some reason
    // there's no good way to get them into the JSpinner.
    private class NonChristianJSpinnerDateEditor extends JSpinner.DateEditor {
        public NonChristianJSpinnerDateEditor(JSpinner spinner, String formatString)
        {
            super(spinner);
            SimpleDateFormat sdf = new SimpleDateFormat(formatString, spinner.getLocale());
            DateFormatSymbols dfs = sdf.getDateFormatSymbols();
            final String eras[] = { "BCE", "CE" };
            dfs.setEras(eras);
            sdf.setDateFormatSymbols(dfs);

            DateFormatter formatter = new DateFormatter(sdf);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(
                                                      formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
        }
    }
}
