package edu.virginia.speclab.juxta.author.model.template;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.virginia.speclab.exceptions.FatalException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate.Action;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate.Behavior;

/**
 * This class is a wrapper awound a basic <code>ArrayList<ParserTemplate></code>.
 * It allows for cleaner code, and adds a few helper methods.
 * 
 * @author loufoster
 *
 */
public final class TemplateConfig extends ArrayList<ParseTemplate> {
    
    private File configFile;
    
    private static final String CONFIG_SCHEMA = "templates.xsd";
    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    
    /**
     * Private c'tors. This class must be instantiated with the static
     * factory method 'fromFile'
     */
    private TemplateConfig( final File file) {
        this.configFile = file;
    }
    public TemplateConfig() throws Exception {
        throw new Exception("Unsupported no-arg constructor");
    }
    
    /**
     * Update the file that will be used to persist the template data.
     * @param newFile
     */
    public void updateFile( final File newFile) {
        this.configFile = newFile;
    }
    
    /**
     * Factory Method. Create a new instance of a template config from the contents
     * of the specified file. This xml must validate againtst res/template.xsd.
     * 
     * @param cfgPath Relative path to the config file 
     * @throws FatalExceptions 
     */
    public static final TemplateConfig fromFile( final String cfgPath ) throws FatalException {
 
        TemplateConfig result = new TemplateConfig( new File(cfgPath) );
        
        try {
            // setup the schema aware document builder
            DocumentBuilder docBuilder = createDocBuilder();
    
            // parse the config into a doc. and walk the template nodes
            Document doc = docBuilder.parse( result.configFile );
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//template");
            XPathExpression tagExpr = xpath.compile("tag");
            NodeList nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            for ( int i=0; i<nodes.getLength(); i++) {
                Node template = nodes.item(i);
                String rootTagName = template.getAttributes().getNamedItem("rootTagName").getTextContent();
                String guid = null;
                Node guidNode = template.getAttributes().getNamedItem("guid");
                if ( guidNode != null ) {
                    guid = guidNode.getTextContent();
                }
                String name = template.getAttributes().getNamedItem("name").getTextContent();
                boolean isDefault = Boolean.parseBoolean( template.getAttributes().getNamedItem("isDefault").getTextContent() );
                
                // create a new template for the config list; add a guid if this 
                // template was created without one
                ParseTemplate parseTemplate = new ParseTemplate(guid, rootTagName, name, isDefault);
                
                // Walk all of its children
                // and add behaviors to this template for each one
                NodeList tags = (NodeList)tagExpr.evaluate(template, XPathConstants.NODESET);
                for (int t=0;t<tags.getLength();t++) {
                    Node tag = tags.item(t);
                    String tagName = tag.getAttributes().getNamedItem("name").getTextContent();
                    String tagAct = tag.getAttributes().getNamedItem("action").getTextContent();
                    boolean tagNl = Boolean.parseBoolean(tag.getAttributes().getNamedItem("newLine").getTextContent());
                    parseTemplate.addBehavior(tagName, Action.valueOf(tagAct), tagNl);
                }
                
                // add the new template to the config object
                result.add( parseTemplate );
            }
        } catch (Exception e) {
            // just rethrow as a Fatal
            throw new FatalException(e, "Unable to initialize parsing templates.");
        }
        
        return result;
    }
    
    /**
     * Helper method used to setup a schema aware, validating XML document builder
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private static DocumentBuilder createDocBuilder() throws SAXException, ParserConfigurationException {
        // load the template schema
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XML_SCHEMA);
        URL schemaUrl = ClassLoader.getSystemResource(CONFIG_SCHEMA);
        Schema schema = schemaFactory.newSchema(schemaUrl);
   
        // setup a docBuilder that will validate against the schema
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setSchema(schema);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        docBuilder.setErrorHandler(new TemplateErrorHander() );
        return docBuilder;
    }
    
    /**
     * Save any changes to this configuration out to the same file that
     * it was loaded from.
     * @throws ReportedException 
     */
    public void save() throws ReportedException {
        
        try {
            // setup the schema aware document builder and create new doc
            DocumentBuilder docBuilder = createDocBuilder();
            Document outDoc = docBuilder.newDocument();
            Element rootEle = outDoc.createElement("templates");
            outDoc.appendChild(rootEle);
            
            // dump the list of cfg data into xml doc: grab each template class
            for ( ParseTemplate template : this) {
                
                // ...transform into new template element...
                Element templateEle = outDoc.createElement("template");
                templateEle.setAttribute("guid", template.getGuid());
                templateEle.setAttribute("name", template.getName());
                templateEle.setAttribute("rootTagName", template.getRootTagName());
                templateEle.setAttribute("isDefault", Boolean.toString(template.isDefault()));
                
                // ...and add a child element for each behavior
                for ( Behavior behavior : template.getBehaviors()) {
                    Element tagEle = outDoc.createElement("tag");
                    tagEle.setAttribute("name", behavior.getTagName());
                    tagEle.setAttribute("action", behavior.getAction().toString());
                    tagEle.setAttribute("newLine", Boolean.toString(behavior.getNewLine()));
                    templateEle.appendChild(tagEle);
                }
                
                // ...add the filled-out element to the templates element
                rootEle.appendChild(templateEle);
            }
                
            // Get a document stream to the config file
            Source source = new DOMSource(outDoc);
            Result result = new StreamResult(this.configFile);
    
            // write source document out to the result stream
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
            
        } catch (Exception e) {
            throw new ReportedException(e, "Unable to save changes to template configuration");
        }
    }
    
    /**
     * Check if the supplied name is a known template confguration
     * 
     * @param name Target name
     * @return True if this name is valid, false otherwise
     */
    public final boolean isTemplate( final String name) {
        for (ParseTemplate template: this) {
            if ( template.getName().equals(name) ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get a ParseTemplate by GUID.
     * 
     * @param guid Target template guid
     * @return The template, or null if the name is invalid
     */
    public final ParseTemplate get( final String guid) {
        for (ParseTemplate template: this) {
            if ( template.getGuid().equals(guid) ) {
                return template;
            }
        }
        return null;
    }
    
    /**
     * Get the default ParseTemplate for the specified root node name. The default template
     * is either a matching template with the default flag set, or the first match found
     * 
     * @param name Root element name of the XML document to be parsed
     * @return The default template, or null of none match
     */
    public final ParseTemplate getDefaultTemplate( final String rootElementName) {
        ParseTemplate match = null;
        for (ParseTemplate template: this) {
            if ( template.getRootTagName().equals(rootElementName) ) {
                match = template;
                if ( template.isDefault() ) {
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Get the template at the specifed index and update it with changes
     * in the new version. Also update the default setting
     * 
     * @param originalIndex
     * @param updatedTemplate
     * @param isDefault
     */
    public void updateTemplate(int originalIndex, ParseTemplate updatedTemplate, boolean isDefault) {
        this.set(originalIndex, updatedTemplate);
        
        // if we are setting a new default, clear any old ones
        if ( isDefault == true ) {
            for (ParseTemplate other: this) {
                if ( other.getRootTagName().equals(updatedTemplate.getRootTagName())) {
                    other.setDefault(false);
                }
            }
        }
        updatedTemplate.setDefault(isDefault);
    }
    
    /**
     * Add a template to the configuration. If a template with a matching 
     * name already exists, it will be overwrittem
     */
    @Override
    public boolean add( ParseTemplate template ) {
        if ( this.contains(template) ) {
            int idx = this.indexOf(template);
            this.set(idx, template.clone());
        } else {
            super.add(template.clone());
        }
        return true;
    }


    /**
     * Generate a basic template based on the root name and tag set. Initialy
     * mark is as the default. The basic template will INCLUDE all tags and
     * add no new lines. Add this new template to the list of template configrations.
     * 
     * @param rootTagName Name of root tag
     * @param tagNames List of tag names from a document
     * @return A new template
     */
    public ParseTemplate createTemplate(final String name, final String rootTag, final Set<String> tagNames, boolean isDefault) {
        
        ParseTemplate template = new ParseTemplate(rootTag, name, rootTag, isDefault);
        for ( String tag : tagNames ) {
            template.addBehavior(tag, Action.INCLUDE, false);
        }
        add(template);
        return template;
    } 
    
    /**
     * Create a new template by cloning the source template. Add this new template
     * to the list of template configrations.
     * 
     * @param name Name of the new templaye
     * @param sourceTemplate The template to clone
     * @return A new template
     */
    public ParseTemplate createTemplate(String name, ParseTemplate sourceTemplate) {
        ParseTemplate template = new ParseTemplate(sourceTemplate.getRootTagName(), name, false);
        for ( Behavior behavior : sourceTemplate.getBehaviors() ) {
            template.addBehavior(behavior.getTagName(), behavior.getAction(), behavior.getNewLine());
        }
        add(template);
        return template;
    }
    
    /**
     * Error handler for parsing template config. Ignore warnings and re-throw
     * any encountered errors
     */
    private static class TemplateErrorHander implements org.xml.sax.ErrorHandler {

        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            // no-op
        }
    }
}
