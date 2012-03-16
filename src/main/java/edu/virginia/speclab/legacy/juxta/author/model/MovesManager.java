package edu.virginia.speclab.legacy.juxta.author.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.util.SimpleLogger;

public class MovesManager 
{
	// some syntactic sugar to work around Java's problem with new'ing nested classes.
	public static Fragment newFragment() { return new MovesManager(null).new Fragment(); }
	public static Fragment newFragment(int docId, int startIndex, int endIndex) { return new MovesManager(null).new Fragment(docId, startIndex, endIndex); }
	public static FragmentPair newFragmentPair() { return new MovesManager(null).new FragmentPair(); }
	public static MoveList newFragmentMoveList() { return new MovesManager(null).new MoveList(); }

	// This is one side of a move. It defines a fragment of one document
	public class Fragment
	{
		public int docId;
		public int startIndex;
		public int endIndex;
		public void set(int docId, int startIndex, int endIndex)
		{
			this.docId = docId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
		public Fragment()
		{
			docId = 0;
			startIndex = 0;
			endIndex = 0;
		}
		public Fragment(int docId, int startIndex, int endIndex)
		{
			set(docId, startIndex, endIndex);
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
			
			if (first.docId < rhs.first.docId) return -1;
			else if (first.docId > rhs.first.docId) return 1;
			if (second.docId < rhs.second.docId) return -1;
			else if (second.docId > rhs.second.docId) return 1;
			
			if (first.startIndex < rhs.first.startIndex) return -1;
			else if (first.startIndex > rhs.first.startIndex) return 1;
			if (second.startIndex < rhs.second.startIndex) return -1;
			else if (second.startIndex > rhs.second.startIndex) return 1;
			
			if (first.endIndex < rhs.first.endIndex) return -1;
			else if (first.endIndex > rhs.first.endIndex) return 1;
			if (second.endIndex < rhs.second.endIndex) return -1;
			else if (second.endIndex > rhs.second.endIndex) return 1;
			
			return 0;
		}
	}
	
	// This defines a set of moves. It is used to store all the moves in the comparison set, and it is
	// also used to return the subset of moves for two particular documents.
	public class MoveList
	{
		private List list = new ArrayList();
		
		private void add(Fragment left, Fragment right)
		{
			FragmentPair fp = new FragmentPair();
			// Always add the documents with the lower document number first.
			// This makes sorting and retrieval easier
			if (left.docId > right.docId)
			{
				fp.first = right;
				fp.second = left;
			}
			else if (left.docId < right.docId)
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
				if ((fp.first.docId == match.first.docId) && (fp.second.docId == match.second.docId) &&
						(fp.first.startIndex == match.first.startIndex) && (fp.second.startIndex == match.second.startIndex) &&
						(fp.first.endIndex == match.first.endIndex) && (fp.second.endIndex == match.second.endIndex))
				{
					it.remove();
					return;
				}
				else if ((fp.first.docId == match.second.docId) && (fp.second.docId == match.first.docId) &&
						(fp.first.startIndex == match.second.startIndex) && (fp.second.startIndex == match.first.startIndex) &&
						(fp.first.endIndex == match.second.endIndex) && (fp.second.endIndex == match.first.endIndex))
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
				if ((fp.first.docId == docId) || (fp.second.docId == docId))
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
				if ((fp.first.docId == docFirst) && (fp.second.docId == docSecond))
				{
					// We found a match. Now create a similar FragmentPair to return.
					// We need to do a deep copy of this.
					FragmentPair fp2 = new FragmentPair();
					if (docId1 == fp.first.docId)
					{
						fp2.first.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
						fp2.second.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
					}
					else
					{
						fp2.first.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
						fp2.second.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
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
				fp2.first.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
				fp2.second.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
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
				if ((fp.first.docId == docId) && 
						((fp.first.startIndex <= pos) && (fp.first.endIndex >= pos)))
						return true;
				if ((fp.second.docId == docId) && 
						((fp.second.startIndex <= pos) && (fp.second.endIndex >= pos)))
						return true;
			}
			return false;
		}
		
		public FragmentPair findBlock(int docId1, int docId2, int pos)
		{
			for (Iterator it = list.iterator(); it.hasNext(); )
			{
				FragmentPair fp = (FragmentPair)it.next();
				if ((fp.first.docId == docId1) && 
						((fp.first.startIndex <= pos) && (fp.first.endIndex >= pos)) &&
						(fp.second.docId == docId2))
						return fp;
				if ((fp.second.docId == docId1) && 
						((fp.second.startIndex <= pos) && (fp.second.endIndex >= pos)) &&
						(fp.first.docId == docId2))
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
				if ((fp.first.docId == id) && 
						((fp.first.startIndex <= offset) && (fp.first.endIndex >= offset)))
					++count;
				else if ((fp.second.docId == id) && 
						((fp.second.startIndex <= offset) && (fp.second.endIndex >= offset)))
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
				if ((fp.first.docId == id) && 
						((fp.first.startIndex <= offset) && (fp.first.endIndex >= offset)))
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
					fp2.second.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
					ml.add(fp2);
				}
				else if ((fp.second.docId == id) && 
						((fp.second.startIndex <= offset) && (fp.second.endIndex >= offset)))
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
					fp2.second.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
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
				if (fp.first.docId == id)
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
					fp2.second.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
					ml.add(fp2);
				}
				else if (fp.second.docId == id)
				{
					FragmentPair fp2 = new FragmentPair();
					fp2.first.set(fp.second.docId, fp.second.startIndex, fp.second.endIndex);
					fp2.second.set(fp.first.docId, fp.first.startIndex, fp.first.endIndex);
					ml.add(fp2);
				}
			}
			return ml;
		}

		private String serialize()
		{
			String xmlContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<moves>\n";
			
			MoveList moveList = getAllMoves();
			
			for (int i = 0; i < moveList.size(); ++i)
			{
				// NOTE for LEGACY version: added id attribute to moves so that converter can correctly
				// identify which move is which.
				
				FragmentPair fp = moveList.get(i);
				// We want to write this file in a deterministic way to make unit tests easier, and remove the possibilities of subtle
				// errors. To do that, we will save the document name instead of the document id, and we will sort the entries.
				String doc1 = documentManager.lookupDocument(fp.first.docId).getDocumentName();
				String doc2 = documentManager.lookupDocument(fp.second.docId).getDocumentName();
				if (doc1.compareTo(doc2) < 0)
				{
				xmlContents += "\t<move" +
					"   id=\"" + i +
					"\" doc1=\"" + doc1 +
					"\" start1=\"" + fp.first.startIndex +
					"\" end1=\"" + fp.first.endIndex + 
					"\" doc2=\"" + doc2 +
					"\" start2=\"" + fp.second.startIndex +
					"\" end2=\"" + fp.second.endIndex + "\" />\n";
				}
				else
				{
					xmlContents += "\t<move" +
					"   id=\"" + i +
					"\" doc1=\"" + doc2 +
					"\" start1=\"" + fp.second.startIndex +
					"\" end1=\"" + fp.second.endIndex + 
					"\" doc2=\"" + doc1 +
					"\" start2=\"" + fp.first.startIndex +
					"\" end2=\"" + fp.first.endIndex + "\" />\n";
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
						fp.first.docId = docNameToId(docList, str);
						fp.first.startIndex = Integer.parseInt(node.getAttributes().getNamedItem("start1").getNodeValue());
						fp.first.endIndex = Integer.parseInt(node.getAttributes().getNamedItem("end1").getNodeValue());
						str = node.getAttributes().getNamedItem("doc2").getNodeValue();
						fp.second.docId = docNameToId(docList, str);
						fp.second.startIndex = Integer.parseInt(node.getAttributes().getNamedItem("start2").getNodeValue());
						fp.second.endIndex = Integer.parseInt(node.getAttributes().getNamedItem("end2").getNodeValue());
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
	private JuxtaSession_1_3_1 juxtaSession;
	
	
	public MovesManager( DocumentManager documentManager )
	{
		this.documentManager = documentManager;
	}
	
	// This returns an empty string if the move can be created, or it returns the reason why not.
	public String canCreate(Fragment left, Fragment right)
	{
		// Before adding a move, check to see that it makes sense. The following cases aren't added:
		// Illegal: Zero or negative length move.
		if ((left.startIndex >= left.endIndex) || (right.startIndex >= right.endIndex))
			return "Cannot create a move with a negative length";
		
		// Illegal: Move that overlaps an existing move.
		MoveList ml = moveList.filter(left.docId, right.docId);
		if (ml.isPositionInBlock(left.docId, left.startIndex))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(left.docId, left.endIndex))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(right.docId, right.startIndex))
			return "Cannot create a move that overlaps an existing move";
		if (ml.isPositionInBlock(right.docId, right.endIndex))
			return "Cannot create a move that overlaps an existing move";
		
		// Illegal: Move where the end points are outside the document.
		if ((left.startIndex < 0 || right.startIndex < 0))
			return "Cannot create a move with a negative value in startIndex";
		if ((left.endIndex > documentManager.lookupDocument(left.docId).getDocumentLength()) ||
				(right.endIndex > documentManager.lookupDocument(right.docId).getDocumentLength()))
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
	
	public MoveList getAllMoves()
	{
		return moveList.deepCopy();
	}
	
	public String getLocationFromFragment(Fragment frag)
	{
		JuxtaDocument document = documentManager.lookupDocument( frag.docId );
		// TODO-PER: This only gets the location of the start. Should it get the locations of all items in the range?
		LocationMarker loc = document.getLocationMarker(frag.startIndex);
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
		return moveList.serialize();
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
    
	public void setSession(JuxtaSession_1_3_1 juxtaSession) {
		this.juxtaSession = juxtaSession;
		
	}

	final private String xmlFileName = "/moves.xml";
	
	public void save(String folder)
	{
		String contents = moveList.serialize();
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
			Difference difference = new Difference(fp.first.docId, fp.second.docId, Difference.MOVE);
			difference.setBaseOffset(fp.first.startIndex);
			difference.setBaseTextLength(fp.first.endIndex-fp.first.startIndex);
			difference.setWitnessOffset(fp.second.startIndex);
			difference.setWitnessTextLength(fp.second.endIndex-fp.second.startIndex);
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
//			for (int j = fp.first.startIndex; j < fp.first.endIndex; ++j)
//				++histogramData[j];
//		}
//	}
}
