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

package edu.virginia.speclab.juxta.author.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.util.SimpleLogger;

public class MovesManager 
{
	// some syntactic sugar to work around Java's problem with new'ing nested classes.
	public static Fragment newFragment() { return new MovesManager(null).new Fragment(); }
	public static Fragment newFragment(DocumentModel doc, int startIndex, int endIndex) { return new MovesManager(null).new Fragment(doc, startIndex, endIndex); }
	public static FragmentPair newFragmentPair() { return new MovesManager(null).new FragmentPair(); }
	public static MoveList newFragmentMoveList() { return new MovesManager(null).new MoveList(); }

	// This is one side of a move. It defines a fragment of one document
	public class Fragment extends OffsetRange
	{
        public Fragment()
        {
            super();
        }

        public Fragment(DocumentModel doc, int startIndex, int endIndex)
        {
            super(doc, startIndex, endIndex, OffsetRange.Space.ACTIVE);
        }

        public void set(DocumentModel doc, int startIndex, int endIndex)
        {
            resetDocument(doc);
            this.set(startIndex, endIndex, OffsetRange.Space.ACTIVE);
        }
    }
	
	// This defines an entire move, because it contains two fragments that are moved to each other.
	public class FragmentPair implements Comparable
	{
		public Fragment first = new Fragment();
		public Fragment second = new Fragment();
		public int compareTo(Object o)
		{
			FragmentPair rhs = (FragmentPair)o;
			
			if (first.getDocumentID() < rhs.first.getDocumentID()) return -1;
			else if (first.getDocumentID() > rhs.first.getDocumentID()) return 1;
			if (second.getDocumentID() < rhs.second.getDocumentID()) return -1;
			else if (second.getDocumentID() > rhs.second.getDocumentID()) return 1;
			
			if (first.getStartOffset(OffsetRange.Space.ACTIVE) < rhs.first.getStartOffset(OffsetRange.Space.ACTIVE)) return -1;
			else if (first.getStartOffset(OffsetRange.Space.ACTIVE) > rhs.first.getStartOffset(OffsetRange.Space.ACTIVE)) return 1;
			if (second.getStartOffset(OffsetRange.Space.ACTIVE) < rhs.second.getStartOffset(OffsetRange.Space.ACTIVE)) return -1;
			else if (second.getStartOffset(OffsetRange.Space.ACTIVE) > rhs.second.getStartOffset(OffsetRange.Space.ACTIVE)) return 1;
			
			if (first.getEndOffset(OffsetRange.Space.ACTIVE) < rhs.first.getEndOffset(OffsetRange.Space.ACTIVE)) return -1;
			else if (first.getEndOffset(OffsetRange.Space.ACTIVE) > rhs.first.getEndOffset(OffsetRange.Space.ACTIVE)) return 1;
			if (second.getEndOffset(OffsetRange.Space.ACTIVE) < rhs.second.getEndOffset(OffsetRange.Space.ACTIVE)) return -1;
			else if (second.getEndOffset(OffsetRange.Space.ACTIVE) > rhs.second.getEndOffset(OffsetRange.Space.ACTIVE)) return 1;
			
			return 0;
		}
	}
	
	// This defines a set of moves. It is used to store all the moves in the comparison set, and it is
	// also used to return the subset of moves for two particular documents.
	public class MoveList
	{
		private List list = new LinkedList();
		
		private void add(Fragment left, Fragment right)
		{
			FragmentPair fp = new FragmentPair();
			// Always add the documents with the lower document number first.
			// This makes sorting and retrieval easier
			if (left.getDocumentID() > right.getDocumentID())
			{
				fp.first = right;
				fp.second = left;
			}
			else if (left.getDocumentID() < right.getDocumentID())
			{
				fp.first = left;
				fp.second = right;
			}
			else
			{
				SimpleLogger.logError("MoveList.add was called with the document IDs the same on both sides.");
				return;
			}
			list.add(fp);
		}
		private void add(FragmentPair fp)
		{
			list.add(fp);
		}
		public int size()
		{
			return list.size();
		}
		public FragmentPair get(int index)
		{
			return (FragmentPair)list.get(index);
		}
		private void remove(FragmentPair match) throws LoggedException
		{
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				// This is a match if all the elements are equal. In addition, there is a match if the elements are reversed.
				if ((fp.first.getDocumentID() == match.first.getDocumentID()) && (fp.second.getDocumentID() == match.second.getDocumentID()) &&
						(fp.first.getStartOffset(OffsetRange.Space.ACTIVE) == match.first.getStartOffset(OffsetRange.Space.ACTIVE)) && (fp.second.getStartOffset(OffsetRange.Space.ACTIVE) == match.second.getStartOffset(OffsetRange.Space.ACTIVE)) &&
						(fp.first.getEndOffset(OffsetRange.Space.ACTIVE) == match.first.getEndOffset(OffsetRange.Space.ACTIVE)) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) == match.second.getEndOffset(OffsetRange.Space.ACTIVE)))
				{
					it.remove();
					return;
				}
				else if ((fp.first.getDocumentID() == match.second.getDocumentID()) && (fp.second.getDocumentID() == match.first.getDocumentID()) &&
						(fp.first.getStartOffset(OffsetRange.Space.ACTIVE) == match.second.getStartOffset(OffsetRange.Space.ACTIVE)) && (fp.second.getStartOffset(OffsetRange.Space.ACTIVE) == match.first.getStartOffset(OffsetRange.Space.ACTIVE)) &&
						(fp.first.getEndOffset(OffsetRange.Space.ACTIVE) == match.second.getEndOffset(OffsetRange.Space.ACTIVE)) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) == match.first.getEndOffset(OffsetRange.Space.ACTIVE)))
				{
					it.remove();
					return;
				}
			}
			// We should never get this far because we should never be passed an item that doesn't match
			throw new LoggedException("MovesManager.delete couldn't find the item to delete");
		}
		private void removeId(int docId)
		{
			// Remove all the entries that contain the docId in either fragment.
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				if ((fp.first.getDocumentID() == docId) || (fp.second.getDocumentID() == docId))
					it.remove();
			}
		}
		private MoveList filter(int docId1, int docId2)
		{
			// This returns a MoveList that only contains the items that match the two documents.
			// The full MoveList is always sorted so that the first doc id is lower than the second. However,
			// the returned structure is always returned in the order that is requested.
			// A copy of all the elements is returned, so the caller can't hurt the list.
			int docFirst = docId1;
			int docSecond = docId2;
			if (docId1 > docId2)
			{
				docFirst = docId2;
				docSecond = docId1;
			}
			MoveList ml = new MoveList();
			for (int i = 0; i < size(); ++i)
			{
				FragmentPair fp = get(i);
				if ((fp.first.getDocumentID() == docFirst) && (fp.second.getDocumentID() == docSecond))
				{
					// We found a match. Now create a similar FragmentPair to return.
					// We need to do a deep copy of this.
					FragmentPair fp2 = new FragmentPair();
					if (docId1 == fp.first.getDocumentID())
					{

						fp2.first.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
						fp2.second.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
					}
					else
					{
						fp2.first.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
						fp2.second.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
					}
					ml.add(fp2);
				}
			}
			Collections.sort(ml.list);
			return ml;
		}
		
		private MoveList deepCopy()
		{
			MoveList ml = new MoveList();
			for (int i = 0; i < size(); ++i)
			{
				FragmentPair fp = get(i);
				FragmentPair fp2 = new FragmentPair();
				fp2.first.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
				fp2.second.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
				ml.add(fp2);
			}
			Collections.sort(ml.list);
			return ml;
		}
		
		// This returns true if the position passed is in an existing block
		private boolean isPositionInBlock(int docId, int pos)
		{
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				if ((fp.first.getDocumentID() == docId) &&
						((fp.first.getStartOffset(OffsetRange.Space.ACTIVE) <= pos) && (fp.first.getEndOffset(OffsetRange.Space.ACTIVE) >= pos)))
						return true;
				if ((fp.second.getDocumentID() == docId) &&
						((fp.second.getStartOffset(OffsetRange.Space.ACTIVE) <= pos) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) >= pos)))
						return true;
			}
			return false;
		}
		
		public FragmentPair findBlock(int docId1, int docId2, int pos)
		{
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				if ((fp.first.getDocumentID() == docId1) &&
						((fp.first.getStartOffset(OffsetRange.Space.ACTIVE) <= pos) && (fp.first.getEndOffset(OffsetRange.Space.ACTIVE) >= pos)) &&
						(fp.second.getDocumentID() == docId2))
						return fp;
				if ((fp.second.getDocumentID() == docId1) &&
						((fp.second.getStartOffset(OffsetRange.Space.ACTIVE) <= pos) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) >= pos)) &&
						(fp.first.getDocumentID() == docId2))
						return fp;
			}
			return null;
		}
		
		public int countMoves(int id, int offset)
		{
			int count = 0;
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				if ((fp.first.getDocumentID() == id) &&
						((fp.first.getStartOffset(OffsetRange.Space.ACTIVE) <= offset) && (fp.first.getEndOffset(OffsetRange.Space.ACTIVE) >= offset)))
					++count;
				else if ((fp.second.getDocumentID() == id) &&
						((fp.second.getStartOffset(OffsetRange.Space.ACTIVE) <= offset) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) >= offset)))
					++count;
			}
			return count;
		}

		// This finds all the moves that match the id and offset.
		public MoveList findMoves(int id, int offset)
		{
			MoveList ml = new MoveList();
			for (int i = 0; i < size(); ++i)
			{
				FragmentPair fp = get(i);
				// We need to do a deep copy of this.
				if ((fp.first.getDocumentID() == id) &&
						((fp.first.getStartOffset(OffsetRange.Space.ACTIVE) <= offset) && (fp.first.getEndOffset(OffsetRange.Space.ACTIVE) >= offset)))
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
					fp2.second.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
					ml.add(fp2);
				}
				else if ((fp.second.getDocumentID() == id) &&
						((fp.second.getStartOffset(OffsetRange.Space.ACTIVE) <= offset) && (fp.second.getEndOffset(OffsetRange.Space.ACTIVE) >= offset)))
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
					fp2.second.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
					ml.add(fp2);
				}
			}
			return ml;
		}

		// This finds all the moves in a particular document.
		public MoveList findMoves(int id)
		{
			MoveList ml = new MoveList();
			for (int i = 0; i < size(); ++i)
			{
				FragmentPair fp = get(i);
				// We need to do a deep copy of this.
				if (fp.first.getDocumentID() == id)
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
					fp2.second.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
					ml.add(fp2);
				}
				else if (fp.second.getDocumentID() == id)
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.second.getDocument(), fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE));
					fp2.second.set(fp.first.getDocument(), fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE));
					ml.add(fp2);
				}
			}
			return ml;
		}

		private String serialize( OffsetRange.Space offsetSpace )
		{
			String xmlContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<moves>\n";
			
			for (int i = 0; i < size(); ++i)
			{
				FragmentPair fp = get(i);
				// We want to write this file in a deterministic way to make unit tests easier, and remove the possibilities of subtle
				// errors. To do that, we will save the document name instead of the document id, and we will sort the entries.
				String doc1 = documentManager.lookupDocument(fp.first.getDocumentID()).getDocumentName();
				String doc2 = documentManager.lookupDocument(fp.second.getDocumentID()).getDocumentName();
				if (doc1.compareTo(doc2) < 0)
				{
				xmlContents += "\t<move doc1=\"" + doc1 +
                    "\" space1=\"" + OffsetRange.spaceToString( offsetSpace ) +
					"\" start1=\"" + fp.first.getStartOffset( offsetSpace ) +
					"\" end1=\"" + fp.first.getEndOffset( offsetSpace ) +
					"\" doc2=\"" + doc2 +
                    "\" space2=\"" + OffsetRange.spaceToString( offsetSpace ) +
					"\" start2=\"" + fp.second.getStartOffset( offsetSpace ) +
					"\" end2=\"" + fp.second.getEndOffset( offsetSpace ) + "\" />\n";
				}
				else
				{
					xmlContents += "\t<move doc1=\"" + doc2 +
                    "\" space1=\"" + OffsetRange.spaceToString( offsetSpace ) +
					"\" start1=\"" + fp.second.getStartOffset( offsetSpace ) +
					"\" end1=\"" + fp.second.getEndOffset( offsetSpace ) +
					"\" doc2=\"" + doc1 +
                    "\" space2=\"" + OffsetRange.spaceToString( offsetSpace ) +
					"\" start2=\"" + fp.first.getStartOffset( offsetSpace ) +
					"\" end2=\"" + fp.first.getEndOffset( offsetSpace ) + "\" />\n";
				}
			}
			xmlContents += "</moves>\n";
			
			return xmlContents;
		}
		
		private void clear()
		{
			list.clear();
		}
		private void deserialize(NodeList moveNodes) throws ReportedException
		{
			try
			{
				LinkedList docList = documentManager.getDocumentList();
				
				for (int i = 0; i < moveNodes.getLength(); i++)
				{
					Node node = moveNodes.item(i);
	
					if (node.getNodeType() == Node.ELEMENT_NODE
							&& node.getNodeName().equals("move"))
					{
						FragmentPair fp = new FragmentPair();
						String str = node.getAttributes().getNamedItem("doc1").getNodeValue();
                        int docId1 = docNameToId(docList, str);
                        DocumentModel doc1 = documentManager.lookupDocument(docId1);
                        
                        int start1 = Integer.parseInt(node.getAttributes().getNamedItem("start1").getNodeValue());
                        int end1 = Integer.parseInt(node.getAttributes().getNamedItem("end1").getNodeValue());
                        Node space1Node = node.getAttributes().getNamedItem("space1");
                        OffsetRange.Space space1 = OffsetRange.Space.ACTIVE;
                        if (space1Node != null)
                            space1 = OffsetRange.stringToSpace(space1Node.getNodeValue());
                        fp.first.set(doc1, start1, end1, space1);

                        str = node.getAttributes().getNamedItem("doc2").getNodeValue();
                        int docId2 = docNameToId(docList, str);
                        DocumentModel doc2 = documentManager.lookupDocument(docId2);

						int start2 = Integer.parseInt(node.getAttributes().getNamedItem("start2").getNodeValue());
						int end2 = Integer.parseInt(node.getAttributes().getNamedItem("end2").getNodeValue());
                        Node space2Node = node.getAttributes().getNamedItem("space2");
                        OffsetRange.Space space2 = OffsetRange.Space.ACTIVE;
                        if (space2Node != null)
                            space2 = OffsetRange.stringToSpace(space2Node.getNodeValue());
                        fp.second.set(doc2, start2, end2, space2);
                        
						add(fp.first, fp.second);
					}
				}
			} catch (LoggedException e) {
				throw new ReportedException(e, e.getMessage());
			}
		}
		
		private int docNameToId(LinkedList docList, String strName) throws LoggedException
		{
			for (Iterator it = docList.iterator(); it.hasNext(); )
			{
				JuxtaDocument doc = (JuxtaDocument)it.next();
				if (doc.getDocumentName().equals(strName))
					return doc.getID();
			}
			throw new LoggedException("The document named " + strName + " is not in the current comparison set.");
		}
	}
	
	private MoveList moveList = new MoveList();
	private List<Difference> differenceMap = new LinkedList<Difference>();
	private DocumentManager documentManager;
    private LinkedList listeners = new LinkedList();
	private JuxtaSession juxtaSession;
	
	
	public MovesManager( DocumentManager documentManager )
	{
		this.documentManager = documentManager;
	}
	
	// This returns an empty string if the move can be created, or it returns the reason why not.
	public String canCreate(Fragment left, Fragment right)
	{
		// Before adding a move, check to see that it makes sense. The following cases aren't added:
		// Illegal: Zero or negative length move.
		if ((left.getStartOffset(OffsetRange.Space.ACTIVE) >= left.getEndOffset(OffsetRange.Space.ACTIVE)) || (right.getStartOffset(OffsetRange.Space.ACTIVE) >= right.getEndOffset(OffsetRange.Space.ACTIVE)))
			return "Cannot create a move with a negative length";
		
		// Illegal: Move that overlaps an existing move.
		MoveList ml = moveList.filter(left.getDocumentID(), right.getDocumentID());
		if (ml.isPositionInBlock(left.getDocumentID(), left.getStartOffset(OffsetRange.Space.ACTIVE)))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(left.getDocumentID(), left.getEndOffset(OffsetRange.Space.ACTIVE)))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(right.getDocumentID(), right.getStartOffset(OffsetRange.Space.ACTIVE)))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(right.getDocumentID(), right.getEndOffset(OffsetRange.Space.ACTIVE)))
			return "Cannot create a move that overlaps an existing move";
		
		// Illegal: Move where the end points are outside the document.
		if ((left.getStartOffset(OffsetRange.Space.ACTIVE) < 0 || right.getStartOffset(OffsetRange.Space.ACTIVE) < 0))
			return "Cannot create a move with a negative value in startIndex";
		if ((left.getEndOffset(OffsetRange.Space.ACTIVE) > documentManager.lookupDocument(left.getDocumentID()).getDocumentLength()) ||
				(right.getEndOffset(OffsetRange.Space.ACTIVE) > documentManager.lookupDocument(right.getDocumentID()).getDocumentLength()))
			return "Cannot create a move that goes past the end of the document";
		
		return "";	// if we got here, we couldn't find any problems, so this indicates that the move is legal
	}
	
	public void createMove(Fragment left, Fragment right) throws LoggedException
	{
		// Before adding a move, check to see that it makes sense.
		String errMessage = canCreate(left, right);
		if (!errMessage.equals(""))
			throw new LoggedException(errMessage);

		moveList.add(left, right);
		fireBlocksChanged();
	}

	// gets all moves containing those two docs
	public MoveList getAllMoves(int docId1, int docId2)
	{
		return moveList.filter(docId1, docId2);
	}

	public MoveList getMoves()
	{
		return moveList;
	}

	public MoveList getAllMoves()
	{
		return moveList.deepCopy();
	}
	
	public String getLocationFromFragment(Fragment frag)
	{
		JuxtaDocument document = documentManager.lookupDocument( frag.getDocumentID() );
		// TODO-PER: This only gets the location of the start. Should it get the locations of all items in the range?
		LocationMarker loc = document.getLocationMarker(frag.getStartOffset(OffsetRange.Space.ACTIVE));
		if (loc == null)
			return "n/a";
		return loc.getLocationName();
	}

	public void updateBlock(FragmentPair fp, Fragment left, Fragment right) throws LoggedException
	{
		deleteMove(fp);
		try
		{
			createMove(left, right);
			fireBlocksChanged();
		}
		catch (LoggedException e) {
			// If we failed to create the new move, re-add the original move back in, so we're in a consistent state.
			createMove(fp.first, fp.second);
			throw e;
		}
	}

	// deletes the fragment pair that exactly matches
	public void deleteMove(FragmentPair fp) throws LoggedException
	{
		moveList.remove(fp);
		fireBlocksChanged();
	}

	public void removeDocument(int docId)
	{
		moveList.removeId(docId);
		fireBlocksChanged();
	}
	
	public String serialize()
	{
		return moveList.serialize(Space.ORIGINAL);
	}

    public void addListener( MovesManagerListener listener )
    {
        listeners.add(listener);
    }
    
    private void fireBlocksChanged()
    {
    	if (juxtaSession != null)
    		juxtaSession.markAsModified();

    	notifyListeners();
    }
    
    private void notifyListeners()
    {
        for( Iterator i = listeners.iterator(); i.hasNext(); )
        {
        	MovesManagerListener listener = (MovesManagerListener) i.next();
            listener.movesChanged(this);            
        }
    }
    
	public void setSession(JuxtaSession juxtaSession) {
		this.juxtaSession = juxtaSession;
		
	}

	final private String xmlFileName = "/moves.xml";
	
	public void saveForExport(String folder)
    {
        String contents = moveList.serialize(Space.ACTIVE);
        String fileName = folder + xmlFileName;
        writeStringToFile(fileName, contents);
    }
	
	public void save(String folder)
	{
		String contents = moveList.serialize(Space.ORIGINAL);
		String fileName = folder + xmlFileName;
		writeStringToFile(fileName, contents);
	}
	
	public void load(String folder) throws ReportedException
	{
		String fileName = folder + xmlFileName;
		try
		{
			moveList.clear();
			
		    File f = new File(fileName);
		    if (!f.exists())
		    	return;	// That's ok if the file isn't there. That is the same as not having any moves defined.
		    
			DocumentBuilderFactory factory = DocumentBuilderFactory	.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Node document = parser.parse(fileName);
			NodeList documentChildren = document.getChildNodes();
			if (documentChildren != null)
			{
				for (int i = 0; i < documentChildren.getLength(); i++)
				{
					Node currentNode = documentChildren.item(i);

					if (currentNode.getNodeType() == Node.ELEMENT_NODE
							&& currentNode.getNodeName().equals("moves"))
					{
						NodeList moveNodes = currentNode.getChildNodes();
						moveList.deserialize(moveNodes);
					}
				}
			}
		}
		catch (SAXException e)
		{
			throw new ReportedException(e, fileName + " is not well formed.");
		}
		catch (IOException e)
		{
			throw new ReportedException( e,
					"IO Exception parsing document: " + fileName);
		} 
		catch (FactoryConfigurationError e)
		{
			throw new ReportedException( "Could not locate factory class.","Factory Configuration Error" );
		} 
		catch (ParserConfigurationException e)
		{
			throw new ReportedException( e, "Could not locate a JAXP Parser.");
		}
		notifyListeners();
	}
	
	private void writeStringToFile(String fileName, String contents)
	{
		try {
			Charset utf8 = Charset.forName("UTF-8");
			OutputStreamWriter outStream = new OutputStreamWriter( new FileOutputStream(fileName), utf8 );
			BufferedWriter writer = new BufferedWriter(outStream);

			writer.write(contents);
			writer.close();
		} catch (IOException e) {
			SimpleLogger.logError(e.getMessage());
		}	
	}
	
	// This looks to see if the offset in the document is part of a move. If so, it returns the move.
	public FragmentPair findMove(int id1, int id2, int offset)
	{
		return moveList.findBlock(id1, id2, offset);
	}
	
	// This looks to see if the offset is part of a move and returns the number of moves that match.
	public int countMoves(int id, int offset)
	{
		return moveList.countMoves(id, offset);
	}
	private List convertToDifferences(MoveList ml)
	{
		List differences = new LinkedList();
		for (int i = 0; i < ml.size(); ++i)
		{
			FragmentPair fp = ml.get(i);
			Difference difference = new Difference(fp.first.getDocumentID(), fp.second.getDocumentID(), Difference.MOVE);
			difference.setBaseOffset(fp.first.getStartOffset(OffsetRange.Space.ACTIVE));
			difference.setBaseTextLength(fp.first.getEndOffset(OffsetRange.Space.ACTIVE)-fp.first.getStartOffset(OffsetRange.Space.ACTIVE));
			difference.setWitnessOffset(fp.second.getStartOffset(OffsetRange.Space.ACTIVE));
			difference.setWitnessTextLength(fp.second.getEndOffset(OffsetRange.Space.ACTIVE)-fp.second.getStartOffset(OffsetRange.Space.ACTIVE));
			boolean needsToBeAdded = true;
			for(Difference diff:differenceMap)
			{
				if(difference.same(diff))
				{
					difference = diff;
					needsToBeAdded=false;
				}
			}
			differences.add(difference);
			if(needsToBeAdded)
				differenceMap.add(difference);
		}		
		return differences;
	}
    public List addMoves(List differenceList, int docId, int offset)
    {
		MoveList ml = moveList.findMoves(docId, offset);
		List list = convertToDifferences(ml);
    	if (list.size() > 0)
    	{
    		if (differenceList == null)
    	        differenceList = new LinkedList();
    		differenceList.addAll(list);
    	}
    	return differenceList;
	}
    public List addMoves(List differenceList, int docId)
    {
		MoveList ml = moveList.findMoves(docId);
		List list = convertToDifferences(ml);
    	if (list.size() > 0)
    	{
    		if (differenceList == null)
    	        differenceList = new LinkedList();
    		differenceList.addAll(list);
    	}
    	return differenceList;
	}
//	public void addToHistogramData(byte[] histogramData, int docId)
//	{
//		MoveList ml = moveList.findMoves(docId);
//		for (int i = 0; i < ml.size(); ++i)
//		{
//			FragmentPair fp = ml.get(i);
//			for (int j = fp.first.getStartOffset(OffsetRange.Space.ACTIVE); j < fp.first.getEndOffset(OffsetRange.Space.ACTIVE); ++j)
//				++histogramData[j];
//		}
//	}
}
