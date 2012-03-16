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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import say.swing.JFontChooser;

import com.Ostermiller.util.Browser;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.FatalException;
import edu.virginia.speclab.exceptions.LoggedException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.Juxta;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaDocumentFactory;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionFile;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionListener;
import edu.virginia.speclab.juxta.author.model.LoaderCallBack;
import edu.virginia.speclab.juxta.author.model.SearchResults;
import edu.virginia.speclab.juxta.author.model.template.ParseTemplate;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfig;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager.ConfigType;
import edu.virginia.speclab.juxta.author.view.collation.CollationViewPanel;
import edu.virginia.speclab.juxta.author.view.collation.CollationViewTextArea;
import edu.virginia.speclab.juxta.author.view.collation.DifferenceViewerListener;
import edu.virginia.speclab.juxta.author.view.compare.DocumentCompareView;
import edu.virginia.speclab.juxta.author.view.export.LoginDialog;
import edu.virginia.speclab.juxta.author.view.export.WebServiceClient;
import edu.virginia.speclab.juxta.author.view.export.WebServiceExportDialog;
import edu.virginia.speclab.juxta.author.view.search.SearchDialog;
import edu.virginia.speclab.juxta.author.view.search.SearchListener;
import edu.virginia.speclab.juxta.author.view.search.SearchOptions;
import edu.virginia.speclab.juxta.author.view.templates.BaseTemplateDialog;
import edu.virginia.speclab.juxta.author.view.templates.EditTemplateDialog;
import edu.virginia.speclab.juxta.author.view.templates.ListTemplatesDialog;
import edu.virginia.speclab.juxta.author.view.templates.SelectTemplateDialog;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.juxta.author.view.ui.StatusBar;
import edu.virginia.speclab.ui.PanelTitle;
import edu.virginia.speclab.ui.filetree.FileTreePanel;
import edu.virginia.speclab.ui.filetree.FileTreePanelSelectionListener;
import edu.virginia.speclab.util.ExtensionFilter;
import edu.virginia.speclab.util.ExtensionGroupFilter;
import edu.virginia.speclab.util.FileUtilities;
import edu.virginia.speclab.util.OSXAdapter;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This is the main window frame for Juxta. It is the top level view object which houses all component views and also
 * drives the main menu and toolbars. It owns the <code>JuxtaSession</code> object which is the top level model object.
 * 
 * @author Nick
 * 
 */
public class JuxtaAuthorFrame extends JFrame implements JuxtaSessionListener, DifferenceViewerListener,
        JuxtaUserInterfaceStyle, LoaderCallBack, FileTreePanelSelectionListener, SearchListener {
    private static final long serialVersionUID = 4217925689604189779L;

    // model root
    private JuxtaSession session;
    
    // Web service members
    private final String webServiceUrl;
    private String webUserEmail = "";
    private String webUserPassword = "";
    
    // last template used. supports sifter 2347: remember last template choice
    private ParseTemplate priorTemplate = null;

    private JSplitPane textImageSplit;
    private PanelTitle comparisonSetTitlePanel;
    private SearchDialog searchDialog;
    private int lastFindOffset = 0;
    private ProgressDialog progressDialog;
    private DifferenceHistogramDialog histogramDialog;
    private ViewSourcePanel viewSourcePanel;
    private ComparisonExplorer comparisonExplorer;
    private DocumentCompareView docCompareView;
    private AddDocumentDialog addDocumentDialog;
    private MovesPanel movesPanel;
    private DocumentViewer documentViewer;
    private CardLayout mainViewCardLayout;
    private JPanel mainViewCardPanel;
    private CollationViewPanel collationViewPanel;
    private ImageDisplay imageDisplay;
    private StatusBar statusBar;
    private SearchResultsPanel searchResultsPanel;
    private AnnotationPanel annotationPanel;
    private JTabbedPane mainViewTabPane;
    private JButton saveButton;
    private JButton wsExportButton;
    private JTabbedPane secondaryViewTabPane;
    private ProcessingInProgressDialog addingDocumentProgressDialog;

    // actions
    private NewAction newAction;
    private FileOpenAction fileOpenAction;
    private FileCloseAction fileCloseAction;
    private FileOpenSampleAction fileOpenSampleAction;
    private FileSaveAction fileSaveAction;
    private FileSaveAsAction fileSaveAsAction;
    private ExitAction exitAction;
    private SearchAction searchAction;
    private ToolbarSearchAction toolbarSearchAction;
    private AddDocumentAction addDocumentAction;
    private RemoveDocumentAction removeDocumentAction;
    private HistogramAction histogramAction;
    private CollateAction collateAction;
    private RequiredCollateAction requiredCollateAction;
    private EditPropertiesAction propertiesAction;
    private AboutAction aboutAction;
    private HelpAction helpAction;
    private ScreenshotAction screenshotAction;
    private ExportAction exportAction;
    private ExportOrphanedAnnotationsAction exportOrpanedAnnotationsAction;
    private WebServiceExportAction wsExportAction;
    private FontPickerAction fontPickerAction;
    private JComboBox comboSearchText;
    public static final String SEARCH_PROMPT = "Type here to search for terms in all documents.";
    // private DebugAction debugAction;

    public static final String WELCOME_FILE = "sample/welcome.jxt";
    public static final String TEST_FILE = "sample/BlessedDamozel.jxt";

    public static final String DEFAULT_SET_ID = "juxta_set";

    public static final int VIEW_MODE_COLLATION = 1;
    public static final int VIEW_MODE_COMPARISON = 2;
    public static final int VIEW_MODE_DONT_CARE = 3;

    public static final int LEFT_PANEL_COLLATION_TAB = 0;
    public static final int LEFT_PANEL_FILES_TAB = 1;

    private static final int SOURCE_TAB = 0;
    private static final int IMAGES_TAB = 1;
    private static final int NOTES_TAB = 2;
    private static final int MOVE_TAB = 3;
    private static final int SEARCH_TAB = 4;

    public static final String DOCUMENT_VIEWER = "documentViewer";
    public static final String MAIN_VIEW_TAB_PANE = "mainTabPane";

    private Action toggleLocationMarkAction;
    private AbstractButton toggleLocationMarkButton;

    private ExportTextualApparatusAction exportTextualApparatusAction;

    private JTabbedPane leftViewTabPane;

    // Check that we are on Mac OS X. This is crucial to loading and using the
    // OSXAdapter class.
    private static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));

    // Ask AWT which menu modifier we should be using.
    final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public JuxtaAuthorFrame(String wsUrl, String startFile) throws FatalException {
        super();
        
        // web service URL setup
        if ( wsUrl == null || wsUrl.length() == 0 ) {
            this.webServiceUrl = WebServiceClient.DEFAULT_URL;
        } else {
            this.webServiceUrl = wsUrl;
        }
        
        // Starting file setup
        File juxtaDataFile;
        if ( startFile == null || startFile.length() == 0 ) {
            juxtaDataFile = new File(WELCOME_FILE); 
        } else {
            juxtaDataFile = new File( startFile );
        }

        // Set up our application to respond to the Mac OS X application menu
        registerForMacOSXEvents();

        newAction = new NewAction(this);
        fileOpenAction = new FileOpenAction();
        fileCloseAction = new FileCloseAction(this);
        fileOpenSampleAction = new FileOpenSampleAction();
        exitAction = new ExitAction();
        searchAction = new SearchAction();
        toolbarSearchAction = new ToolbarSearchAction();
        addDocumentAction = new AddDocumentAction();
        removeDocumentAction = new RemoveDocumentAction(this);
        fileSaveAction = new FileSaveAction();
        fileSaveAsAction = new FileSaveAsAction();
        histogramAction = new HistogramAction();
        collateAction = new CollateAction();
        requiredCollateAction = new RequiredCollateAction();
        toggleLocationMarkButton = new JButton();
        toggleLocationMarkAction = new ToggleLocationMarkAction();
        exportTextualApparatusAction = new ExportTextualApparatusAction();
        propertiesAction = new EditPropertiesAction(this);
        aboutAction = new AboutAction();
        helpAction = new HelpAction();
        screenshotAction = new ScreenshotAction(this);
        exportAction = new ExportAction();
        wsExportAction = new WebServiceExportAction(this);
        exportOrpanedAnnotationsAction = new ExportOrphanedAnnotationsAction();
        fontPickerAction = new FontPickerAction(this);
        comboSearchText = new JComboBox();
        viewSourcePanel = new ViewSourcePanel(this);
        collationViewPanel = new CollationViewPanel( this );
        // debugAction = new DebugAction();

        initUI();

        // create a search dialog for later use.
        searchDialog = new SearchDialog(this);
        searchDialog.addSearchListener(this);

        // create a progress dialog for later use
        progressDialog = new ProgressDialog(this);

        addDocumentDialog = new AddDocumentDialog();

        histogramDialog = new DifferenceHistogramDialog(this);
        getCollationView().addListener(viewSourcePanel);

        addingDocumentProgressDialog = new ProcessingInProgressDialog(this, "Adding document...");

        statusBar.setText("juxta v" + Juxta.JUXTA_VERSION + " - www.juxtasoftware.org");

        JuxtaSession session = null;
        try {
            session = JuxtaSession.createSession(juxtaDataFile, this, true);
        } catch (Exception e) {
            // if anything goes wrong, try creating a blank session instead.
            try {
                session = JuxtaSession.createSession(null, this, true);
            } catch (LoggedException e1) {
                // no good, give it up
                throw new FatalException(e1, "Unable to create new session.");
            }
        }
        saveButton.setEnabled(false);
        wsExportButton.setEnabled(true);

        try {
            loadSession(session);
        } catch (ReportedException e) {
            throw new FatalException(e, "Unable to load session.");
        }

    }
    
    public boolean hasLoggedOnWeb() {
        return (this.webUserEmail.length() > 0 && this.webUserPassword.length() > 0);
    }
    public void setWebUserEmail( final String email ) {
        this.webUserEmail = email;
    }
    public String getWebUserEmail() {
        return this.webUserEmail;
    }
    public void setWebUserPassword( final String pass ) {
        this.webUserPassword = pass;
    }
    public String getWebUserPassword() {
        return this.webUserPassword;
    }
    public String getWebServiceUrl() {
        return this.webServiceUrl;
    }

    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing
    // any Apple APIs
    public void registerForMacOSXEvents() {
        if (MAC_OS_X) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of
                // all the methods we wish to
                // use as delegates for various
                // com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[]) null));
                // OSXAdapter.setPreferencesHandler(this,
                // getClass().getDeclaredMethod("preferences", (Class[])null));
                // OSXAdapter.setFileHandler(this,
                // getClass().getDeclaredMethod("loadImageFile", new Class[] {
                // String.class }));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }

    // General info dialog; fed to the OSXAdapter as the method to call when
    // "About OSXAdapter" is selected from the application menu
    public void about() {
        AboutDialog dialog = new AboutDialog(JuxtaAuthorFrame.this);
        dialog.setVisible(true);
    }

    // General preferences dialog; fed to the OSXAdapter as the method to call
    // when
    // "Preferences..." is selected from the application menu
    // public void preferences() {
    // prefs.setLocation((int)this.getLocation().getX() + 22,
    // (int)this.getLocation().getY() + 22);
    // prefs.setVisible(true);
    // }

    // General quit handler; fed to the OSXAdapter as the method to call when a
    // system quit event occurs
    // A quit event is triggered by Cmd-Q, selecting Quit from the application
    // or Dock menu, or logging out
    public boolean quit() {
        if (openSavePromptDialog()) {
            if (this.session != null) {
                try {
                    this.session.close();
                } catch (ReportedException e) {
                    ErrorHandler.handleException(e);
                }
            }
            return true;
        }
        return false;
        // int option = JOptionPane.showConfirmDialog(this,
        // "Are you sure you want to quit?", "Quit?",
        // JOptionPane.YES_NO_OPTION);
        // return (option == JOptionPane.YES_OPTION);
    }

    /**
     * Reload the current session using the passed document ID as the base for collation
     * 
     * @param baseDocumentID
     * @throws ReportedException
     */
    public void reloadSession(final int baseDocumentID) throws ReportedException {

        this.session.resetCurrentCollation();
        this.comparisonExplorer.setJuxtaAuthorSession(this.session);
        this.collationViewPanel.setSession(this.session);
        this.histogramDialog.setSession(this.session);
        this.docCompareView.setSession(this.session);
        this.annotationPanel.setSession(this.session);
        this.searchResultsPanel.setSession(this.session);
        this.movesPanel.setSession(this.session);
        this.documentViewer.setSession(this.session);
        this.viewSourcePanel.setSession(this.session);
        this.session.setBaseText(null);
        this.collationViewPanel.setDocument(null);
        this.imageDisplay.resetImage();
        this.histogramDialog.close();

        this.session.startSession(this);
    }

    // Load the session object into the component view objects and display it on
    // the screen.
    private void loadSession(JuxtaSession session) throws ReportedException {
        // clean up previous session
        if (this.session != null) {
            this.session.removeListener(this);
            this.session.close();
        }

        // make this the new session
        this.session = session;

        // start listening for updates to this session
        this.session.addListener(this);

        this.comparisonExplorer.setJuxtaAuthorSession(this.session);
        this.collationViewPanel.setSession(this.session);
        this.histogramDialog.setSession(this.session);
        this.docCompareView.setSession(this.session);
        this.annotationPanel.setSession(this.session);
        this.searchResultsPanel.setSession(this.session);
        this.movesPanel.setSession(this.session);
        this.documentViewer.setSession(this.session);
        this.viewSourcePanel.setSession(this.session);

        this.session.setBaseText(null);
        this.collationViewPanel.setDocument(null);
        this.imageDisplay.resetImage();
        this.histogramDialog.close();

        // if there is a save file associate with this session, display it on
        // the title bar
        String archiveName = this.session.getDocumentManager().getArchiveFileName();
        updateTitleBar(archiveName);

        // start the session, calls back with loadingComplete() when done
        // loading.
        this.session.startSession(this);
    }

    private void updateTitleBar(String archiveName) {
        if (archiveName != null) {
            setTitle("Juxta - " + archiveName);
        } else {
            setTitle("Juxta");
        }
    }

    /**
     * Removes the currently selected document from the comparison set and updates the associated views.
     */
    public void removeDocument() {
        JuxtaDocument document = comparisonExplorer.getSelectedDocument();
        removeDocument(document);
    }

    public void removeDocument(JuxtaDocument document) {
        if (document != null) {
            SimpleLogger.logInfo("removing document: " + document.getDocumentName());

            // remove the document from the session
            try {
                this.session.removeDocument(document);
            } catch (ReportedException e) {
                ErrorHandler.handleException(e);
            }

            // remove the document from the explorer list
            comparisonExplorer.removeDocument(document);

            // if this is the current document, remove it from the screen.
            if (getCollationView().getCurrentText() == document) {
                this.collationViewPanel.setDocument(null);
            }
        }
    }

    private interface DocumentRemoverCallback {
        void finishedRemovingDocument(JuxtaDocument document);
    }

    private class DocumentRemover extends Thread {
        JuxtaDocument document;
        DocumentRemoverCallback callback;

        public DocumentRemover(JuxtaDocument deadDocument, DocumentRemoverCallback callback) {
            document = deadDocument;
            this.callback = callback;
        }

        public void run() {
            removeDocument(document);
            callback.finishedRemovingDocument(document);
        }
    }

    // Exit the software. This is the single exit point.
    private void exit() {
        if (openSavePromptDialog()) {
            if (this.session != null) {
                try {
                    this.session.close();
                } catch (ReportedException e) {
                    ErrorHandler.handleException(e);
                }
            }
            dispose();
            System.exit(0);
        }
    }

    private void close() {
        if (openSavePromptDialog()) {
            try {
                loadSession(JuxtaSession.createSession(null, this, false));
                histogramDialog.close();
            } catch (LoggedException e1) {
                ErrorHandler.handleException(e1);
            }
        }
    }

    // Opens the collation dialog
    private boolean openCollationDialog() {
        TokenizerSettings oldSettings = this.session.getComparisonSet().getTokenizerSettings();

        CollationDialog dialog = new CollationDialog(oldSettings, this);
        dialog.setVisible(true);

        TokenizerSettings settings = dialog.getSettings();

        if (dialog.isOk() && settings != null) {
            // update the tokenizer settings
            this.session.getComparisonSet().setTokenizerSettings(settings);

            try {
                // recalculate based on new settings.
                this.session.refreshComparisonSet();

                // reload the session
                loadSession(this.session);
                this.session.markAsModified();
            } catch (LoggedException e) {
                ErrorHandler.handleException(e);
            }
            return true;
        }
        return false;
    }

    // This is the single point of entry for saving the current session.
    private boolean save(File saveFile) {
        if (saveFile == null) {
            saveFile = openSaveDialog();

            // pre-existing file, prompt for overwrite
            if (saveFile != null && saveFile.exists()) {
                if (overwriteFilePrompt(saveFile.getName()) == false)
                    return false;
            }
        }

        if (saveFile == null)
            return false;

        try {
            this.session.saveSession(saveFile);
            saveButton.setEnabled(false);
            updateTitleBar(saveFile.getName());
            return true;
        } catch (LoggedException e1) {
            ErrorHandler.handleException(e1);
            return false;
        }
    }

    private boolean overwriteFilePrompt(String fileName) {
        String prompt = "File " + fileName + " already exists. Overwrite?";

        Object[] options = { "Yes", "No" };

        int n = JOptionPane.showOptionDialog(this, prompt, "Overwrite?", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        switch (n) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                return false;
        }
    }

    // Displays the open file dialog for loading a new session.
    private void openFileDialog(boolean useSampleDirectory) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        File myDir = Juxta.selectStartDirectory(useSampleDirectory);
        chooser.setCurrentDirectory(myDir);
        chooser.setAcceptAllFileFilterUsed(false);
        ExtensionGroupFilter fileFilter = new ExtensionGroupFilter("Juxta File", true);
        fileFilter.addExtension(JuxtaSession.JUXTA_FILE_EXTENSION);
        chooser.setFileFilter(fileFilter);
        chooser.setDialogTitle("Open Comparison Set");
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            setCursor(JuxtaUserInterfaceStyle.WAIT_CURSOR);

            File sf = chooser.getSelectedFile();
            try {
                JuxtaSession session = null;
                if (this.session != null)
                    this.session.close();
                session = JuxtaSession.createSession(sf, this, true);

                loadSession(session);
                saveButton.setEnabled(false);
            } catch (Exception e) {
                if (e instanceof InvalidClassException) {
                    ErrorHandler.handleException(new ReportedException(e, "Unable to load file, format obsolete."));
                } else {
                    ErrorHandler.handleException(e);
                }
            }
        }
    }

    private File openSaveDialog() {
        return openSaveDialog(JuxtaSession.JUXTA_FILE_EXTENSION, "Juxta File");
    }

    // Displays the file save dialog for saving the current session.
    private File openSaveDialog(String extension, String description) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        File myDir = Juxta.selectStartDirectory(false);
        chooser.setCurrentDirectory(myDir);

        ExtensionFilter fileFilter = new ExtensionFilter(extension, description, true);
        chooser.setFileFilter(fileFilter);

        int option = chooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();

            // add juxta extension if necessary
            selectedFile = FileUtilities.appendFileExtension(selectedFile, extension);

            return selectedFile;
        } else {
            return null;
        }
    }

    // Displays the Add Document dialog.
    private void openAddDocumentDialog() {
        addDocumentDialog.setDialogTitle("Add Document");

        int returnVal = addDocumentDialog.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            comparisonExplorer.setSelectionEnabled(false);
            System.gc();
            File file = addDocumentDialog.getSelectedFile();

            try {
                if (file != null && file.isFile()) {
                    JuxtaDocumentFactory factory = new JuxtaDocumentFactory(addDocumentDialog.getEncoding());
                    JuxtaDocument document = this.session.getDocumentManager().constructDocument(file.getName(),
                            file.getAbsolutePath(), addDocumentDialog.getEncoding());

                    if (document != null ) {
                        // no need to process non-xml files. just add them
                        if ( document.isXML() == false) {
                            this.session.addExistingDocument(document, this);
                            addingDocumentProgressDialog.showDialog();
                            return;
                        }
                        
                        // setup some common data for the rest of this mess:
                        ParseTemplate template = null;
                        TemplateConfig templateCfg = TemplateConfigManager.getInstance().getConfig(ConfigType.SESSION);
                        String rootName = document.getSourceDocument().getXMLRoot().getName();
                        boolean newTemplate = false;
                        BaseTemplateDialog dialog = null;
                        
                        // was a template used before?
                        if ( this.priorTemplate != null ) {
                            // make sure it still applies
                            if ( rootName.equals(this.priorTemplate.getRootTagName())) {
                                template = this.priorTemplate;
                            } else {
                                // Doesn't apply: try to get different existing one
                                template = templateCfg.getDefaultTemplate(rootName);
                                this.priorTemplate = null;
                            } 
                        } else {
                            // see if a template exists for this type of document
                            template = templateCfg.getDefaultTemplate(rootName);
                        }
                        
                        // Generate the appropriate dialog to show:
                        if ( template != null ) {   
                            // template exits, just show select to confirm
                            dialog = new SelectTemplateDialog(this);
                        } else {
                            // No template: generate a new one and create the EDIT popup
                            template = templateCfg.createTemplate(rootName, rootName,
                                    document.getSourceDocument().getElementsEncountered(), true);
                            dialog = new EditTemplateDialog(this);
                            this.showNewTemplateHelp( document.getDocumentName() );
                            newTemplate = true;
                        }
                        
                        // show the dialog and import document if ok was clicked
                        dialog.setup(document, template.getGuid() );
                        dialog.setModal(true);
                        dialog.setVisible(true);
                        if (dialog.wasOkClicked()) {
                            
                            // Get the final template that was configured by the dialog
                            // and bind it to the document
                            String guid = dialog.getTemplateGuid();
                            template = TemplateConfigManager.getInstance().getTemplate(
                                ConfigType.SESSION, guid);
                            
                            // save it off as the last used template. This allows
                            // it to be reused when adding the next document
                            this.priorTemplate = template;
                            
                            // parse and add it to the session
                            factory.reparseDocument(document, template);
                            this.session.addExistingDocument(document, this);
                            this.addingDocumentProgressDialog.showDialog();
                            
                        } else {
                            // add was canceled. If there was a new template, remove it too
                            if ( newTemplate ) {
                                templateCfg.remove(template);
                            }
                        }
                    }
                }
            } catch (ReportedException ex) {
                ErrorHandler.handleException(ex);
            }
        }
    }

    /**
     * Helper to show an info messgae about the new template setup process.
     * This should be used when a new document is added and a new template
     * was created to match its structure
     * 
     * @param docName
     */
    void showNewTemplateHelp(final String docName) {
        String msg = "No existing template matched the structure of \"" + docName + "\", \n"
                + "so a new template was auto-generated.\n\n"
                + "Update the default settings and click save when finished.";
        JOptionPane.showMessageDialog(this, msg, "New Template", JOptionPane.INFORMATION_MESSAGE);
    }

    // Displays the Export Annotations dialog
    private void openExportAnnotationsDialog() {
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle("Export Annotations");
        ExtensionGroupFilter fileFilter = new ExtensionGroupFilter("HTML Files", true);
        fileFilter.addExtension("html");
        dialog.setFileFilter(fileFilter);

        File myDir = Juxta.selectStartDirectory(false);
        dialog.setCurrentDirectory(myDir);

        int returnVal = dialog.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = dialog.getSelectedFile();

            // add HTML extension if necessary
            selectedFile = FileUtilities.appendFileExtension(selectedFile, "html");

            // if the user doesn't want to overwrite the existing file, abort.
            if (selectedFile.exists() && overwriteFilePrompt(selectedFile.getName()) == false)
                return;

            try {
                AnnotationExportRunner annotationExportRunner = new AnnotationExportRunner(this.session, selectedFile);
                annotationExportRunner.start();
            } catch (NullPointerException e) {
                ErrorHandler.handleException(e);
                JOptionPane.showMessageDialog(null, "No document was selected, "
                        + "therefore the report could not be generated at " + selectedFile.getName(), "Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Displays the Export dialog
    private void openExportDialog() {
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle("Generate Critical Apparatus");
        ExtensionGroupFilter fileFilter = new ExtensionGroupFilter("HTML Files", true);
        fileFilter.addExtension("html");
        dialog.setFileFilter(fileFilter);

        File myDir = Juxta.selectStartDirectory(false);
        dialog.setCurrentDirectory(myDir);

        int returnVal = dialog.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = dialog.getSelectedFile();

            // add HTML extension if necessary
            selectedFile = FileUtilities.appendFileExtension(selectedFile, "html");

            // if the user doesn't want to overwrite the existing file, abort.
            if (selectedFile.exists() && overwriteFilePrompt(selectedFile.getName()) == false)
                return;

            try {
                Collation currentCollation = this.session.getCurrentCollation();
                DocumentManager documentManager = this.session.getDocumentManager();
                JuxtaDocument baseDocument = (JuxtaDocument) documentManager.lookupDocument(currentCollation
                        .getBaseDocumentID());
                String title = (String) JOptionPane
                        .showInputDialog(
                                this,
                                "Enter your preferred title for the critical apparatus report, or click cancel to use the default title: ",
                                "Enter Report Title", JOptionPane.QUESTION_MESSAGE, null, null, baseDocument
                                        .getBiblioData().getTitle() + " Collation");
                SimpleLogger.logInfo("number of differences: " + currentCollation.getNumberOfDifferences());
                CriticalApparatusRunner criticalApparatusRunner = new CriticalApparatusRunner(this.session,
                        selectedFile, title);
                ProgressDialogRunner pdr = new ProgressDialogRunner(progressDialog, criticalApparatusRunner);
                pdr.start();
            } catch (NullPointerException e) {
                ErrorHandler.handleException(e);
                JOptionPane.showMessageDialog(null, "No document was selected, "
                        + "therefore the report could not be generated at " + selectedFile.getName(), "Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Displays the search dialog.
    private void openSearchDialog() {
        searchDialog.display();
    }

    // Displays the Save yes/no dialog.
    private boolean openSavePromptDialog() {
        if (this.session != null && this.session.isModified()) {
            String prompt = "";
            if (this.session.getSaveFile() != null) {
                if ( this.session.wasExported()) {
                    prompt = "Your changes have been exported to the\n" +
                    		"Juxta Web Service but not saved locally.\n\n";
                }
                prompt += "Save changes to " + this.session.getSaveFile().getName() + "?";
            } else {
                if ( this.session.wasExported()) {
                    prompt = "Your comparison set has been exported to the Juxta Web Service\n" +
                    		"but has not been saved locally.\n\nWould you like to save?";
                } else {
                    prompt = "Save changes to new comparison set?";
                }
            }

            Object[] options = { "Yes", "No", "Cancel" };

            int n = JOptionPane.showOptionDialog(this, prompt, "Save changes?", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            switch (n) {
                case 0:
                    return save(this.session.getSaveFile());
                case 1:
                    return true;
                case 2:
                    return false;
                default:
                    return false;
            }
        }

        return true;
    }

    private void initMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        final JMenu menuFile = new JMenu("File");
        menuBar.add(menuFile);

        final JMenuItem menuItem_1 = new JMenuItem();
        menuItem_1.setAction(newAction);
        menuFile.add(menuItem_1);
        menuItem_1.setText("New Comparison Set");

        final JMenuItem menuFileOpen = new JMenuItem();
        menuFileOpen.setAction(fileOpenAction);
        menuFile.add(menuFileOpen);
        menuFileOpen.setText("Open Comparison Set...");

        final JMenuItem menuFileClose = new JMenuItem();
        menuFileClose.setAction(fileCloseAction);
        menuFile.add(menuFileClose);
        menuFileClose.setText("Close Comparison Set");

        final JMenuItem menuFileOpenSample = new JMenuItem();
        menuFileOpenSample.setAction(fileOpenSampleAction);
        menuFile.add(menuFileOpenSample);
        menuFileOpenSample.setText("Open Sample...");

        final JMenuItem menuFileSave = new JMenuItem();
        menuFileSave.setAction(fileSaveAction);
        menuFile.add(menuFileSave);
        menuFileSave.setText("Save Comparison Set");

        final JMenuItem menuItem_2 = new JMenuItem();
        menuItem_2.setAction(fileSaveAsAction);
        menuFile.add(menuItem_2);
        menuItem_2.setText("Save Comparison Set As...");
        
        final JMenuItem wsExportItem = new JMenuItem();
        wsExportItem.setAction(this.wsExportAction);
        menuFile.add(wsExportItem);
        wsExportItem.setText("Export to Juxta Web Service...");

        final JMenuItem menuItemScreenshot = new JMenuItem();
        menuItemScreenshot.setAction(screenshotAction);
        menuFile.add(menuItemScreenshot);
        menuItemScreenshot.setText("Take Screenshot...");

        final JMenuItem menuItemExport = new JMenuItem();
        menuItemExport.setAction(exportAction);
        menuFile.add(menuItemExport);
        menuItemExport.setText("Export Source Document...");

        final JMenuItem menuItemAnnotationExport = new JMenuItem();
        menuItemAnnotationExport.setAction(exportOrpanedAnnotationsAction);
        menuFile.add(menuItemAnnotationExport);
        menuItemAnnotationExport.setText("Export Orphaned Annotations...");

        menuFile.addSeparator();

        final JMenuItem exportMenuItem = new JMenuItem();
        exportMenuItem.setAction(this.exportTextualApparatusAction);
        menuFile.add(exportMenuItem);
        exportMenuItem.setText("Generate Critical Apparatus...");

        if (!MAC_OS_X) {
            menuFile.addSeparator();

            final JMenuItem menuFileExit = new JMenuItem();
            menuFileExit.setAction(exitAction);
            menuFile.add(menuFileExit);
            menuFileExit.setText("Exit");
        }

        final JMenu menuEdit = new JMenu();
        menuBar.add(menuEdit);
        menuEdit.setText("Edit");

        final JMenuItem addDocumentItem = new JMenuItem();
        menuEdit.add(addDocumentItem);
        addDocumentItem.setAction(addDocumentAction);
        addDocumentItem.setText("Add Document...");

        final JMenuItem menuItem = new JMenuItem();

        menuItem.setAction(removeDocumentAction);
        menuEdit.add(menuItem);
        menuItem.setText("Remove Document");

        final JMenuItem editDocumentNameItem = new JMenuItem();
        menuEdit.add(editDocumentNameItem);
        editDocumentNameItem.setAction(propertiesAction);
        editDocumentNameItem.setText("Edit Document Properties");
        
        final JMenuItem editTemplatesItem = new JMenuItem();
        menuEdit.add(editTemplatesItem);
        editTemplatesItem.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                ListTemplatesDialog dlg = new ListTemplatesDialog(JuxtaAuthorFrame.this);
                dlg.setModal(true);
                dlg.setVisible(true); 
                if ( dlg.hasTemplateChanges() ) {
                    try {
                        session.markAsModified();
                        for ( ParseTemplate t : dlg.getModifiedTemplates() ) {
                            session.reparseDocuments( t );
                        }
                        session.refreshComparisonSet();
                        int baseDocID = session.getCurrentCollation().getBaseDocumentID();
                        collationViewPanel.setDocument(session.getDocumentManager().lookupDocument(baseDocID));
                    } catch (ReportedException ex) {
                        ErrorHandler.handleException(ex);
                    } catch (LoggedException ex) {
                        ErrorHandler.handleException(ex);
                    }
                }
            }
            
        });
        editTemplatesItem.setText("Edit XML Parsing Templates...");

        menuEdit.addSeparator();

        final JMenuItem searchItem = new JMenuItem();
        searchItem.setAction(searchAction);
        menuEdit.add(searchItem);
        searchItem.setText("Search...");

        final JMenu menuView = new JMenu();
        menuBar.add(menuView);
        menuView.setText("View");

        final JMenuItem menuViewHistogram = new JMenuItem();
        menuView.add(menuViewHistogram);
        menuViewHistogram.setAction(histogramAction);
        menuViewHistogram.setText("Histogram Graph");

        final JMenuItem menuLocationMarks = new JMenuItem();
        menuView.add(menuLocationMarks);
        menuLocationMarks.setAction(toggleLocationMarkAction);
        menuLocationMarks.setText("Toggle Location Marks");

        final JMenuItem menuItemFont = new JMenuItem();
        menuItemFont.setAction(fontPickerAction);
        menuView.add(menuItemFont);
        menuItemFont.setText("Change text font...");

        final JMenuItem menuItemCenterPanel = new JMenuItem();
        menuItemCenterPanel.setAction(new HideCenterPanelAction(this, menuItemCenterPanel));
        menuView.add(menuItemCenterPanel);
        menuItemCenterPanel.setText("Hide Comparison Lines");

        final JMenu menuCollation = new JMenu();
        menuBar.add(menuCollation);
        menuCollation.setText("Collation");

        final JMenuItem collateItem = new JMenuItem();
        menuCollation.add(collateItem);
        collateItem.setAction(collateAction);
        collateItem.setText("Collate...");

        final JMenu menuHelp = new JMenu("Help");
        menuBar.add(menuHelp);

        final JMenuItem helpManual = new JMenuItem();
        helpManual.setAction(helpAction);
        helpManual.setText("Help...");
        menuHelp.add(helpManual);

        final JMenuItem helpAbout = new JMenuItem();
        helpAbout.setAction(aboutAction);
        helpAbout.setText("About...");
        menuHelp.add(helpAbout);
    }

    private void initToolBar() {
       
        saveButton = new JButton(fileSaveAction);
        this.wsExportButton = new JButton( this.wsExportAction );
        this.wsExportButton.setEnabled(true);

        // Designer code
        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        toggleLocationMarkButton.setAction(toggleLocationMarkAction);

        toolBar.add(fileOpenAction);
        toolBar.add(saveButton);
        toolBar.add(wsExportButton);
        toolBar.addSeparator();
        
        toolBar.add(addDocumentAction);
        toolBar.add(removeDocumentAction);
        toolBar.add(propertiesAction);
        
        toolBar.addSeparator();
        toolBar.add(histogramAction);
        
        toolBar.addSeparator();
        toolBar.add(exportTextualApparatusAction);
        toolBar.add(toggleLocationMarkButton);
        
        toolBar.addSeparator();
        toolBar.add(requiredCollateAction);
        
        toolBar.addSeparator();
        toolBar.add(comboSearchText);
        toolBar.add(toolbarSearchAction);

        comboSearchText.setEditable(true);
        comboSearchText.setMaximumSize(new Dimension(275, 600));
        comboSearchText.setToolTipText(SEARCH_PROMPT);
        comboSearchText.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doSearch();
                }
            }
        });
        comboSearchText.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent fe) {
                // get the text using getText() method
            }

            public void focusLost(FocusEvent fe) {
                // get the text using getText() method
            }
        });

    }

    // Called once during constructing to initialize the user interface of this
    // frame.
    private void initUI() {
        
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Rectangle bounds = new Rectangle();
        bounds.width = Math.min(1024, screenSize.width);
        bounds.height = Math.min(768, screenSize.height);
        bounds.x = (screenSize.width - bounds.width) / 2;
        bounds.y = (screenSize.height - bounds.height) / 2;
        setBounds(bounds);
        
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("juxta");

        // setIconImage(JUXTA_ICON.getImage());

        initMenuBar();
        initToolBar();
        initStatusBar();
        initContentArea();
        initSystemExit();
    }

    private void initStatusBar() {
        // Init status bar
        statusBar = new StatusBar();
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private void initSystemExit() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                SimpleLogger.logInfo("Handling system quit action");
                exit();
            }
        });
    }

    private void initContentArea() {
        final JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(false);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        splitPane.setDividerLocation(300);

        initMainPanel(splitPane);
        initLeftPanel(splitPane);
    }

    private JTabbedPane initSecondaryPanel() {
        // create the image display panel
        imageDisplay = new ImageDisplay(getCollationView(), getCollationViewScroller() );
        imageDisplay.setTitleBackgroundColor(TITLE_BACKGROUND_COLOR);
        imageDisplay.setTitleFont(TITLE_FONT);
        imageDisplay.setImageBackgroundColor(Color.WHITE);

        // create the difference display panel
        annotationPanel = new AnnotationPanel(this);

        // create the block display panel
        movesPanel = new MovesPanel(this);

        // create the search panel
        searchResultsPanel = new SearchResultsPanel(this);

        secondaryViewTabPane = new JTabbedPane();
        secondaryViewTabPane.setTabPlacement(SwingConstants.BOTTOM);
        secondaryViewTabPane.setFont(NORMAL_FONT);
        secondaryViewTabPane.addTab("Source", null, viewSourcePanel, null);
        secondaryViewTabPane.addTab("Images", null, imageDisplay, null);
        secondaryViewTabPane.addTab("Notes", null, annotationPanel, null);
        secondaryViewTabPane.addTab("Moves", null, movesPanel, null);
        secondaryViewTabPane.addTab("Search", null, searchResultsPanel, null);
        secondaryViewTabPane.setToolTipTextAt(SOURCE_TAB, "View document source");
        secondaryViewTabPane.setToolTipTextAt(IMAGES_TAB, "View associated images");
        secondaryViewTabPane.setToolTipTextAt(NOTES_TAB, "View annotations");
        secondaryViewTabPane.setToolTipTextAt(MOVE_TAB, "View moves");
        secondaryViewTabPane.setToolTipTextAt(SEARCH_TAB, "View search results");

        secondaryViewTabPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (secondaryViewTabPane.getSelectedIndex() == IMAGES_TAB) {
                    imageDisplay.setEnabled(true);
                    imageDisplay.recalculateImage(false);
                } else {
                    imageDisplay.setEnabled(false);
                }
                if (secondaryViewTabPane.getSelectedIndex() != MOVE_TAB) {
                    getCollationView().clearRangeSelection();
                    switchMainView(MAIN_VIEW_TAB_PANE);
                }
            }
        });

        return secondaryViewTabPane;
    }

    public void switchMainView(String destinationView) {
        mainViewCardLayout.show(mainViewCardPanel, destinationView);
    }

    public void switchLeftView(int tabIndex) {
        this.leftViewTabPane.setSelectedIndex(tabIndex);
    }

    private void initMainPanel(JSplitPane splitPane) {
        JTabbedPane secondaryPanel = initSecondaryPanel();
        this.documentViewer = new DocumentViewer( this );

        docCompareView = new DocumentCompareView(this);

        // set up the tabs between the collation views
        mainViewTabPane = new JTabbedPane();
        mainViewTabPane.setTabPlacement(SwingConstants.BOTTOM);
        mainViewTabPane.setFont(NORMAL_FONT);
        mainViewTabPane.addTab("", ONE_TO_MANY_VIEW, collationViewPanel, null);
        mainViewTabPane.addTab("", ONE_TO_ONE_VIEW, docCompareView, null);
        mainViewTabPane.setToolTipTextAt(0, "Collation View");
        mainViewTabPane.setToolTipTextAt(1, "Comparison View");

        mainViewTabPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleViewModeSwitch();
            }
        });

        // card layout allows us to switch between the collation/comparison view
        // and the file browser
        mainViewCardPanel = new JPanel();
        mainViewCardLayout = new CardLayout();
        mainViewCardPanel.setLayout(mainViewCardLayout);
        mainViewCardPanel.add(this.documentViewer, DOCUMENT_VIEWER);
        mainViewCardPanel.add(mainViewTabPane, MAIN_VIEW_TAB_PANE);
        switchMainView(MAIN_VIEW_TAB_PANE);

        // set up the splitter between the main view and second view
        textImageSplit = new JSplitPane();
        textImageSplit.setOneTouchExpandable(false);
        textImageSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        textImageSplit.setTopComponent(mainViewCardPanel);
        textImageSplit.setBottomComponent(secondaryPanel);
        textImageSplit.setDividerLocation(300);

        // associate with top level splitter
        splitPane.setRightComponent(textImageSplit);
    }

    private void handleViewModeSwitch() {
        // carry over the document that is currently being viewed into the next
        // view.
        if (getViewMode() == VIEW_MODE_COLLATION) {
             if (histogramDialog.isVisible()) {
                histogramDialog.currentCollationChanged(this.session.getCurrentCollation());
            }

        } else {
            // going to comparison
            docCompareView.setLocation(null, getCollationView().getCurrentText(), docCompareView.getWitnessDocument());
            if (histogramDialog.isVisible()) {
                histogramDialog.selectedDocumentsChanged(getCollationView().getCurrentText(),
                        docCompareView.getWitnessDocument());
            }
        }
    }

    private JPanel initFileTreePanel() {
        FileTreePanel fileTree = new FileTreePanel(Juxta.selectStartDirectory(true));
        fileTree.addListener(this);

        PanelTitle fileTreePanelTitle = new FileTreeTitle(this, fileTree);
        fileTreePanelTitle.setFont(TITLE_FONT);
        fileTreePanelTitle.setTitleText("Local Texts");
        fileTreePanelTitle.setBackground(TITLE_BACKGROUND_COLOR);

        JPanel fileTreePanel = new JPanel();
        fileTreePanel.setLayout(new BorderLayout());
        fileTreePanel.add(fileTreePanelTitle, BorderLayout.NORTH);
        fileTreePanel.add(fileTree, BorderLayout.CENTER);

        return fileTreePanel;
    }

    private void initLeftPanel(JSplitPane splitPane) {
        final JPanel comparisonSetPanel = new JPanel();
        comparisonSetPanel.setLayout(new BorderLayout());
        comparisonSetTitlePanel = new PanelTitle();
        comparisonSetTitlePanel.setFont(TITLE_FONT);
        comparisonSetTitlePanel.setTitleText("Comparison Set");
        comparisonSetTitlePanel.setBackground(TITLE_BACKGROUND_COLOR);

        final JButton sortButton = new JButton(SORT_ICON);
        // This next line is here for the following case:
        // If you click on the button to raise the menu, the standard behavior
        // would be to
        // click on that button again to close the menu. However, in the Java
        // Swing JPopUpMenu
        // behavior, that second click on the button closes the menu, and then
        // the click
        // propagates to the button which causes it to click again. If you pass
        // this flag to
        // the UIManager, it will destroy (supposedly) those closing clicks
        // without letting them
        // cause any other actions. This means that if you click on the button
        // again the
        // desired behavior occurs. This behavior seems intuitive if you click
        // on other UI elements
        // instead of the button again as well.
        UIManager.put("PopupMenu.consumeEventOnClose", Boolean.TRUE);
        comparisonSetTitlePanel.add(sortButton, BorderLayout.EAST);
        sortButton.addActionListener(new SortButtonActionListener(this, comparisonSetPanel, sortButton));

        comparisonSetPanel.add(comparisonSetTitlePanel, BorderLayout.NORTH);
        JScrollPane comparisonSetScrollPanel = new JScrollPane();
        comparisonSetScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        comparisonExplorer = new ComparisonExplorer(this);
        comparisonSetScrollPanel.setViewportView(comparisonExplorer);
        comparisonSetPanel.add(comparisonSetScrollPanel);

        JPanel fileTreePanel = initFileTreePanel();

        leftViewTabPane = new JTabbedPane();
        leftViewTabPane.setTabPlacement(SwingConstants.BOTTOM);
        leftViewTabPane.setFont(NORMAL_FONT);
        leftViewTabPane.addTab("Comparison", null, comparisonSetPanel, null);
        leftViewTabPane.addTab("Files", null, fileTreePanel, null);
        leftViewTabPane.setToolTipTextAt(0, "Comparison Set");
        leftViewTabPane.setToolTipTextAt(1, "Local Texts");
        splitPane.setLeftComponent(leftViewTabPane);
    }

    /**
     * Only for use with collation view
     * 
     * @return
     */
    public JuxtaDocument getCurrentDocument() {
        return getCollationView().getCurrentText();
    }

    public void setBaseDocument(JuxtaDocument document) {
        getCollationView().setJuxtaDocument(document);
        docCompareView.setLocation(null, document, null);
        switchMainView(MAIN_VIEW_TAB_PANE);
    }

    public void setViewmode(int viewMode) {
        if ( getViewMode() == viewMode) {
            return;
        }
        if (viewMode == VIEW_MODE_COLLATION) {
            mainViewTabPane.setSelectedIndex(0);
        } else if (viewMode == VIEW_MODE_COMPARISON) {
            mainViewTabPane.setSelectedIndex(1);
        }
    }

    /**
     * Selects the specified difference and document in the main document window.
     * 
     * @param currentText
     *            The document to display.
     * @param difference
     *            The difference to display within that document.
     */
    public void setLocation(Difference difference) {
        if (difference == null || this.session == null) {
            return;
        }

        DocumentManager  documentManager = this.session.getDocumentManager();
        if (documentManager == null) {
            return;
        }

        JuxtaDocument baseDocument = documentManager.lookupDocument(difference.getBaseDocumentID());
        JuxtaDocument witnessDocument = documentManager.lookupDocument(difference.getWitnessDocumentID());
        if (baseDocument == null || witnessDocument == null) {
            return;
        }

        try {
            if ((this.session.getCurrentCollation() == null)
                    || (this.session.getCurrentCollation().getBaseDocumentID() != baseDocument.getID())) {
                this.session.setBaseText(baseDocument);
                getCollationView().setLocation(difference);
            }

            // select the difference on the currently displayed view
            if (getViewMode() == VIEW_MODE_COLLATION)
                getCollationView().setLocation(difference);
            else {
                docCompareView.setLocation(difference, baseDocument, witnessDocument);
            }
            switchMainView(MAIN_VIEW_TAB_PANE);
        } catch (ReportedException e) {
            ErrorHandler.handleException(e);
        }

    }

    public void setLocation(final int view, final int idBase, final int ofsBase, final int lenBase, 
        final int idWitness, final int ofsWitness, final int lenWitness) {
        
        setViewmode(view);
       
        SwingUtilities.invokeLater( new Runnable() {       
            public void run() {
                Difference difference = new Difference(idBase, idWitness, Difference.NONE);
                if ( lenBase > -1 ) {
                    difference.setBaseOffset(ofsBase);
                    difference.setBaseTextLength(lenBase);
                }
                if ( lenWitness > -1 ) {
                    difference.setWitnessOffset(ofsWitness);
                    difference.setWitnessTextLength(lenWitness);
                }
                setLocation(difference);
            }
        });
    }

    public int getViewMode() {
        if (mainViewTabPane.getSelectedIndex() == 0)
            return VIEW_MODE_COLLATION;
        else
            return VIEW_MODE_COMPARISON;
    }

    public int getSecondaryPanelMode() {
        return secondaryViewTabPane.getSelectedIndex();
    }

    public void makeSearchPaneVisible() {
        secondaryViewTabPane.setSelectedIndex(SEARCH_TAB);
    }

    public void makeMovesPaneVisible() {
        secondaryViewTabPane.setSelectedIndex(MOVE_TAB);
    }

    public void makeSourcePaneVisible( JuxtaDocument activeDoc, int offset, Space space ) {
        secondaryViewTabPane.setSelectedIndex(SOURCE_TAB);
        if ( this.viewSourcePanel.getDocument().getID() != activeDoc.getID()) {
            this.viewSourcePanel.documentChanged( activeDoc );
        }
        this.viewSourcePanel.textClicked(offset, space, true );
    }
    
    public void makeSourcePaneVisible( JuxtaDocument activeDoc, int activeOffset) {
        secondaryViewTabPane.setSelectedIndex(SOURCE_TAB);
        if ( this.viewSourcePanel.getDocument().getID() != activeDoc.getID()) {
            this.viewSourcePanel.documentChanged( activeDoc );
        }
        this.viewSourcePanel.textClicked(activeOffset);
    }

    public void currentCollationChanged(Collation collation) {
        int baseID = collation.getBaseDocumentID();
        JuxtaDocument doc = this.session.getDocumentManager().lookupDocument(baseID);
        this.collationViewPanel.setDocument(doc);//.refreshHeatMap();
    }

    private class FileOpenAction extends AbstractAction {
        public FileOpenAction() {
            super("Open", FILE_OPEN);
            putValue(SHORT_DESCRIPTION, "Open comparison set");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            if (openSavePromptDialog()) {
                openFileDialog(false);
            }
        }

    }

    private class FileCloseAction extends AbstractAction {
        JuxtaAuthorFrame frame;

        public FileCloseAction(JuxtaAuthorFrame frame) {
            super("Close");
            this.frame = frame;
            putValue(SHORT_DESCRIPTION, "Close comparison set");
        }

        public void actionPerformed(ActionEvent e) {
            // if( openSavePromptDialog() )
            frame.close();
        }
    }

    private class FileOpenSampleAction extends AbstractAction {
        public FileOpenSampleAction() {
            super("Open Samples", FILE_OPEN);
            putValue(SHORT_DESCRIPTION, "Open a sample comparison set");
        }

        public void actionPerformed(ActionEvent e) {
            if (openSavePromptDialog()) {
                openFileDialog(true);
            }
        }

    }

    private class ExportTextualApparatusAction extends AbstractAction {
        public ExportTextualApparatusAction() {
            super("Generate", GLOBE);
            putValue(SHORT_DESCRIPTION, "Generate critical apparatus");
        }

        public void actionPerformed(ActionEvent e) {
            openExportDialog();
        }
    }

    private class CollateAction extends AbstractAction {
        public CollateAction() {
            super("Collate", REFRESH);
            putValue(SHORT_DESCRIPTION, "Collate documents");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        }

        public void actionPerformed(ActionEvent e) {
            openCollationDialog();
        }

    }

    private class RequiredCollateAction extends CollateAction {
        // RequiredCollateAction exists so that when it is disabled
        // the user can still re-collate even when a collation isn't
        // "needed" according to juxta. For example, wanting to change
        // filters
        public RequiredCollateAction() {
            super();
        }

    }

    private class FileSaveAction extends AbstractAction {
        public FileSaveAction() {
            super("", FILE_SAVE);
            putValue(SHORT_DESCRIPTION, "Save Comparison Set");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            save(session.getSaveFile());
        }

    }

    private class FileSaveAsAction extends AbstractAction {
        public FileSaveAsAction() {
            super("", FILE_SAVE);
            putValue(SHORT_DESCRIPTION, "Save As...");
        }

        public void actionPerformed(ActionEvent e) {
            save(null);
        }

    }

    private class EditPropertiesAction extends AbstractAction {
        private JuxtaAuthorFrame frame;
        public EditPropertiesAction(JuxtaAuthorFrame juxtaAuthorFrame) {
            super("Edit Base Document Properties", BIBLIO_DATA);
            putValue(SHORT_DESCRIPTION, "Edit Base Document Properties");
            this.frame = juxtaAuthorFrame;
        }

        public void actionPerformed(ActionEvent e) {
            if (session.getCurrentCollation() == null)
                return;

            int baseDocID = session.getCurrentCollation().getBaseDocumentID();
            JuxtaDocument document = session.getDocumentManager().lookupDocument(baseDocID);
            PropertiesDialog dialog = new PropertiesDialog(document, JuxtaAuthorFrame.this);
            dialog.setVisible(true);

            if (dialog.isOk()) {
                document.setBiblioData(dialog.getBiblioData());
                comparisonExplorer.maybeReorderTable();
                session.markAsModified();

                if ( dialog.wasTemplateChanged() ) {
                    ParseTemplate template = TemplateConfigManager.getInstance().getTemplate(
                        ConfigType.SESSION, document.getParseTemplateGuid());
                    
                    try {
                        // reparse all documents that use this template
                        session.reparseDocuments( template );
                        
                        // warn and offer chance to re-edit if this change resulted in
                        // no text in the document
                        if (document.getDocumentText().length() == 0) {
                            Object[] options = { "Edit Template", "Continue" };
                            int opt = JOptionPane.showOptionDialog(frame,
                                "The current parsing template filtered out all text!"
                                    + "\nClick 'Edit Template' to modify the current parsing template"
                                    + " or 'Continue' to ignore this message.", "No Text",
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                                options[0]);
                            if (opt == 0) {
                                actionPerformed(e);
                                return;
                            }
                        }
                        
                        this.frame.session.refreshComparisonSet();
                        this.frame.collationViewPanel.setDocument(document);
                        this.frame.session.markAsModified();

                    } catch (LoggedException ex) {
                        JOptionPane.showMessageDialog(null,
                            "There was an error processing your template changes!/n/n     " + ex.toString(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private class ExportOrphanedAnnotationsAction extends AbstractAction {
        public ExportOrphanedAnnotationsAction() {
            super("Export Annotations...", GLOBE);
            putValue(SHORT_DESCRIPTION, "Export Annotations...");
        }

        public void actionPerformed(ActionEvent e) {
            if (session.getCurrentCollation() != null) {
                openExportAnnotationsDialog();
            }
        }
    }
    
    /**
     * Action to export current session to the juxta web service
     * @author loufoster
     *
     */
    private static final class WebServiceExportAction extends AbstractAction {
        private JuxtaAuthorFrame frame;
        public WebServiceExportAction( JuxtaAuthorFrame frame) {
            super("", WS_EXPORT);
            putValue(SHORT_DESCRIPTION, "Export to Juxta Web Service...");
            this.frame = frame;
        }
        
        public void actionPerformed(ActionEvent arg0) {
            if ( this.frame.hasLoggedOnWeb() ) {
                WebServiceExportDialog dlg = new  WebServiceExportDialog(this.frame);
                dlg.setModal(true);
                dlg.setVisible(true);
            } else {
                LoginDialog dlg = new LoginDialog(this.frame);
                dlg.setModal(true);
                dlg.setVisible(true);
            }
        }
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super("Export Source Document...");
            putValue(SHORT_DESCRIPTION, "Export Source Document...");
        }

        public void actionPerformed(ActionEvent e) {
            if (session.getCurrentCollation() == null)
                return;

            int baseDocID = session.getCurrentCollation().getBaseDocumentID();
            JuxtaDocument document = session.getDocumentManager().lookupDocument(baseDocID);

            File saveFile = openSaveDialog("", "Source document");
            File sourceFile = new File(JuxtaSessionFile.JUXTA_TEMP_DIRECTORY + "/"
                    + JuxtaSessionFile.JUXTA_DOCUMENT_DIRECTORY + JuxtaSessionFile.JUXTA_SOURCE_DOCUMENT_DIRECTORY
                    + document.getSourceDocument().getFileName());

            try {
                // copy the source file to be where we want it right away, we'll
                // just read it from there
                FileUtilities.copyFile(sourceFile, saveFile, false);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Couldn't save the file: " + ex.getMessage(), "Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private class SortButtonActionListener implements ActionListener {
        private JuxtaAuthorFrame frame;
        private Container p;
        private JButton button;
        private JPopupMenu menu;

        private JCheckBoxMenuItem sortByDateAscendingMI;
        private JCheckBoxMenuItem sortByDateDescendingMI;
        private JCheckBoxMenuItem sortByNameAscendingMI;
        private JCheckBoxMenuItem sortByNameDescendingMI;

        public SortButtonActionListener(JuxtaAuthorFrame frame, Container p, JButton button) {
            this.frame = frame;
            this.p = p;
            this.button = button;
            this.menu = initSortingPopupMenu();
        }

        public void actionPerformed(ActionEvent e) {
            Rectangle bounds = button.getBounds();

            if (menu.isVisible())
                menu.setVisible(false);
            else
                menu.show(p, bounds.x, bounds.y + bounds.height);
        }

        private JPopupMenu initSortingPopupMenu() {
            menu = new JPopupMenu();
            sortByDateAscendingMI = new JCheckBoxMenuItem();
            sortByDateAscendingMI.setAction(new SortAction(frame, this, ComparisonExplorer.SORT_BY_DATE,
                    ComparisonExplorer.SORT_DIRECTION_ASCENDING));
            menu.add(sortByDateAscendingMI);
            sortByDateAscendingMI.setText("Oldest First");

            sortByDateDescendingMI = new JCheckBoxMenuItem();
            sortByDateDescendingMI.setAction(new SortAction(frame, this, ComparisonExplorer.SORT_BY_DATE,
                    ComparisonExplorer.SORT_DIRECTION_DESCENDING));
            sortByDateDescendingMI.setState(true);
            menu.add(sortByDateDescendingMI);
            sortByDateDescendingMI.setText("Newest First");

            sortByNameAscendingMI = new JCheckBoxMenuItem();
            sortByNameAscendingMI.setAction(new SortAction(frame, this, ComparisonExplorer.SORT_BY_NAME,
                    ComparisonExplorer.SORT_DIRECTION_ASCENDING));
            menu.add(sortByNameAscendingMI);
            sortByNameAscendingMI.setText("Alphabetical Order");

            sortByNameDescendingMI = new JCheckBoxMenuItem();
            sortByNameDescendingMI.setAction(new SortAction(frame, this, ComparisonExplorer.SORT_BY_NAME,
                    ComparisonExplorer.SORT_DIRECTION_DESCENDING));
            menu.add(sortByNameDescendingMI);
            sortByNameDescendingMI.setText("Reverse Alphabetical Order");

            return menu;
        }

        public void deselectAllMenuItems() {
            sortByNameAscendingMI.setState(false);
            sortByNameDescendingMI.setState(false);
            sortByDateAscendingMI.setState(false);
            sortByDateDescendingMI.setState(false);
        }
    }

    private class SortAction extends AbstractAction {
        private JuxtaAuthorFrame frame;
        private String sortField;
        private String sortDirection;
        private SortButtonActionListener listener;

        public SortAction(JuxtaAuthorFrame frame, SortButtonActionListener listener, String sortField,
                String sortDirection) {
            super("Sorting", null);

            this.frame = frame;
            this.listener = listener;
            this.sortField = sortField;
            this.sortDirection = sortDirection;
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JCheckBoxMenuItem) {
                listener.deselectAllMenuItems();
                ((JCheckBoxMenuItem) source).setState(true);
            }

            frame.comparisonExplorer.setSort(sortField, sortDirection);
        }
    }

    private class HideCenterPanelAction extends AbstractAction {
        JuxtaAuthorFrame frame;
        JMenuItem menuItem;

        public HideCenterPanelAction(JuxtaAuthorFrame frame, JMenuItem menuItem) {
            super("Toggle display of the comparison lines", null);
            this.frame = frame;
            this.menuItem = menuItem;
        }

        public void actionPerformed(ActionEvent e) {
            boolean isShown = frame.docCompareView.dualTextPanel.toggleCenterStripVisibility();
            if (isShown)
                menuItem.setText("Hide Comparison Lines");
            else
                menuItem.setText("Show Comparison Lines");
        }
    }

    private class ScreenshotAction extends AbstractAction {
        JuxtaAuthorFrame frame;

        public ScreenshotAction(JuxtaAuthorFrame frame) {
            super("Take screenshot", null);
            putValue(SHORT_DESCRIPTION, "Take screenshot");
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Robot robot = new Robot();
                Rectangle bounds = getWorldComponentBounds(frame.mainViewCardPanel);
                BufferedImage image = robot.createScreenCapture(bounds);

                File saveFile = openSaveDialog("png", "PNG Image File");

                // pre-existing file, prompt for overwrite
                if (saveFile != null && saveFile.exists()) {
                    if (overwriteFilePrompt(saveFile.getName()) == false)
                        return;
                }

                if (!ImageIO.write(image, "png", saveFile)) {
                    JOptionPane.showMessageDialog(null, "The screenshot operation failed: could not create PNG file.",
                            "Error", JOptionPane.WARNING_MESSAGE);
                }
            } catch (AWTException ex) {
                ErrorHandler.handleException(ex);
                JOptionPane.showMessageDialog(null, "The screenshot operation failed.", "Error",
                        JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                ErrorHandler.handleException(ex);
                JOptionPane.showMessageDialog(null, "Could not save the image file.", "Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        // Method to find the bounding rectangle for a Container object in
        // "global" space.
        // A given Container will return a bounding rectangle via getBounds()
        // but the
        // location of this rectangle is scoped inside its parent container,
        // whose bounds
        // are scoped inside ITS parent container, and so forth.
        //
        // Iterate up the component tree via getParent(), modifying the bounding
        // rectangle's
        // root location until you get to the Container that has no parent,
        // whose location is
        // rooted in the "global" space.
        private Rectangle getWorldComponentBounds(Container comp) {
            if (comp == null) {
                return null;
            }

            Rectangle result = comp.getBounds();
            Container current = comp.getParent();
            while (current != null) {
                Rectangle bounds = current.getBounds();
                result.setLocation(result.x + bounds.x, result.y + bounds.y);
                current = current.getParent();
            }
            return result;
        }

    }

    private class FontPickerAction extends AbstractAction {
        private JuxtaAuthorFrame frame;

        public FontPickerAction(JuxtaAuthorFrame frame) {
            super("Font chooser", null);
            putValue(SHORT_DESCRIPTION, "Choose a display font");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_MASK));

            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            JFontChooser fontChooser = new JFontChooser();
            int result = fontChooser.showDialog(frame);
            if (result == JFontChooser.OK_OPTION) {
                Font font = fontChooser.getSelectedFont();
                frame.getCollationView().setTextFont(font);
                frame.docCompareView.dualTextPanel.setFont(font);
            }
        }
    }

    private class AboutAction extends AbstractAction {
        public AboutAction() {
            super("About", ABOUT);
            putValue(SHORT_DESCRIPTION, "About Juxta");
        }

        public void actionPerformed(ActionEvent e) {
            AboutDialog dialog = new AboutDialog(JuxtaAuthorFrame.this);
            dialog.setVisible(true);
        }
    }

    private class HelpAction extends AbstractAction {
        public HelpAction() {
            super("Help", HELP);
            putValue(SHORT_DESCRIPTION, "Help");
        }

        public void actionPerformed(ActionEvent e) {
            Browser.init();
            try {
                Browser.displayURL("http://www.juxtasoftware.org/documentation.html");
            } catch (IOException e1) {
                ReportedException re = new ReportedException(e1, "Unable to connect to Juxta help page.");
                ErrorHandler.handleException(re);
            }
        }
    }

    private class ExitAction extends AbstractAction {
        public ExitAction() {
            super("", EXIT);
            putValue(SHORT_DESCRIPTION, "Exit juxta");
        }

        public void actionPerformed(ActionEvent e) {
            exit();
        }
    }

    private class AddDocumentAction extends AbstractAction {
        public AddDocumentAction() {
            super("Add Document", ADD_DOCUMENT);
            putValue(SHORT_DESCRIPTION, "Add Document");
        }

        public void actionPerformed(ActionEvent e) {
            openAddDocumentDialog();
        }

    }

    private class RemoveDocumentAction extends AbstractAction implements DocumentRemoverCallback {
        private JuxtaAuthorFrame frame;
        private ProcessingInProgressDialog dialog;

        public RemoveDocumentAction(JuxtaAuthorFrame frame) {
            super("Remove Document", REMOVE_DOCUMENT);
            putValue(SHORT_DESCRIPTION, "Remove Document");
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            JuxtaDocument document = comparisonExplorer.getSelectedDocument();
            DocumentRemover remover = new DocumentRemover(document, this);
            dialog = new ProcessingInProgressDialog(frame, "Removing document...");
            remover.start();
            dialog.showDialog();
        }

        public void finishedRemovingDocument(JuxtaDocument document) {
            dialog.setVisible(false);
        }

    }

    private class ToggleLocationMarkAction extends AbstractAction {
        public ToggleLocationMarkAction() {
            super("", LINE_NUMBER);
            putValue(SHORT_DESCRIPTION, "Toggle Location Marks");
        }

        public void actionPerformed(ActionEvent e) {
            boolean visible = !getCollationView().isLocationMarkStripVisible();
            getCollationView().showLocationMarkStrip(visible);
            docCompareView.showLocationMarkStrip(visible);
            toggleLocationMarkButton.setSelected(visible);
        }

    }

    private class NewAction extends AbstractAction {
        JuxtaAuthorFrame frame;

        public NewAction(JuxtaAuthorFrame frame) {
            super("New", FILE_NEW);
            putValue(SHORT_DESCRIPTION, "New");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK));
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            // if( openSavePromptDialog() )
            frame.close();
        }
    }

    private class SearchAction extends AbstractAction {
        public SearchAction() {
            super("Back", SEARCH);
            putValue(SHORT_DESCRIPTION, "Search Text");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            openSearchDialog();
        }

    }

    private class ToolbarSearchAction extends AbstractAction {
        public ToolbarSearchAction() {
            super("Back", SEARCH);
            putValue(SHORT_DESCRIPTION, "Search Text In Files");
        }

        public void actionPerformed(ActionEvent e) {
            doSearch();
        }

    }

    private void doSearch() {
        String strEditorItem = (String) comboSearchText.getEditor().getItem();
        String strSelectedItem = (String) comboSearchText.getSelectedItem();
        // String strList = "";
        // for (int i = 0; i < comboSearchText.getItemCount(); ++i)
        // strList += (String)comboSearchText.getItemAt(i) + "/";
        // String str = "Ed: " + (strEditorItem==null?"null":strEditorItem) +
        // " " +
        // "Sel: " + (strSelectedItem==null?"null":strSelectedItem) + " List: "
        // + strList;
        // SimpleLogger.logInfo(str);

        String sel = strEditorItem;
        if (sel == null)
            sel = strSelectedItem;
        if ((sel == null) || (sel.equals("")))
            return;
        comboSearchText.setSelectedItem(sel);
        if (comboSearchText.getSelectedIndex() == -1)
            comboSearchText.addItem(sel);
        searchDialog.addSearchItem(sel);
        handleSearch(new SearchOptions(sel, true, false));
    }

    public void addSearchItem(String str, boolean tellSearchDialog) {
        comboSearchText.setSelectedItem(str);
        if (comboSearchText.getSelectedIndex() == -1)
            comboSearchText.addItem(str);
        if (comboSearchText.getItemCount() > 20)
            comboSearchText.removeItemAt(0);
        if (tellSearchDialog)
            searchDialog.addSearchItem(str);
    }

    private class HistogramAction extends AbstractAction {
        public HistogramAction() {
            super("View Historgram", HISTOGRAM);
            putValue(SHORT_DESCRIPTION, "View Histogram");
        }

        public void actionPerformed(ActionEvent e) {
            openHistogramDialog();
        }
    }

    private void openHistogramDialog() {
        docCompareView.addListener(histogramDialog);
        if (getViewMode() == VIEW_MODE_COLLATION) {
            if (session.getCurrentCollation() != null)
                histogramDialog.display(session.getCurrentCollation());
        } else {
            histogramDialog.display(docCompareView.getBaseDocument(), docCompareView.getWitnessDocument());
        }
    }

    public void documentChanged(JuxtaDocument document) {
        // clear the images cache
        imageDisplay.clearCache();
        imageDisplay.recalculateImage(true);
    }

    /**
     * Figure out what percent of the document is currently visible in the main document window.
     * 
     * @return The percentage expressed as a float.
     */
    public float calculateVisibleDocumentAreaPercentage() {
        if (getCollationView() == null )
            return 0;

        float visibleHeight = getCollationView().getVisibleRect().height;
        float totalHeight = getCollationView().getHeight();
        return visibleHeight / totalHeight;
    }

    /**
     * Repositions the scroll bar of the main document window.
     * 
     * @param position
     */
    public void setScrollPosition(float position) {
        if (getViewMode() == VIEW_MODE_COLLATION) {
            if (getCollationView() == null) {
                return;
            }
            getCollationView().scrollToPercent(position);
        } else {
            if (docCompareView == null)
                return;

            int side = (docCompareView.getScrollMode() == DocumentCompareView.SCROLL_MODE_LINKED) ? DocumentCompareView.RIGHT
                    : DocumentCompareView.LEFT;
            docCompareView.scrollToPosition(position, side);
        }

    }

    /**
     * Repositions the image panel splitter so that the image panel is visible if it is not currently visible.
     */
    public void openImagePanel() {
        textImageSplit.setDividerLocation(250);
    }

    public void setSearchResults(SearchResults searchResults) {
        docCompareView.setSearchResults(searchResults);
        getCollationView().setSearchResults(searchResults);
        searchResultsPanel.setSearchResults(searchResults);
        makeSearchPaneVisible();
    }

    public void setFilterStrength(int strength) {
        session.getCurrentCollation().setMinChangeDistance(strength);
        getCollationView().refreshHeatMap();
        histogramDialog.repaint();
    }

    /**
     * Gets the current scroll position of the main document window.
     * 
     * @return
     */
    public float getScrollPosition() {
        if (getViewMode() == VIEW_MODE_COLLATION) {
            BoundedRangeModel model = getCollationViewScroller().getVerticalScrollBar().getModel();
            float range = model.getMaximum() - model.getMinimum();
            float value = model.getValue() - model.getMinimum();
            if (range > 0f)
                return value / range;
            else
                return 0f;
        } else {
            return docCompareView.getScrollPosition();
        }
    }

    public void documentAdded(JuxtaDocument document) {
    }

    public void sessionModified() {
        if (saveButton != null) {
            saveButton.setEnabled(true);
        }
    }

    public void loadingComplete() {
        if (addingDocumentProgressDialog != null && addingDocumentProgressDialog.isVisible()){
            addingDocumentProgressDialog.setVisible(false);
            
            // try to reset the base document. this will force the collation
            // to be refreshed and the heat map will reflect the newly added doc
            if ( session.getCurrentCollation() != null ) {
                int baseDocID = session.getCurrentCollation().getBaseDocumentID();
                JuxtaDocument document = session.getDocumentManager().lookupDocument(baseDocID);
                try {
                    session.setBaseText(document);
                } catch (ReportedException e) {}
            }
        }

        // if a base document has not yet been selected, select one
        if ( this.session.getCurrentCollation() == null ) {
        
            LinkedList<JuxtaDocument> documentList = session.getDocumentManager().getDocumentList();
            if (documentList.isEmpty() == false) {
                try {
                    this.session.setBaseText( (JuxtaDocument) documentList.getFirst() );
                } catch (ReportedException e) {
                    ErrorHandler.handleException(e);
                }
            }
        }

        // loading is complete, allow selection of collations
        this.comparisonExplorer.setSelectionEnabled(true);
        setCursor(JuxtaUserInterfaceStyle.NORMAL_CURSOR);
    }

    /**
     * Add a listener to the main document window scrollbar.
     * 
     * @param listener
     *            A <code>ChangeListener</code> object.
     */
    public void addScrollListener(ChangeListener listener) {
        if (getCollationViewScroller() == null)
            return;
        getCollationViewScroller().getVerticalScrollBar().getModel().addChangeListener(listener);

        docCompareView.addScrollBarListener(listener);
    }

    public void currentCollationFilterChanged(Collation currentCollation) {
        // refresh the heat map to include/exclude the change
        getCollationView().refreshHeatMap();
    }
    
    public JuxtaSession getSession() {
        return this.session;
    }

    public MovesPanel getMovesPanel() {
        return movesPanel;
    }

    public void fileTreePanelFileSelected(File file) {
        try {
            // open this file as a JuxtaDocument if possible
            JuxtaDocument document = session.getDocumentManager().constructDocument(file.getName(),
                    file.getAbsolutePath(), "UTF-8");

            // if there is a document name specified by the XML, use the name
            // given
            if (document.getDocumentName().equals(""))
                document.setDocumentName(file.getName());

            documentViewer.setDocument(document);
            switchMainView(DOCUMENT_VIEWER);
        } catch (ReportedException e) {
            ErrorHandler.handleException(e);
        }
    }

    public DocumentCompareView getDocumentCompareView() {
        return docCompareView;
    }

    public CollationViewTextArea getCollationView() {
        return this.collationViewPanel.getTextArea();
    }
    
    public JScrollPane getCollationViewScroller() {
        return this.collationViewPanel.getScrollPane();
    }

    public void lockdownUI( JComponent source, boolean locked ) {
        this.comparisonExplorer.setSelectionEnabled( !locked );
        this.mainViewTabPane.setEnabled( !locked );
        this.secondaryViewTabPane.setEnabled( !locked );
        this.leftViewTabPane.setEnabled( !locked );
        
        if ( this.viewSourcePanel.equals(source) ) {
            this.collationViewPanel.setEnabled(!locked);
        } else {
            this.viewSourcePanel.setEnabled( !locked );
        }
        
        // toolbar 
        this.fileOpenAction.setEnabled( !locked );
        this.wsExportButton.setEnabled( !locked  );
        this.saveButton.setEnabled( !locked );
        if ( locked == false ) {
            this.saveButton.setEnabled( this.session.isModified() );
            this.wsExportButton.setEnabled( true );
        }
        this.addDocumentAction.setEnabled( !locked );
        this.removeDocumentAction.setEnabled( !locked );
        this.propertiesAction.setEnabled( !locked );
        this.histogramAction.setEnabled( !locked );
        this.exportTextualApparatusAction.setEnabled( !locked );
        this.toggleLocationMarkButton.setEnabled( !locked );
        this.requiredCollateAction.setEnabled( !locked );
        this.comboSearchText.setEnabled( !locked );
        this.toolbarSearchAction.setEnabled( !locked );
    }

    public void handleSearch(SearchOptions opts) {
        if ( opts.searchAllFiles() ) {
            addSearchItem(opts.getSearchTerm(), false);
            SearchResults results;
            try {
                results = session.getDocumentManager().search(opts.getSearchTerm());
                setSearchResults(results);
            } catch (ReportedException e) {
                ErrorHandler.handleException(e);
            }
        } else {
            int docId = this.session.getCurrentCollation().getBaseDocumentID();
            JuxtaDocument doc = this.session.getDocumentManager().lookupDocument(docId);
            int offset = doc.search(this.lastFindOffset, opts.getSearchTerm(), opts.wrapSearch());
            if ( offset > -1 ) {
                this.lastFindOffset = offset+opts.getSearchTerm().length();
                getCollationView().selectText(offset, opts.getSearchTerm().length());
                this.docCompareView.setLocation(offset, opts.getSearchTerm().length());
            }
        }
    }
    
    public void clearSearch() {
        getCollationView().clearRangeSelection();
        this.docCompareView.searchHighlightRemoval();
    }
}
