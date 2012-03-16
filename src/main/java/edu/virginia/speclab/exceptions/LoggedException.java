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

public class LoggedException extends Exception
{
    String logMessage;
    
    public LoggedException( Exception e )
    {
        super(e);
    }
    
    public LoggedException( String logMessage )
    {
        this.logMessage = logMessage;
    }
    
    public String getLocalizedMessage()
    {
        return getMessage();
    }
    
    public String getMessage()
    {
        if( logMessage == null )     
            return super.getMessage();
        else
            return logMessage;
    }

}
