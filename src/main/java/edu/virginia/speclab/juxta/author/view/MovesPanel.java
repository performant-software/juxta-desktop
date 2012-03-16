/*
 * Created on Jul 20, 2007
 *
 */
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

/**
 * @author Cortlandt
 */

package edu.virginia.speclab.juxta.author.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.MovesManager;
import edu.virginia.speclab.juxta.author.model.MovesManager.FragmentPair;
import edu.virginia.speclab.juxta.author.model.MovesManagerListener;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;
import edu.virginia.speclab.util.SimpleLogger;

public class MovesPanel extends JPanel implements JuxtaUserInterfaceStyle {
    private JTable blockTable;
    private JScrollPane scrollPane;
    private DocumentManager documentManager;
    private JuxtaAuthorFrame juxtaAuthorFrame;

    private BlockTable blockTableModel;
    private BlockToolBar toolbar;

    public MovesPanel(JuxtaAuthorFrame juxtaAuthorFrame) {
        this.juxtaAuthorFrame = juxtaAuthorFrame;
        initUI();
    }

    private void initUI() {
        PanelTitle titlePanel = new PanelTitle();
        titlePanel.setFont(TITLE_FONT);
        titlePanel.setBackground(TITLE_BACKGROUND_COLOR);
        titlePanel.setTitleText("Moves");

        blockTableModel = new BlockTable();
        blockTable = new JTable(blockTableModel);
        blockTable.setFont(SMALL_FONT);
        blockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //blockTable.setFocusable(false);
        blockTable.addMouseListener(new ClickTracker());
        blockTable.addKeyListener(new KeyTracker());

        toolbar = new BlockToolBar();

        scrollPane = new JScrollPane(blockTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(titlePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(toolbar, BorderLayout.SOUTH);
    }

    public void setSession(JuxtaSession session) {
        if (session != null) {
            documentManager = session.getDocumentManager();
            blockTableModel.movesChanged(documentManager.getMovesManager());
            documentManager.getMovesManager().addListener(blockTableModel);
        }
    }

    private class KeyTracker extends KeyAdapter {
        public void keyReleased(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_ENTER) || (e.getKeyCode() == KeyEvent.VK_DOWN)
                || (e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_PAGE_UP)
                || (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)) {
                int selectedRow = blockTable.getSelectedRow();
                MovesManager.FragmentPair move = blockTableModel.getMove(selectedRow);
                selectMove(move);
            }
        }
    }

    private class ClickTracker extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            int selectedRow = blockTable.getSelectedRow();
            MovesManager.FragmentPair move = blockTableModel.getMove(selectedRow);

            // if it is a double click, open the block edit window
            if (e.getClickCount() >= 2) {
                editMove(move);
            } else {
                selectMove(move);
            }
        }

        private void editMove(MovesManager.FragmentPair move) {
            selectMove(move);
        }

    }

    private void selectMove(MovesManager.FragmentPair move) {
        // collect offsets
        int start1 =  move.first.getStartOffset(OffsetRange.Space.ACTIVE);
        int end1 =  move.first.getEndOffset(OffsetRange.Space.ACTIVE);
        int start2 =  move.second.getStartOffset(OffsetRange.Space.ACTIVE);
        int end2 =  move.second.getEndOffset(OffsetRange.Space.ACTIVE);
        
        // NOTE: change from prior versions. Don't try to keep views as they
        // were. Instead, make the views match the order of the selected move.
        // this keeps the UI consistent with the description of the move
        // and avoids a problem where the system gets crossed up about which doc
        // is base vs witness which leads to errors when dealing with moves
        // near the end of differently sized documents
        juxtaAuthorFrame.setLocation(
          JuxtaAuthorFrame.VIEW_MODE_COMPARISON,
          move.first.getDocumentID(),  start1,  (end1-start1),
          move.second.getDocumentID(), start2, (end2-start2) );
    }

    private class BlockTable extends AbstractTableModel implements MovesManagerListener {
        private static final int LEFT_DOC = 0;
        private static final int LEFT_LOCATION = 1;
        private static final int RIGHT_DOC = 2;
        private static final int RIGHT_LOCATION = 3;
        private static final int NUM_COLUMNS = 4;

        private MovesManager.MoveList moveList;

        public BlockTable() {
            moveList = new MovesManager(null).new MoveList();
        }

        public void movesChanged(MovesManager movesManager) {
            moveList = movesManager.getAllMoves();
            fireTableDataChanged();
        }

        public MovesManager.FragmentPair getMove(int index) {
            if (index >= 0 && index < moveList.size()) {
                return moveList.get(index);
            } else {
                return null;
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public int getColumnCount() {
            return NUM_COLUMNS;
        }

        public int getRowCount() {
            return moveList.size();
        }

        public String getColumnName(int column) {
            switch (column) {
                case LEFT_DOC:
                    return "Left Document";
                case LEFT_LOCATION:
                    return "Left Location";
                case RIGHT_DOC:
                    return "Right Document";
                case RIGHT_LOCATION:
                    return "Right Location";
            }

            return null;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < moveList.size()) {
                MovesManager.FragmentPair move = moveList.get(rowIndex);

                switch (columnIndex) {
                    case LEFT_DOC:
                        return move.first.getDocument();
                    case LEFT_LOCATION:
                        return getLocation(move.first);
                    case RIGHT_DOC:
                        return move.second.getDocument();
                    case RIGHT_LOCATION:
                        return getLocation(move.second);
                }
            }

            return null;
        }

        private Object getLocation(MovesManager.Fragment fragment) {
            boolean debug = false;
            if (debug)
                return documentManager.getMovesManager().getLocationFromFragment(fragment) + " ("
                    + fragment.getStartOffset(OffsetRange.Space.ACTIVE) + ","
                    + fragment.getEndOffset(OffsetRange.Space.ACTIVE) + ")";
            else
                return documentManager.getMovesManager().getLocationFromFragment(fragment);
        }
    }

    private class BlockToolBar extends JPanel {
        public BlockToolBar() {
            JToolBar rightToolBar = new JToolBar();
            rightToolBar.setFloatable(false);

            JButton deleteButton = new JButton(REMOVE_MOVE);
            deleteButton.setToolTipText("Remove selected move");

            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        int selectedRow = blockTable.getSelectedRow();
                        if (selectedRow >= 0) {
                            MovesManager.FragmentPair move = blockTableModel.getMove(selectedRow);
                            documentManager.getMovesManager().deleteMove(move);
                        }
                    } catch (LoggedException ex) {
                        SimpleLogger.logError(ex.getMessage());
                    }
                }
            });

            rightToolBar.add(deleteButton);

            JPanel southPanel = new JPanel();
            southPanel.setLayout(new BorderLayout());

            CompoundBorder compoundBorder = new CompoundBorder(LineBorder.createGrayLineBorder(), new EmptyBorder(2, 0,
                2, 0));

            setLayout(new BorderLayout());
            setBorder(compoundBorder);
            add(rightToolBar, BorderLayout.EAST);
        }
    }

    public void select(FragmentPair fpSelect) {
        for (int i = 0; i < blockTableModel.getRowCount(); ++i) {
            FragmentPair fp = blockTableModel.getMove(i);
            if (fp.compareTo(fpSelect) == 0) {
                blockTable.addRowSelectionInterval(i, i);
                return;
            }
        }
        SimpleLogger.logError("Attempted to select a move that doesn't exist in the move panel.");
    }
}
