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

package edu.virginia.speclab.juxta.author;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.FatalException;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.template.TemplateConfigManager;
import edu.virginia.speclab.juxta.author.view.JuxtaAuthorFrame;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.util.FileUtilities;
import edu.virginia.speclab.util.OSDetector;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class acts as the application entry point for Juxta. It also performs global initializations for things such as
 * logging, L&F, etc.
 * 
 * @author Nick
 * 
 */
public class Juxta implements JuxtaUserInterfaceStyle {
    // TODO when changing version number, also update isVersionCompatible() function
    public static final String JUXTA_VERSION = "1.7";

    public static final String ORIGINAL_SAMPLE_DIRECTORY = "./sample";
    public static final String MAC_OSX_BASE_DIRECTORY = System.getProperty("user.home")
            + "/Library/Application Support/Juxta";
    public static final String PC_BASE_DIRECTORY = System.getProperty("user.home") + "/Application Data/Juxta";
    public static final String MAC_OSX_SAMPLE_DIRECTORY = MAC_OSX_BASE_DIRECTORY + "/sample";
    public static final String PC_SAMPLE_DIRECTORY = PC_BASE_DIRECTORY + "/sample";

    private static void initLookNFeel() {
        WindowsLookAndFeel winLAF = new WindowsLookAndFeel();

        if (winLAF.isNativeLookAndFeel()) {
            try {
                UIManager.setLookAndFeel(winLAF);
            } catch (UnsupportedLookAndFeelException e) {
                System.out.println("Windows L&F not found. Using default L&F");
            }
        } else if (OSDetector.getOperatingSystem() == OSDetector.MAC) {
            UIManager.getDefaults().put("MenuItem.acceleratorSelectionForeground", Color.BLACK);
            UIManager.getDefaults().put("MenuItem.selectionForeground", Color.BLACK);
            UIManager.getDefaults().put("MenuItem.selectionBackground", SECOND_COLOR_BRIGHTEST);
        }

        // use white as the default viewport color
        UIManager.getDefaults().put("Viewport.background", Color.WHITE);

        // mac property, ignored by other platforms
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    public static File selectStartDirectory(boolean useSampleDirectory) {
        if (useSampleDirectory) {
            if (OSDetector.getOperatingSystem() == OSDetector.MAC) {
                return new File(Juxta.MAC_OSX_SAMPLE_DIRECTORY);
            } else {
                return new File(Juxta.PC_SAMPLE_DIRECTORY);
            }
        } else {
            if (OSDetector.getOperatingSystem() == OSDetector.MAC) {
                // return javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();
                return new File(System.getProperty("user.home") + "/Documents");
            } else {
                return new File(System.getProperty("user.home") + "/My Documents");
            }
        }
    }

    private static void initSampleDirectory() throws IOException {
        File baseDirectory = (OSDetector.getOperatingSystem() == OSDetector.MAC) ? new File(MAC_OSX_BASE_DIRECTORY)
                : new File(PC_BASE_DIRECTORY);
        File originalDirectory = new File(ORIGINAL_SAMPLE_DIRECTORY);
        FileUtilities.copyFile(originalDirectory, baseDirectory, true);
    }

    private static void initApplicationDirectory() throws IOException {

        File baseDir = (OSDetector.getOperatingSystem() == OSDetector.MAC) ? new File(MAC_OSX_BASE_DIRECTORY)
                : new File(PC_BASE_DIRECTORY);

        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new IOException("Unable to create application directory: " + baseDir.getPath());
            }
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        
        // encapsulates execution so it runs on the swing thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                
                String startFile = "";
                String wsUrl = "";
                if (args != null ) {
                    for ( int i=0; i<args.length; i++) {
                        String arg = args[i];
                        if ( arg.contains("url=")) {
                            wsUrl = arg.substring(4);
                        } else {
                            startFile = arg;
                        }
                    }
                }
                
                // Use Xerces for parsing in order to support files
                // which have a Byte Order Marker (BOM)
                System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");


                try {
                    // set up the application support dir
                    initApplicationDirectory();

                    if (OSDetector.getOperatingSystem() == OSDetector.MAC) {
                        SimpleLogger.initFileLogging(MAC_OSX_BASE_DIRECTORY + "/juxta.log");

                    } else {
                        SimpleLogger.initFileLogging(PC_BASE_DIRECTORY + "/juxta.log");
                    }

                    SimpleLogger.setLoggingLevel(DiffAlgorithm.VERBOSE_LOGGING);
                    // SimpleLogger.setSimpleConsoleOutputEnabled(true);
                    // SimpleLogger.setLoggingLevel(10);
                } catch (IOException e1) {
                    ReportedException reportedE = new ReportedException(e1, "Unable to initialize diagnostic logging.");
                    ErrorHandler.handleException(reportedE);
                }

                initLookNFeel();

                try {
                    initSampleDirectory();
                } catch (IOException e1) {
                    ReportedException reportedE = new ReportedException(e1, "Unable to install sample files.");
                    ErrorHandler.handleException(reportedE);
                }
                
                // Initialize the TemplateManager instance with
                // the master template configuration
                try {
                    TemplateConfigManager.getInstance().loadMasterConfig();
                } catch (FatalException e) {
                    ErrorHandler.handleException(e);
                }
                
                // create the main frame and display
                JuxtaAuthorFrame frame;
                try {
                    // pass .jxt file as a parameter for windows users who double click the file
                    frame = new JuxtaAuthorFrame(wsUrl, startFile);              
                    frame.setVisible(true);
                } catch (FatalException e) {
                    ErrorHandler.handleException(e);
                }
            }
        });
    }

    public static boolean isVersionCompatible(String version) {
        return ( version.equals("1.3.1") || version.equals("1.4") || 
                 version.equals("1.4 RC5") || version.equals("1.4 RC7") ||
                 version.equals("1.5") || version.equals("1.6") || 
                 version.equals("1.6.1") || version.equals("1.6.2") || version.equals("1.6.5") || version.equals("1.7"));
    }
}
