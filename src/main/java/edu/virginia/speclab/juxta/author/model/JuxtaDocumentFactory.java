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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.document.DocumentWriter;
import edu.virginia.speclab.diff.document.SourceDocumentModel;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.Juxta;
import edu.virginia.speclab.juxta.author.model.JuxtaXMLParser.DocumentType;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.util.FileUtilities;

/**
 * Responsible for loading and saving <code>JuxtaDocument</code> objects. 
 * @author Nick
 *
 */
public class JuxtaDocumentFactory {
    private Charset encodingCharSet;
    private String juxtaVersion;

    // parsing states
    //    private static final int STRUCTURE_MODE = 0;
    //    private static final int BIBLIO_MODE = 1;
    //    private static final int DOCUMENT_MODE = 2;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public JuxtaDocumentFactory() {
        encodingCharSet = Charset.availableCharsets().get(DEFAULT_ENCODING);
        juxtaVersion = Juxta.JUXTA_VERSION;
    }

    public JuxtaDocumentFactory(String encoding) {
        this.encodingCharSet = Charset.availableCharsets().get(encoding);
        this.juxtaVersion = Juxta.JUXTA_VERSION;
    }

    public JuxtaDocumentFactory(String juxtaVersion, String encoding) {
        this.encodingCharSet = Charset.availableCharsets().get(encoding);
        this.juxtaVersion = juxtaVersion;
    }

    /**
     * Reads the specified text or xml file, parses it and returns a juxta document model object.
     * @param fileName The path to the target file.
     * @return A <code>JuxtaDocument</code> object.
     * @throws ReportedException If there was a problem reading the file.
     */
    public JuxtaDocument readFromFile(File documentFile) throws ReportedException {
        JuxtaDocument document = null;
        try {
            JuxtaXMLParser xmlParser = new JuxtaXMLParser(documentFile, juxtaVersion, encodingCharSet);

            // parse the document. This produces flattened text, a parse tree and raw XML information
            xmlParser.parse();
            BiblioData biblioData = xmlParser.getBiblioData();

            JuxtaXMLParserDocumentPointerTagHandler docPointer = xmlParser.getDocumentPointer();
            File referencedFile = documentFile;
            SourceDocumentModel sourceDoc;
            ParseTemplate parseTemplate = getParseTemplate(docPointer, xmlParser.getRootNode());

            // Hold on there! The parser noticed that this file
            // merely references another file via the <juxta-doc-reference> tag.
            if (docPointer.getReferencedFilename() != null) {
                String referencedFilename = docPointer.getReferencedFilename();
                referencedFile = new File(documentFile.getParent() + File.separator + referencedFilename);

                // Generate a new JuxtaXMLParser for the parsing of the inner file
                xmlParser = new JuxtaXMLParser(referencedFile, juxtaVersion, encodingCharSet);
                
                // setup the parse template
                if ( parseTemplate != null) {
                    xmlParser.setExcludedTagSet( parseTemplate.getExcludedTagSet() );
                    xmlParser.setNotableTagSet( parseTemplate.getNotableTagSet());
                    xmlParser.setNewlineTagSet( parseTemplate.getNewlineTagSet() );
                }
                
                // Now we know if ths document has accepted changes listed
                // in the wrapper file. If this is the case, let the parser
                // know that it shoud use them
                xmlParser.setRevisionsToAccept( docPointer.getAcceptedRevsisons() );

                // Parse again!
                xmlParser.parse();

                // Generate a SourceDocumentModel from the results of the parsing
                sourceDoc = new SourceDocumentModel(referencedFile.getName(), xmlParser.getXMLText(),
                    xmlParser.getRootNode(), xmlParser.getOffsetMap(), 
                    xmlParser.getDocumentType().equals(DocumentType.XML));
            } else {
                // This only happens when the document is first read (and there is no wrapper document)
                // or as a backwards compatability step for previous juxta bundles that did not
                // separate their documents in this way.

                // Create a SourceDocumentModel from information in our parser.
                sourceDoc = new SourceDocumentModel(documentFile.getName(), xmlParser.getXMLText(),
                    xmlParser.getRootNode(), xmlParser.getOffsetMap(), 
                    xmlParser.getDocumentType().equals(DocumentType.XML));

                // We don't have any of this data, so set these to be empty TagSets
                sourceDoc.setExcludedTagSet(new TagSet());
                sourceDoc.setNotableTagSet(new TagSet());
                sourceDoc.setNewlineTagSet(xmlParser.getNewlineTagSet());

                // copy the source document file out where it should go so we can correctly reference it later
                // THIS IS REALLY HACKY!!! But it's a limited case, just when opening an old juxta bundle or
                // adding a new file to an existing bundle. Modifying it
                // and saving it makes it a new juxta bundle that avoids this code path.  This is necessary so
                // if the document is "reparsed" via JuxtaDocumentFactory#reparseDocument we'll have a file to open.
                File targetFile = new File(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/"
                    + JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY
                    + documentFile.getName());
                try {
                    // copy the source file to be where we want it right away, we'll just read it from there
                    FileUtilities.copyFile(documentFile, targetFile, false);
                } catch (IOException e) {
                    throw new ReportedException(e, "unable to copy file.");
                }
            }

            // let the SourceDocumentModel know about all of the tags we saw
            sourceDoc.setElementsEncountered(xmlParser.getElementsEncountered());

            OffsetRange activeRangeFromXPath = null;
            if (xmlParser.getDocumentType() == JuxtaXMLParser.DocumentType.XML) {
  
                JuxtaXMLNode root = xmlParser.getRootNode();
                activeRangeFromXPath = new OffsetRange();
                activeRangeFromXPath.set(root.getXMLStartOffset(), root.getXMLEndOffset(), OffsetRange.Space.ORIGINAL);
            }

            DocumentModel doc = new DocumentModel(sourceDoc, documentFile.getPath(), xmlParser.getFlattenedText(),
                encodingCharSet.displayName());

            if (activeRangeFromXPath != null) {
                doc.setActiveRange(activeRangeFromXPath);
            }

            doc.setLocationMarkerList(xmlParser.getLocationMarkerList());
            doc.setNotes(xmlParser.getNotes());

            // Construct the JuxtaDocument from this document model.
            document = new JuxtaDocument(doc, biblioData);
            if ( parseTemplate != null ) {
                document.setParseTemplateGuid(parseTemplate.getGuid());
            }
            document.setAcceptedRevisions( docPointer.getAcceptedRevsisons() );
            document.setRevisions( xmlParser.getRevisions() );
            document.setPageBreaks( xmlParser.getPageBreaks() );
        } catch (IOException e) {
            throw new ReportedException(e, "An error occured reading the file " + documentFile + ".");
        }
        return document;
    }
    

    private ParseTemplate getParseTemplate(JuxtaXMLParserDocumentPointerTagHandler docPointer, JuxtaXMLNode rootNode) {
        
        TemplateConfig cfg = TemplateConfigManager.getInstance().getConfig(ConfigType.SESSION);
        if( cfg == null ) {
            cfg = TemplateConfigManager.getInstance().getConfig(ConfigType.MASTER);
        }
        
        String templateGuid = docPointer.getParseTemplate();
        if (templateGuid == null || templateGuid.length() == 0) {
            String xmlRoot = rootNode.getName();
           return cfg.getDefaultTemplate(xmlRoot);
        } else {
            return cfg.get(templateGuid);
        }
    }


    /**
     * Writes the document to the target source file. This overwrites any existing file.
     * @param document The document to write.
     * @param sourceFile The file to write to.
     * @throws ReportedException If anything bad happens writing the file. 
     */
    public void writeToFile(JuxtaDocument document, File sourceFile) throws ReportedException {
        JuxtaDocumentWriter documentWriter = new JuxtaDocumentWriter(document);

        try {
            documentWriter.writeDocument(sourceFile);
        } catch (IOException e) {
            throw new ReportedException(e, "An error occurred writing the file " + sourceFile);
        }
    }

    /**
     * Call this method when the parse template has changed and you want the document to be reparsed.
     *
     * @param document The document to reparse
     * @param template The template used to reparse the document
     * @throws ReportedException
     */
    public void reparseDocument(JuxtaDocument document, ParseTemplate template) throws ReportedException {

        // update the document with the new parse template information?
        if (template != null) {
            document.setParseTemplateGuid(template.getGuid());
            document.getSourceDocument().setNotableTagSet(template.getNotableTagSet());
            document.getSourceDocument().setExcludedTagSet(template.getExcludedTagSet());
            document.getSourceDocument().setNewlineTagSet(template.getNewlineTagSet());
        }

        try {
            document.releaseTokenTable();
            SourceDocumentModel sourceDocument = document.getSourceDocument();
            File documentWrapper = new File(document.getFileName());
            File sourceDocumentFile = new File(documentWrapper.getParentFile() + File.separator
                + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY + sourceDocument.getFileName());
            JuxtaXMLParser xmlParser = new JuxtaXMLParser(sourceDocumentFile, this.juxtaVersion,
                this.encodingCharSet);
            xmlParser.setExcludedTagSet(sourceDocument.getExcludedTagSet());
            xmlParser.setNotableTagSet(sourceDocument.getNotableTagSet());
            xmlParser.setNewlineTagSet(sourceDocument.getNewlineTagSet());
            xmlParser.setRevisionsToAccept( document.getAcceptedRevisionsString() );
            xmlParser.parse();

            JuxtaXMLNode root = xmlParser.getRootNode();

            // Tell the source document about the new tree produced by the new parsing
            sourceDocument.setXMLRoot(root);

            // offsets have changed because of new parsing
            sourceDocument.setOffsetMap(xmlParser.getOffsetMap());

            document.setProcessedText(xmlParser.getFlattenedText());
            document.setActiveRange(new OffsetRange(document, root.getStartOffset(), root.getEndOffset(),
                OffsetRange.Space.PROCESSED));
            document.setLocationMarkerList(xmlParser.getLocationMarkerList());
            document.setNotes(xmlParser.getNotes());
            document.setRevisions(xmlParser.getRevisions());
            document.setPageBreaks( xmlParser.getPageBreaks() );
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JuxtaDocumentFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class JuxtaDocumentWriter {
        private JuxtaDocument document;

        public JuxtaDocumentWriter(JuxtaDocument document) {
            this.document = document;
        }

        public void writeDocument(File saveFile) throws IOException {
            // First, write the source file out. This we have in a text buffer inside the document.getSourceDocument()
            File sourceDocFile = new File(saveFile.getParentFile() + File.separator
                + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY + document.getSourceDocument().getFileName());
            OutputStreamWriter outStreamSource = new OutputStreamWriter(new FileOutputStream(sourceDocFile),
                encodingCharSet);
            BufferedWriter sourceWriter = new BufferedWriter(outStreamSource);
            sourceWriter.write(document.getSourceDocument().getRawXMLContent());
            sourceWriter.close();

            // Now, write out the shell document that includes the bibliodata and a reference to the source document.
            StringBuffer buffer = new StringBuffer();

            buffer.append("<?xml version=\"1.0\" encoding=\"" + encodingCharSet.displayName() + "\"?>\n");
            buffer.append("<juxta-document>\n");

            writeBilbioData(buffer);
            buffer.append("<juxta-doc-reference filename=\"" + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY
                + document.getSourceDocument().getFileName() + "\" ");
            buffer.append(" >\n");
            
            buffer.append("<parseTemplate>")
                .append(DocumentWriter.escapeText(this.document.getParseTemplateGuid()))
                .append("</parseTemplate>\n");
            buffer.append("<acceptedRevisions>").append(this.document.getAcceptedRevisionsString()).append("</acceptedRevisions>");

            if (document.getSourceDocument().getNotableTagSet() != null) {
                buffer.append(document.getSourceDocument().getNotableTagSet().toXML("notable"));
            }

            if (document.getSourceDocument().getExcludedTagSet() != null) {
                buffer.append(document.getSourceDocument().getExcludedTagSet().toXML("excluded"));
            }

            if (document.getSourceDocument().getNewlineTagSet() != null) {
                buffer.append(document.getSourceDocument().getNewlineTagSet().toXML("newline"));
            }

            buffer.append("</juxta-doc-reference>");
            buffer.append("</juxta-document>\n");

            OutputStreamWriter outStream = new OutputStreamWriter(new FileOutputStream(saveFile), encodingCharSet);
            BufferedWriter writer = new BufferedWriter(outStream);
            writer.write(buffer.toString());
            writer.close();
        }

        private void writeBilbioData(StringBuffer buffer) {
            BiblioData data = document.getBiblioData();

            buffer.append("<bibliographic>\n");

            buffer.append("<title>" + DocumentWriter.escapeText(data.getTitle()) + "</title>\n");
            buffer.append("<short-title>" + DocumentWriter.escapeText(data.getShortTitle()) + "</short-title>\n");
            buffer.append("<author>" + DocumentWriter.escapeText(data.getAuthor()) + "</author>\n");
            buffer.append("<editor>" + DocumentWriter.escapeText(data.getEditor()) + "</editor>\n");
            buffer.append("<source>" + DocumentWriter.escapeText(data.getSource()) + "</source>\n");
            buffer.append("<date>" + DocumentWriter.escapeText(data.getDate()) + "</date>\n");
            buffer.append("<notes>" + DocumentWriter.escapeText(data.getNotes()) + "</notes>\n");
            if (data.getSortDate() != null)
                buffer.append("<sort-date>" + DocumentWriter.escapeText(Long.toString(data.getSortDate().getTime()))
                    + "</sort-date>\n");

            buffer.append("</bibliographic>\n");
        }

    }

}
