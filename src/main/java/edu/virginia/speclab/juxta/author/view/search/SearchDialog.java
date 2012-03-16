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

package edu.virginia.speclab.juxta.author.view.search;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;

public class SearchDialog extends JDialog {
    private static final long serialVersionUID = -6384420960398466805L;
    private JComboBox comboSearchText;
    private Set<SearchListener>  listeners = new HashSet<SearchListener>();

    public SearchDialog(JuxtaAuthorFrame parent) {
        super(parent);

        setTitle("Search In Files");
        setSize(400, 100);
        setResizable(false);
        
        addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                close();
            }
        });
        
        // search panel
        JPanel findPnl = new JPanel();
        findPnl.setLayout( new BoxLayout(findPnl, BoxLayout.X_AXIS));
        findPnl.add( new JLabel("Search: ") );
        this.comboSearchText = new JComboBox();
        this.comboSearchText.setEditable(true);
        this.comboSearchText.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onSearch();
                }
            }
        });
        findPnl.add( this.comboSearchText  );
        
        // Buttons panel
        JPanel btns = new JPanel();
        btns.setLayout( new BoxLayout(btns, BoxLayout.X_AXIS));
        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        
        final JButton findButton = new JButton("Search");
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSearch();
            }
        });
        btns.add( Box.createHorizontalGlue());
        btns.add(findButton);
        btns.add(closeButton);
        
        JPanel content = new JPanel();
        content.setLayout( new BorderLayout());
        content.setBorder( BorderFactory.createEmptyBorder(5,5,5,5));
        content.add(findPnl, BorderLayout.NORTH);
        content.add(btns, BorderLayout.SOUTH);
        
        add(content, BorderLayout.CENTER);
    }
    
    public void addSearchListener( SearchListener l ) {
        this.listeners.add(l);
    }

    private void onSearch() {
        String sel = (String) comboSearchText.getEditor().getItem();
        if (sel == null) {
            sel = (String) comboSearchText.getSelectedItem();
        }
        if ((sel == null) || (sel.equals(""))) {
            return;
        }

        addSearchItem(sel);
        SearchOptions opts = new SearchOptions(sel, true, false);
        for (SearchListener l : this.listeners ) {
            l.handleSearch(opts);
        }
    }

    public void close() {
        setVisible(false);
    }

    public void display() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void addSearchItem(String item) {
        comboSearchText.setSelectedItem(item);
        if (comboSearchText.getSelectedIndex() == -1)
            comboSearchText.addItem(item);
        if (comboSearchText.getItemCount() > 20)
            comboSearchText.removeItemAt(0);
    }
}
