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
 
package edu.virginia.speclab.exceptions;

import javax.swing.JOptionPane;

import edu.virginia.speclab.util.SimpleLogger;

public class ErrorHandler
{
    private boolean fatalErrorOccurred;    
    private static ErrorHandler singleton;
    
    protected ErrorHandler() {}
    
    public static void initErrorHandler()
    {
        singleton = new ErrorHandler();
    }
    
    public static void handleException( Exception e )
    {
        if( singleton == null ) initErrorHandler();
        
        if( e instanceof FatalException )
        {
            singleton.handleFatalException((FatalException)e);
        }
        else if( e instanceof ReportedException )
        {
            singleton.handleReportedException((ReportedException)e);
        }
        else if( e instanceof LoggedException )
        {
            singleton.handleLoggedException((LoggedException)e);
        }
        else
        {
            singleton.handleUnknownException(e);
        }
    }
    
    private void handleUnknownException( Exception e )
    {
        SimpleLogger.logError( "Unclassified exception: "+e.getLocalizedMessage());
        logStackTrace(e.getStackTrace());    
    }

    private void handleLoggedException( LoggedException e )
    {
        SimpleLogger.logError(e.getLocalizedMessage());
        logStackTrace(e.getStackTrace());
    }
    
    private void logStackTrace( StackTraceElement stackTrace[] )
    {
        StringBuffer messageBuffer = new StringBuffer();
        
        for( int i = 0; i < stackTrace.length; i++ )
        {
            StackTraceElement element = stackTrace[i];            
            messageBuffer.append(element.toString() + "\n");
        }        
        
        SimpleLogger.logError(messageBuffer.toString());
    }
    
    private void handleReportedException( ReportedException e )
    {
        JOptionPane.showMessageDialog(null,e.getUserMessage());
        SimpleLogger.logError(e.getLocalizedMessage());
        logStackTrace(e.getStackTrace());
    }
    
    private void handleFatalException( FatalException e )
    {
        fatalErrorOccurred = true;
        JOptionPane.showMessageDialog(null,e.getUserMessage());
        SimpleLogger.logError(e.getLocalizedMessage());
        logStackTrace(e.getStackTrace());        
    }

    public boolean fatalErrorOccurred()
    {
        return fatalErrorOccurred;
    }
}
