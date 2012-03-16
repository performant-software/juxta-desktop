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

package edu.virginia.speclab.juxta.author.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import edu.virginia.speclab.juxta.author.model.SearchResults.SearchResult;
import edu.virginia.speclab.juxta.author.view.collation.CollationViewTextArea;
import edu.virginia.speclab.juxta.author.view.compare.DocumentCompareView;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * Display the Search Results Panel  
 * @author Nick
 *
 */
public class SearchResultsPanel extends JPanel implements JuxtaUserInterfaceStyle {
    private JTable resultsTable;

    private JuxtaSession session;
    private JuxtaAuthorFrame juxtaFrame;

    private static final int DOCUMENT_NAME = 0;
    private static final int LINE_NUM = 1;
    private static final int FRAGMENT = 2;
    private static final int NUM_COLS = 3;

    private static final String BASE_TITLE = "Search Results";
    private PanelTitle titlePanel = null;
    private JCheckBox highlightButton;

    public SearchResultsPanel(JuxtaAuthorFrame frame) {
        this.juxtaFrame = frame;
        initUI();
    }

    public void setSession(JuxtaSession session) {
        this.session = session;
        setSearchResults(null);
    }

    public void setSearchResults(SearchResults searchResults) {
        if (searchResults == null) {
            resultsTable.setModel(new ResultsTable(new SearchResults("")));
            titlePanel.setTitleText(BASE_TITLE);
        } else {
            resultsTable.setModel(new ResultsTable(searchResults));
            titlePanel.setTitleText(BASE_TITLE + " - \"" + searchResults.getOriginalQuery() + "\" ("
                + searchResults.getSearchResults().size() + " found)");
            
            // re-check highlight box and show results
            this.highlightButton.setSelected(true);
            DocumentCompareView   compareView = juxtaFrame.getDocumentCompareView();
            CollationViewTextArea collationView = juxtaFrame.getCollationView();
            compareView.setHighlightAllSearchResults(true);
            collationView.setHighlightAllSearchResults(true);
        }
        
        DocumentCompareView view = juxtaFrame.getDocumentCompareView();
        view.searchHighlightRemoval();
    }

    private void initUI() {
        titlePanel = new PanelTitle();
        titlePanel.setFont(TITLE_FONT);
        titlePanel.setBackground(TITLE_BACKGROUND_COLOR);
        titlePanel.setTitleText(BASE_TITLE);

        //toolbar = new SearchToolBar();
        //resultsTable = new JTable(new ResultsTable(new SearchResults("")));		
        resultsTable = new JTable(new ResultsTable(new SearchResults(""))) {

            //Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                if (colIndex == FRAGMENT) {
                    String str = (String) getValueAt(rowIndex, colIndex);
                    return str;
                }
                return "";
            }
        };
        resultsTable.setFont(SMALL_FONT);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.addMouseListener(new ClickTracker());
        resultsTable.addKeyListener(new KeyTracker());

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(titlePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add( createSearchToolBar(), BorderLayout.SOUTH);
    }
    
    private void clearResults() {
        DocumentCompareView view = juxtaFrame.getDocumentCompareView();
        view.clearSearchResults();
        CollationViewTextArea collationView = juxtaFrame.getCollationView();
        collationView.clearSearchResults();
        setSearchResults(null);
    }

    public JToolBar createSearchToolBar() {
        JToolBar bar = new JToolBar();
        bar.setLayout( new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setFloatable(false);
        bar.setBorder( BorderFactory.createEmptyBorder(1,0,1,2));

        this.highlightButton = new JCheckBox("Highlight All Results", true);
        this.highlightButton.setFont(SMALL_FONT);

        this.highlightButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DocumentCompareView   compareView = juxtaFrame.getDocumentCompareView();
                CollationViewTextArea collationView = juxtaFrame.getCollationView();
                boolean showHighlights = ((JCheckBox)e.getSource()).isSelected();
                if ( showHighlights == false ) {
                    collationView.setHighlightAllSearchResults(false);
                    compareView.setHighlightAllSearchResults(false);
                } else {
                    compareView.setHighlightAllSearchResults(true);
                    collationView.setHighlightAllSearchResults(true);
                }
            }
        });

        bar.add(this.highlightButton);
        bar.add( Box.createHorizontalGlue() );
        JButton trash = new JButton(REMOVE_ANNOTATION);
        trash.setToolTipText("Clear search results");
        trash.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                clearResults();
            }
        });
        bar.add(trash);
        return bar;
    }

    private class KeyTracker extends KeyAdapter {
        public void keyReleased(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_ENTER) || (e.getKeyCode() == KeyEvent.VK_DOWN)
                || (e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_PAGE_UP)
                || (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)) {
                int selectedRow = resultsTable.getSelectedRow();
                ResultsTable model = (ResultsTable) resultsTable.getModel();
                if (model.empty())
                    return;
                SearchResults.SearchResult result = model.getSearchResult(selectedRow);
                SimpleLogger.logInfo("Clicked on: " + model.getDocumentName(result) + " "
                    + Integer.toString(result.getOffset()));

                selectSearchResult(result);
            }
        }
    }

    private class ClickTracker extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            int selectedRow = resultsTable.getSelectedRow();
            ResultsTable model = (ResultsTable) resultsTable.getModel();
            if (model.empty())
                return;
            SearchResults.SearchResult result = model.getSearchResult(selectedRow);
            SimpleLogger.logInfo("Clicked on: " + model.getDocumentName(result) + " "
                + Integer.toString(result.getOffset()));

            selectSearchResult(result);
        }

    }

    private void selectSearchResult(SearchResults.SearchResult result) {
        // If the comparison view is visible, and the target document is one of 
        // the documents visible, then don't change the view.
        // Otherwise, change to collation view, with the target document as the base.
        int targetViewMode = JuxtaAuthorFrame.VIEW_MODE_COMPARISON;
        DocumentCompareView view = juxtaFrame.getDocumentCompareView();
        
        if (juxtaFrame.getViewMode() == JuxtaAuthorFrame.VIEW_MODE_COLLATION) {
            targetViewMode = JuxtaAuthorFrame.VIEW_MODE_COLLATION;
        } else if ((view.getBaseDocument() == null) || (view.getWitnessDocument() == null)) {
            targetViewMode = JuxtaAuthorFrame.VIEW_MODE_COLLATION;
        } else if ((view.getBaseDocument().getID() != result.getDocumentID()) 
            && (view.getWitnessDocument().getID() != result.getDocumentID())) {
            targetViewMode = JuxtaAuthorFrame.VIEW_MODE_COLLATION;
        }

        if (targetViewMode == JuxtaAuthorFrame.VIEW_MODE_COLLATION) {
            juxtaFrame.setLocation(targetViewMode, result.getDocumentID(), result.getOffset(), result.getLength(),
                result.getDocumentID(), result.getOffset(), result.getLength());
        } else {
            int baseId = view.getBaseDocument().getID();
            int witnessId = view.getWitnessDocument().getID();
            
            // Check if the documents are already visible
            if (baseId == result.getDocumentID() ) {
                this.juxtaFrame.setLocation(targetViewMode, 
                    baseId, result.getOffset(), result.getLength(),
                    witnessId, -1, -1);
            } else if( witnessId == result.getDocumentID()) {
                this.juxtaFrame.setLocation(targetViewMode, 
                    baseId, -1, -1,
                    witnessId, result.getOffset(), result.getLength() );
            } else {
                this.juxtaFrame.setLocation(targetViewMode, view.getBaseDocument().getID(), 0, 0, result.getDocumentID(),
                    result.getOffset(), result.getLength());
            }
        }
    }

    private class ResultsTable extends AbstractTableModel {
        private SearchResults searchResults;

        public ResultsTable(SearchResults results) {
            searchResults = results;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public int getColumnCount() {
            return NUM_COLS;
        }

        public boolean empty() {
            return searchResults.getSearchResults().size() == 0;
        }

        public int getRowCount() {
            if (empty())
                return 1;
            return searchResults.getSearchResults().size();
        }

        public String getColumnName(int column) {
            switch (column) {
                case DOCUMENT_NAME:
                    return "Text";
                case LINE_NUM:
                    return "Location";
                case FRAGMENT:
                    return "Passage";
            }

            return null;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            List<SearchResult> results = this.searchResults.getSearchResults();

            if (empty() && (columnIndex == DOCUMENT_NAME))
                return "No results found";

            if (rowIndex < results.size()) {
                SearchResults.SearchResult result = (SearchResults.SearchResult) results.get(rowIndex);

                switch (columnIndex) {
                    case DOCUMENT_NAME:
                        return getDocumentName(result);
                    case LINE_NUM:
                        return getLineNum(result);
                    case FRAGMENT:
                        return "<html>" + getFragment(result) + "</html>";
                }
            }

            return null;
        }

        private Object getFragment(SearchResult result) {
            return result.getTextFragment();
        }

        public Object getDocumentName(SearchResult result) {
            JuxtaDocument document = session.getDocumentManager().lookupDocument(result.getDocumentID());
            return document.getDocumentName();
        }

        public Object getLineNum(SearchResult result) {
            JuxtaDocument document = session.getDocumentManager().lookupDocument(result.getDocumentID());
            LocationMarker loc = document.getLocationMarker(result.getOffset());
            if (loc == null)
                return "n/a";
            return loc.getLocationName(); // + " " + result.dump();
        }

        public SearchResults.SearchResult getSearchResult(int iRow) {
            return (SearchResult) searchResults.getSearchResults().get(iRow);
        }
    }

}
