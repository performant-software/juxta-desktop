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

package edu.virginia.speclab.diff.document.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.util.SimpleLogger;

public class SearchDocumentIndex {
	
	private QueryParser parser;
	private Directory directory;
	private Analyzer analyzer;
	private boolean closed;
	private File indexDirectory;
	
	public SearchDocumentIndex( File indexDirectory ) {
		closed = true;
		this.indexDirectory = indexDirectory;
	}
	
	public void openIndex() throws ReportedException {
		
		if( !closed ) {
			close();
		}
		
		try {
			analyzer = new StandardAnalyzer();
			parser = new QueryParser("fulltext", analyzer);
		    directory = FSDirectory.getDirectory(indexDirectory.getPath());
		    closed = false;
		}
		catch( IOException e ) {
			throw new ReportedException(e,"Unable to create index in directory: "+indexDirectory.getPath());
		}		
	}

	public boolean indexExists() {
		File[] fileList = indexDirectory.listFiles();
		if( fileList == null || fileList.length == 0 ) 
			return false;
		return true;
	}
	
	private void addDoc( DocumentModel document, IndexWriter iwriter ) throws ReportedException {
	
		SimpleLogger.logInfo("Lucene search: adding document to index: " + document.getDocumentName());
	    try {
		    Document doc = new Document();
		    doc.add(new Field("docid", Integer.toString(document.getID()), Field.Store.YES, Field.Index.NO));
		    doc.add(new Field("fulltext", document.getDocumentText(), Field.Store.NO, Field.Index.TOKENIZED));
			iwriter.addDocument(doc);
		} catch (CorruptIndexException e) {
			throw new ReportedException(e,"There was an error adding the following text to the search index: "+document.getDocumentName());
		} catch (IOException e) {
			throw new ReportedException(e,"There was an error writing the following text to the search index: "+document.getDocumentName());
		}
	}
	
	private IndexWriter createIndexWriter(boolean forceCreate ) throws ReportedException {
		try {
			boolean bCreate = !indexExists();
			if (forceCreate)
				bCreate= true;
			IndexWriter iwriter = new IndexWriter(directory, analyzer, bCreate);
			iwriter.setMaxFieldLength(Integer.MAX_VALUE);
			return iwriter;
		}
		catch( IOException e ) {
			throw new ReportedException(e,"Unable to create index in directory: "+indexDirectory.getPath());
		}
	}
	
	public void addDocument( DocumentModel document ) throws ReportedException {
		if(isClosed() )
			openIndex();

		try {
			IndexWriter iwriter = createIndexWriter(false);

			addDoc(document, iwriter);
			optimize(iwriter);
			iwriter.close();
		}
		catch( IOException e ) {
			throw new ReportedException(e,"Unable to add the document \"" + document.getDocumentName() + "\" to the index in directory: "+indexDirectory.getPath());
		}
	}

	public void addDocuments(ArrayList documents)  throws ReportedException
	{
		// This is for reindexing all documents at once.
		if(isClosed() )
			openIndex();

		try {
			IndexWriter iwriter = createIndexWriter(true);
			for (int i = 0; i < documents.size(); ++i)
				addDoc((DocumentModel)documents.get(i), iwriter);

			optimize(iwriter);
			iwriter.close();
		}
		catch( IOException e ) {
			throw new ReportedException(e,"Unable to add documents to the index in directory: "+indexDirectory.getPath());
		}
	}
	
	private void optimize(IndexWriter iwriter) throws ReportedException {
		if(isClosed() )
			openIndex();
		
	    try {
			iwriter.optimize();
		} catch (CorruptIndexException e) {
			throw new ReportedException(e,"The index is corrupted and cannot be optimized.");
		} catch (IOException e) {
			throw new ReportedException(e,"There was an error writing the index file during optimization.");
		}		
		
	}
	
	/**
	 * Search for documents containing the search query string and return an
	 * array of integer IDs for DocumentModels.
	 * 
	 * @param searchQuery
	 *            The string to search for.
	 * @return An ArrayList of Document ID Integer values.
	 * @throws ReportedException
	 *             If there was an error reading the index or parsing the query.
	 */
	public ArrayList search( String searchQuery ) throws ReportedException {

		if(isClosed() )
			openIndex();

		ArrayList documentIDs = new ArrayList();
		Hits hits = null;
		
		if( closed ) throw new ReportedException("Attempted to search closed index.","Attempted to search closed index.");
		
		try {
			IndexSearcher isearcher = new IndexSearcher(directory);
			Query query = parser.parse(searchQuery);
		    hits = isearcher.search(query);

		    // Iterate through the results and pull out the document IDs
		    for (int i = 0; i < hits.length(); i++) {
		      Document hitDoc = hits.doc(i);
		      Integer id = new Integer( hitDoc.get("docid") );
		      documentIDs.add(id);
		    }
		    
		} catch (ParseException e) {
			throw new ReportedException(e,"There was an error parsing the search query: "+searchQuery);
		} catch (IOException e) {
			throw new ReportedException(e,"There was an error reading the search index.");
		}
		
		return documentIDs;
	}
	
	public void close() throws ReportedException {
		
		if(closed) return;
		
	    try {
		    //if( isearcher != null ) isearcher.close();
		    directory.close();		
		    closed = true;
		} catch (CorruptIndexException e) {
			throw new ReportedException(e,"The search index was corrupted and could not be closed.");
		} catch (IOException e) {
			throw new ReportedException(e,"There was an error closing the search index file.");
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public File getIndexDirectory() {
		return indexDirectory;
	}
		
}
