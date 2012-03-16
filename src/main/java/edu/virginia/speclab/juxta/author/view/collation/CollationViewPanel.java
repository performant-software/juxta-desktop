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
package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.Scrollable;

import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;


public class CollationViewPanel extends JPanel {
    private CollationViewTextArea textArea;
    private JScrollPane scrollPane;
    private PanelTitle titlePanel;
    private JButton revisionToggle;
    private boolean revisionViewOn;
    private JButton editRevisionBtn;
    private List<JButton> editButtons;
    private boolean showPager = false;
    private List<JButton> pageButtons;
    private JToolBar toolbar;
    private RevisionManager revisionManager;
    private JuxtaAuthorFrame juxtaFrame;
    
    // icons
    private static final ImageIcon ACCEPT = new ImageIcon(ClassLoader.getSystemResource("icons/accept.gif"));
    private static final ImageIcon DECLINE = new ImageIcon(ClassLoader.getSystemResource("icons/decline.gif"));
    private static final ImageIcon VIEW_REVS = new ImageIcon(ClassLoader.getSystemResource("icons/view_diplomatic.gif"));
    private static final ImageIcon EDIT_REVS = new ImageIcon(ClassLoader.getSystemResource("icons/edit_diplomatic.gif"));
    private static final ImageIcon NEXT = new ImageIcon(ClassLoader.getSystemResource("icons/right.gif"));
    private static final ImageIcon PREV = new ImageIcon(ClassLoader.getSystemResource("icons/left.gif"));
    private static final ImageIcon RECOLLATE = new ImageIcon(ClassLoader.getSystemResource("icons/recollate.gif"));
    private static final ImageIcon DISCARD_CHANGES = JuxtaUserInterfaceStyle.REMOVE_ANNOTATION;

    public CollationViewPanel(final JuxtaAuthorFrame frame) {
        super();
        setLayout( new BorderLayout() );
        this.juxtaFrame = frame;
              
        this.textArea = new CollationViewTextArea(frame);
        this.textArea.addListener( frame );
        JPanel textPanel = new ReasonablyScrollingPanel();
        textPanel.setLayout(new BorderLayout());
        textPanel.add( this.textArea, BorderLayout.CENTER);
        textPanel.add( this.textArea.getLocationMarkStrip(), BorderLayout.WEST);
        textPanel.setBackground(Color.WHITE);

        this.scrollPane = new JScrollPane();
        this.scrollPane.setViewportView(textPanel);
        this.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.scrollPane.setBorder(null);
        this.scrollPane.validate();

        textPanel.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                scrollBar.setValue(scrollBar.getValue() + (e.getUnitsToScroll() * 5));
            }
        });
        
        // if the text area resizes, redraw
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                textArea.updateSize();
            }
        });

        // create the title panel and add it to the top
        this.titlePanel = new PanelTitle();
        this.titlePanel.setBorder( BorderFactory.createEmptyBorder(0,3,3,3));
        this.titlePanel.setFont(JuxtaUserInterfaceStyle.TITLE_FONT);
        this.titlePanel.setBackground(JuxtaUserInterfaceStyle.TITLE_BACKGROUND_COLOR);
        
        this.revisionToggle = new JButton(VIEW_REVS);
        this.revisionViewOn = false;
        this.revisionToggle.setToolTipText("Toggle revisions view");
        this.revisionToggle.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewRevisionsToggleClicked();
            }
        });
        
        JToolBar ctls = new JToolBar();
        ctls.setFloatable(false);
        ctls.setOpaque(false);
        ctls.setLayout( new BoxLayout(ctls, BoxLayout.X_AXIS));
        ctls.add( this.revisionToggle );
        this.titlePanel.add( ctls, BorderLayout.EAST);
        
        add(this.titlePanel, BorderLayout.NORTH);
        add(this.scrollPane, BorderLayout.CENTER);
        this.toolbar = createToolbar();
        add(this.toolbar, BorderLayout.SOUTH );
    }
    
    private JToolBar createToolbar() {
        
        // create the manager FIRST as the buttons will send 
        // events to it
        this.revisionManager = new RevisionManager(this.textArea);
        
        JToolBar bar = new JToolBar();
        bar.setLayout( new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setFloatable(false);
        bar.setBorder( BorderFactory.createEmptyBorder(4,4,4,4));
        
        this.editButtons = new ArrayList<JButton>();
        JButton yesAll = new JButton("Accept All");
        yesAll.setToolTipText("Accept all revisions");
        yesAll.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.acceptAllRevisions();
            }
        });
        yesAll.setVisible(false);
        this.editButtons.add( yesAll );
        
        JButton noAll = new JButton("Reject All");
        noAll.setToolTipText("Reject all revisions");
        noAll.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.rejectAllRevisions();
            }
        });
        noAll.setVisible(false);
        this.editButtons.add( noAll );
        
        JButton yes = new JButton("Accept", ACCEPT);
        yes.setToolTipText("Accept current revision");
        yes.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.acceptRevision();
            }
        });
        yes.setVisible(false);
        this.editButtons.add( yes );
        
        JButton no = new JButton("Reject", DECLINE);
        no.setToolTipText("Reject current revision");
        no.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.rejectRevision();
            }
        });
        no.setVisible(false);
        this.editButtons.add( no );
        
        JButton prev = new JButton(PREV);
        prev.setToolTipText("Previous Revision");
        prev.setName("prev");
        prev.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.previousRevision();
            }
        });
        prev.setVisible(false);
        this.editButtons.add( prev );

        JButton next = new JButton( NEXT);
        next.setName("next");
        next.setToolTipText("Next Revision");
        next.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revisionManager.nextRevision();
            }
        });
        next.setVisible(false);
        this.editButtons.add( next );

        JButton collate = new JButton(RECOLLATE);
        collate.setToolTipText("Save changes and recollate");
        collate.setVisible(false);
        collate.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                collateClicked();
            }
        });
        this.editButtons.add( collate );
        
        JButton cancel = new JButton(DISCARD_CHANGES);
        cancel.setToolTipText("Discard changes");
        cancel.setVisible(false);
        cancel.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelClicked();
            }
        });
        this.editButtons.add( cancel );
      
        this.editRevisionBtn = new JButton(EDIT_REVS);
        this.editRevisionBtn.setToolTipText("Edit Revisions");
        this.editRevisionBtn.addActionListener( new ActionListener() {   
            public void actionPerformed(ActionEvent e) {
                editClicked();
            }
        });
        
        createPagingButtons();
        for (JButton btn : this.pageButtons ) {
            bar.add(btn);
        }
        
        for (JButton btn : this.editButtons ) {
            bar.add(btn);
            if ( btn.equals(next)) {
                bar.add(Box.createHorizontalGlue());
            } else {
                if ( btn.equals(no)) {
                    bar.add(Box.createHorizontalGlue());
                } else if ( btn.equals(cancel) == false) {
                    bar.add(Box.createHorizontalStrut(5));
                }
            }
        }
        bar.add( this.editRevisionBtn );
        
        return bar;
    }
    
    private void createPagingButtons() {
        this.pageButtons = new ArrayList<JButton>();
        JButton prev = new JButton(PREV);
        prev.setToolTipText("Previous Page");
        prev.setName("prev");
        prev.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.previousPage();
            }
        });
        prev.setVisible(false);
        this.pageButtons.add( prev );

        JButton next = new JButton( NEXT);
        next.setName("next");
        next.setToolTipText("Next Page");
        next.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.nextPage();
            }
        });
        next.setVisible(false);
        this.pageButtons.add( next );
    }
    
    private void showPagingButtons( boolean show ) {
        for ( JButton b : this.pageButtons ) {
            b.setVisible(show);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.revisionToggle.setEnabled(enabled);
        this.editRevisionBtn.setEnabled(enabled);
    }
    
    private void setNavigationButtonsVisible( boolean vis ) {
        for (JButton button:this.editButtons) {
            if ( button.getName() == null ) {
                continue;
            }
            
            if ( button.getName().equals("next") ||  
                 button.getName().equals("prev") ) {
                button.setVisible(vis);
            }
        }
    }
   
    private void viewRevisionsToggleClicked() {
        this.revisionViewOn = !this.revisionViewOn;
        this.revisionToggle.setSelected(this.revisionViewOn);
        this.textArea.setHeatmapEnabled(!this.revisionViewOn);
        setNavigationButtonsVisible(this.revisionViewOn);
        this.revisionManager.enableRevisionHover(this.revisionViewOn);
        if ( this.revisionViewOn ) {
            this.revisionManager.showRevisions();
        }
    }
    
    private void editClicked() {
        showPagingButtons(false);
        this.editRevisionBtn.setVisible(false);
        this.revisionToggle.setEnabled(false);
        this.revisionManager.setEnabled(true);
        this.scrollPane.setBorder( BorderFactory.createLineBorder(Color.RED));
        for (JButton button:this.editButtons) {
            button.setVisible(true);
        }
        
        if ( this.revisionViewOn ) {
            this.revisionViewOn = false;
            this.revisionToggle.setSelected(false);
        } else {
            this.textArea.setHeatmapEnabled(false);
            this.revisionManager.showRevisions();
        }
                
        this.juxtaFrame.lockdownUI(this, true);
    }
    
    private void cancelClicked() {
        this.revisionManager.cancelChanges();
        this.revisionManager.setEnabled(false);
        this.editRevisionBtn.setVisible(true);
        this.revisionToggle.setEnabled(true);
        this.textArea.setHeatmapEnabled(true);
        this.scrollPane.setBorder( null);
        for (JButton button:this.editButtons) {
            button.setVisible(false);
        }
        showPagingButtons(this.showPager);
        this.juxtaFrame.lockdownUI(this, false);
    }
    
    private void collateClicked() {
        try {
            this.revisionManager.commitChanges( this.juxtaFrame.getSession() );
            showPagingButtons(this.showPager);
            this.revisionManager.setEnabled(false);
            this.editRevisionBtn.setVisible(true);
            this.revisionToggle.setEnabled(true);
            this.textArea.setHeatmapEnabled(true);
            this.scrollPane.setBorder( null);
            for (JButton button:this.editButtons) {
                button.setVisible(false);
            }
            this.juxtaFrame.lockdownUI(this, false);
            this.juxtaFrame.reloadSession(this.textArea.getJuxtaDocument().getID());
        } catch (Exception e ) {
            String msg = "Unable to apply selected revision edits for the following reason:\n\n"
                + "     "+ e.toString();
            JOptionPane.showMessageDialog(null, msg, "Edit Revisions Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void setSession(JuxtaSession session) {
        this.textArea.setSession(session);
    }
    
    public void setDocument( JuxtaDocument doc ) {
        this.showPager = false;
        this.textArea.setJuxtaDocument(doc);
        boolean showRevisionControls = false;
        if ( doc != null && doc.hasRevisions() ) {
            showRevisionControls = true;
        }
        this.showPager = ( doc != null && doc.getPageBreaks().size() > 0 );
        this.revisionToggle.setVisible( showRevisionControls );
        this.editRevisionBtn.setVisible( showRevisionControls );
        this.toolbar.setVisible( showRevisionControls || this.showPager );
        if ( doc != null ) {
            this.titlePanel.setTitleText( doc.getDocumentName());
            showPagingButtons( this.showPager );
        } else {
            this.titlePanel.setTitleText("");
        }
        
        
    }

    public CollationViewTextArea getTextArea() {
        return textArea;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    private class ReasonablyScrollingPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        public static final int SCROLL_BLOCK_INCREMENT = 100;
        public static final int SCROLL_UNIT_INCREMENT = 8;

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true; // always match the width of the viewport
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_BLOCK_INCREMENT;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT;
        }
    }
}
