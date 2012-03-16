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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.Box;
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

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.AnnotationListener;
import edu.virginia.speclab.juxta.author.model.AnnotationManager;
import edu.virginia.speclab.juxta.author.model.CriticalApparatus;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.Lemma;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.PanelTitle;

/**
 * Display the Annotation Panel within a given JuxtaFrame. 
 * @author Nick
 *
 */
public class AnnotationPanel extends JPanel implements JuxtaUserInterfaceStyle, AnnotationListener
{
	private AnnotationManager annotationManager;
    private DocumentManager documentManager;
	
	private AnnotationTable annotationTableModel;
	private JTable annotationTable;
    
    private boolean synchWindows;
	
	private static final int BASE_TEXT = 0;
	private static final int WITNESS_TEXT = 1;
	private static final int QUOTE = 2;
	private static final int NOTES = 3;
    private JuxtaAuthorFrame juxtaFrame;

	public AnnotationPanel( JuxtaAuthorFrame frame )
	{
		synchWindows = true;
        this.juxtaFrame = frame;
		initUI();				
	}
	
	public void setSession( JuxtaSession session )
	{
		if( this.annotationManager != null )
		{
			annotationManager.removeListener(this);
            annotationTableModel.clear();
		}
		
        if( session != null )
        {
            this.annotationManager = session.getAnnotationManager();
            
            if( annotationManager != null )
            {
                annotationManager.addListener(this);
                
                for( Iterator i = annotationManager.getAnnotations().iterator(); i.hasNext(); )
                {
                    Annotation annotation = (Annotation) i.next();
                    annotationTableModel.addAnnotation(annotation);
                }                
            }       
            
            documentManager = session.getDocumentManager();
        }
        else
        {
            annotationManager = null;
            documentManager = null;
        }
	}
    
    private boolean toggleSynchWindows()
    {
        synchWindows = !synchWindows;
        
        if( synchWindows )
        {
            annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            return true;
        }
        else
        {
            annotationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            return false;
        }
    }
	
	private void initUI()
	{
	    PanelTitle titlePanel = new PanelTitle();
		titlePanel.setFont(TITLE_FONT);
		titlePanel.setBackground(TITLE_BACKGROUND_COLOR);
		titlePanel.setTitleText("Notes");

		annotationTableModel = new AnnotationTable();
		
		annotationTable = new JTable(annotationTableModel) {
            
            //Implement table cell tool tips.
            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                if ((colIndex == QUOTE) || (colIndex == NOTES))
                {
	                String str = (String)getValueAt(rowIndex, colIndex);
	                return str;
                }
                return "";
            }
        };
		annotationTable.setFont(SMALL_FONT);
		annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annotationTable.addMouseListener(new ClickTracker());
        annotationTable.addKeyListener(new KeyTracker());
        
		AnnotationToolBar toolbar = new AnnotationToolBar();
				
		JScrollPane scrollPane = new JScrollPane(annotationTable);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		setLayout( new BorderLayout() );
		add( titlePanel, BorderLayout.NORTH );
		add( scrollPane, BorderLayout.CENTER );
		add( toolbar, BorderLayout.SOUTH );
	}
    
	// PER: The following is a hack to make the selection scroll to the correct place. Two things need to happen when
	// something is selected: the document needs to change to the selected one, and the place selected needs 
	// to be scrolled to. Unfortunately, when a new document is selected, we can't get information about scrolling
	// until it is painted once, so we start a new thread to give the app time to paint, then select again. The real solution
	// is probably in MarkableTextPanel::centerOffset(). modelToView() returns null without this hack.
	private class Reselect extends Thread 
	{
		public Difference difference = null;
        @Override
		public void run()                       
		{
			try {
				sleep(100);
			} catch (InterruptedException e) {}
            juxtaFrame.setLocation(difference);
		}
	}
	
	private class KeyTracker extends KeyAdapter
	{
        @Override
		public void keyReleased(KeyEvent e)
		{
            if(( e.getKeyCode() == KeyEvent.VK_ENTER ) ||
                	( e.getKeyCode() == KeyEvent.VK_DOWN ) ||
                	( e.getKeyCode() == KeyEvent.VK_UP ) ||
                	( e.getKeyCode() == KeyEvent.VK_PAGE_UP ) ||
                	( e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ))
            {
                int selectedRow = annotationTable.getSelectedRow();
                Annotation annotation = annotationTableModel.getAnnotation(selectedRow);                
                juxtaFrame.setLocation(annotation.getDifference());
                Reselect reselect = new Reselect();
                reselect.difference = annotation.getDifference();
                reselect.start();
            }
		}
	}
	private class ClickTracker extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            int selectedRow = annotationTable.getSelectedRow();
            Annotation annotation = annotationTableModel.getAnnotation(selectedRow);                

            // if it is a double click, open the annotation window
            if( e.getClickCount() >= 2 )
                selectAnnotation(annotation);
            // select the associated difference in the document panel
            else if( synchWindows  && !annotation.isFromOldVersion())
            {
                juxtaFrame.setLocation(annotation.getDifference());
                Reselect reselect = new Reselect();
                reselect.difference = annotation.getDifference();
                reselect.start();
                }
        }
        
        private void selectAnnotation( Annotation annotation )
        {
            JuxtaDocument baseDocument = annotation.getBaseDocument(documentManager);
            JuxtaDocument witnessDocument = annotation.getWitnessDocument(documentManager);            
            AnnotationDialog dialog = new AnnotationDialog(annotation,baseDocument,witnessDocument,juxtaFrame);
            dialog.setVisible(true);
            
            if( dialog.isOk() )
            {
                annotation.setIncludeImage(dialog.includeImage());
                annotationManager.markAnnotation(annotation,dialog.getNotes());
            }
        }
    }
	
	private class AnnotationTable extends AbstractTableModel
	{
		private LinkedList annotations;
		
		public AnnotationTable()
		{
			annotations = new LinkedList();
		}
        
        public Annotation getAnnotation( int index )
        {
            if( index >= 0 && index < annotations.size() )
                return (Annotation) annotations.get(index);
            else return null;            
        }
		
		public void clear()
        {
            if( annotations.size() > 0 )
            {
                fireTableRowsDeleted(0,annotations.size()-1);
                annotations.clear();
            }
        }

        @Override
        public void setValueAt(Object aValue,int rowIndex,int columnIndex)
		{
			if( columnIndex == NOTES )
			{
				if( rowIndex < annotations.size() && aValue instanceof String)
				{
					Annotation annotation = (Annotation) annotations.get(rowIndex);
                    annotationManager.markAnnotation(annotation,(String)aValue);					
				}
			}
		}
		
        @Override
		public boolean isCellEditable(int rowIndex,int columnIndex)
		{
			return false;			
		}
		
		public void addAnnotation(Annotation annotation) 
		{
			annotations.add(annotation);
			int row = annotations.size()-1;
			fireTableRowsInserted(row,row); 
		}		

		public void removeAnnotation(Annotation annotation) 
		{			
			int row = annotations.indexOf(annotation);
			annotations.remove(annotation);
			fireTableRowsDeleted(row,row);
		}

		public int getColumnCount() 
		{
			return 4;
		}

		public int getRowCount() 
		{
			return annotations.size();
		}

        @Override
		public String getColumnName(int column)
		{
			switch( column )
			{
				case BASE_TEXT:
					return "Base Text";
				case WITNESS_TEXT:
					return "Witness Text";
				case QUOTE:
					return "Lemma";
				case NOTES:
					return "Notes";					
			}

			return null;
		}

		public Object getValueAt(int rowIndex, int columnIndex) 
		{
			if( rowIndex < annotations.size() )
			{
				Annotation annotation = (Annotation) annotations.get(rowIndex);

				switch( columnIndex )
				{
					case BASE_TEXT:
						return getBaseText(annotation);
					case WITNESS_TEXT:
						return getWitnessText(annotation);
					case QUOTE:
						return getQuote(annotation);
					case NOTES:
						return getNotes(annotation);
				}
			}
			
			return null;
		}

		private Object getNotes(Annotation annotation) 
		{
			return annotation.getNotes();
		}

		private Object getQuote(Annotation annotation) 
		{
            Difference difference = annotation.getDifference();
            JuxtaDocument baseDocument = annotation.getBaseDocument(documentManager);
            JuxtaDocument witnessDocument = annotation.getWitnessDocument(documentManager);
            if ((difference == null) || (baseDocument == null) || (witnessDocument == null))
            	return "";

            // Check to see if this is outdated--if these offsets are zero, that means the annotation is bad, i.e., from an older version
            if (annotation.isFromOldVersion())
                return "Orphaned Annotation: Use 'File->Export Orphaned Annotations' to Recover.";

            Lemma lemma = CriticalApparatus.generateLemma(difference,baseDocument,witnessDocument);
            return lemma.getLemmaText();
        }
        
		private Object getWitnessText(Annotation annotation) 
		{
			JuxtaDocument document = annotation.getWitnessDocument(documentManager);
			
			if( document != null )
			{
				return document.getDocumentName();
			}
			else return null;
		}

		private Object getBaseText(Annotation annotation) 
		{
			JuxtaDocument document = annotation.getBaseDocument(documentManager);
			
			if( document != null )
			{
				return document.getDocumentName();
			}
			else return null;
		}

		public void removeSelected() 
		{
            LinkedList deadList = new LinkedList();
			int rows[] = annotationTable.getSelectedRows();
			
			for( int i = 0; i < rows.length; i++ )
			{
				if( rows[i] < annotations.size() )
				{
					Annotation annotation = (Annotation) annotations.get(rows[i]);
                    deadList.add(annotation);
				}
			}

            // now remove them so the indices aren't disturbed
            for( Iterator i = deadList.iterator(); i.hasNext(); )
            {
                Annotation annotation = (Annotation) i.next();
                annotationManager.removeAnnotation(annotation);                
            }
            
		}

	}
	
	private class AnnotationToolBar extends JPanel
	{
        private JButton synchButton;
        
		public AnnotationToolBar()
		{
	        JToolBar rightToolBar = new JToolBar();
	        rightToolBar.setFloatable(false);

	        JButton deleteButton = new JButton(REMOVE_ANNOTATION);
            deleteButton.setToolTipText("Remove selected annotation");
            
            synchButton = new JButton(LOCK_WINDOWS);
            synchButton.setToolTipText("Unlock selection from text");
				
			deleteButton.addActionListener( new ActionListener() {
	            public void actionPerformed(ActionEvent e)
	            {
					annotationTableModel.removeSelected();
	            } 
	        });

            synchButton.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {                     
                    if( toggleSynchWindows() )  
                    {
                        synchButton.setIcon(LOCK_WINDOWS);
                        synchButton.setToolTipText("Unlock selection from text");
                    }
                    else
                    {
                        synchButton.setIcon(UNLOCK_WINDOWS);
                        synchButton.setToolTipText("Lock selection to text");
                    }
                } 
            });

            rightToolBar.add(synchButton);
            rightToolBar.add(Box.createRigidArea(new Dimension(10,1)));
	        rightToolBar.add(deleteButton);
	        
	        JPanel southPanel = new JPanel();        
	        southPanel.setLayout( new BorderLayout());
	        
	        CompoundBorder compoundBorder = new CompoundBorder( LineBorder.createGrayLineBorder(),
	                                                            new EmptyBorder(2,0,2,0) );
	        
			setLayout( new BorderLayout() );
	        setBorder(compoundBorder);
	        add(rightToolBar,BorderLayout.EAST);        	                    
		}		
	}
   
	public void annotationAdded(Annotation annotation) 
	{
		annotationTableModel.addAnnotation(annotation);		
	}

	public void annotationRemoved(Annotation annotation) 
	{
		annotationTableModel.removeAnnotation(annotation);		
	}

	public void annotationMarked(Annotation annotation) 
    {
		repaint();		
	}

}
