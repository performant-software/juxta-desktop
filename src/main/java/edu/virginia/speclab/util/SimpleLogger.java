//
// SimpleLogger
// Utility class to perform simple logging
// Author: Lou Foster
// Date  : 10/01/03
//

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
 

package edu.virginia.speclab.util;

import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public final class SimpleLogger
{
   public static final int MAX_LOGGING_LEVEL = 100;
   private static final Logger logger = Logger.getLogger("SimpleLogger");
   private static boolean simpleOutEnabled = false;
   private static boolean logToConsole = false;
   private static int loggingLevel = MAX_LOGGING_LEVEL;
   
   public static void initConsoleLogging()
   {  
      ConsoleAppender ca = new ConsoleAppender(
         new PatternLayout("%d{E MMM dd, HH:mm:ss} [%p] - %m\n"));
      BasicConfigurator.configure( ca ); 
      logToConsole = true;
   }
   
   public static void initFileLogging(String fileName) throws IOException
   {
      // purge old log on startup
      File logFile = new File(fileName);
      if (logFile.exists())
      {
//         SimpleDateFormat dateFormat = 
//            new SimpleDateFormat ("MMddhhmm");
//         Date date = new Date();
//         String newName = fileName + "-" + dateFormat.format(date);
//         logFile.renameTo(new File(newName));
         logFile.delete();
      }
      
      FileAppender fa = new FileAppender(
         new PatternLayout("%d{E MMM dd, HH:mm:ss} [%p] - %m\n"), fileName);
      BasicConfigurator.configure( fa );
      logToConsole = false;
   }
   
   private SimpleLogger()
   {
   }
   
   public static void logError(String errorMsg)
   {
      logger.error(errorMsg);
      
      if (simpleOutEnabled && logToConsole == false)
      {
         System.err.println(errorMsg);
      }
   }
   
   public static void logInfo( String infoMsg)
   {
      logger.info(infoMsg);
      
      if (simpleOutEnabled && logToConsole == false)
      {
         System.out.println(infoMsg);
      }
   }
   
   public static void logInfo( String infoMsg, int level )
   {
       if( level >= loggingLevel )
       {
           logInfo(infoMsg);
       }
   }

   public static void setSimpleConsoleOutputEnabled(boolean b)
   {
      SimpleLogger.simpleOutEnabled = b;
   }

    public static void setLoggingLevel(int loggingLevel)
    {
        if( loggingLevel < 0 ) loggingLevel = 0;
        if( loggingLevel > MAX_LOGGING_LEVEL ) loggingLevel = MAX_LOGGING_LEVEL;
        
        SimpleLogger.loggingLevel = loggingLevel;
    }

} 
