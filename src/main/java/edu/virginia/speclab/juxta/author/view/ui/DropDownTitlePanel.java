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
 

package edu.virginia.speclab.juxta.author.view.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class DropDownTitlePanel extends JPanel implements JuxtaUserInterfaceStyle
{
    private JComboBox dropDownBox;
    
    public DropDownTitlePanel()
    {
        super();
        setSize(500, 37);

        initUI();
                        
        setBorder(null);
        dropDownBox.setFont(NORMAL_FONT);
        dropDownBox.setBackground(THIRD_COLOR_WHITE);
    }
	
	public void addActionListener( ActionListener listener )
	{
		dropDownBox.addActionListener(listener);
	}
	
	public Object getSelected()
	{
		return dropDownBox.getSelectedItem();
	}
    
    public void setSelection( Object item )
    {
        dropDownBox.setSelectedItem(item);
    }
    
    public void setList( List items )
    {
		ComboBoxModel model = new DefaultComboBoxModel(items.toArray());	
        dropDownBox.setModel(model);
    }

    private void initUI()
    {
		dropDownBox = new JComboBox();        
		dropDownBox.setBorder(new EmptyBorder(2, 3, 2, 3));
		dropDownBox.setOpaque(false);
        setLayout(new BorderLayout());
        add(dropDownBox,BorderLayout.CENTER);        
    }

}
