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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class FileUtilities 
{
	public static void unzip(File archiveFile, File targetDir) throws ZipException, IOException 
	{
		ZipFile zipFile = new ZipFile(archiveFile);
		
		Enumeration entries = zipFile.entries();

		SimpleLogger.logInfo("unzipping files to temp dir: " + targetDir.getPath());

		while (entries.hasMoreElements())
		{
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();

			InputStream inStream = zipFile.getInputStream(zipEntry);
			SimpleLogger.logInfo("zip entry: " + zipEntry.getName() + " size: "+zipEntry.getSize());
			File destFile = new File(targetDir.getCanonicalPath() + '/'
					+ zipEntry.getName());
			
			if( zipEntry.getSize() > 0 )
			{
				writeFile(inStream, destFile);
				SimpleLogger.logInfo("unzipped file: " + destFile.getPath());
			}
		}

		zipFile.close();
		SimpleLogger.logInfo("unzip complete!");
	}
    
    /**
     * Appends the specified file extension to the file if it doesn't have this extension already. 
     * @param file The file to extend.
     * @param extension The extension to use.
     * @return The new file with the extension or the existing file if it already had one.
     */
    public static File appendFileExtension(File file, String extension)
    {
        if( file.getName().endsWith(extension) == false )
        {
            String selectedFilePath = file.getParent();
            String selectedFileName = file.getName() + "." + extension;
            return new File( selectedFilePath, selectedFileName );
        }    
        else return file;
    }
    
	public static String getFileExtension(String path) {
		int extensionStart = path.lastIndexOf(".");
		if( extensionStart != -1 ) {
			return path.substring(extensionStart);
		} else {
			return "";			
		}
	}
    
    public static String removeFileExtension( String path ) {    	
    	int extensionStart = path.lastIndexOf(".");
    	if( extensionStart != -1 ) {
        	return path.substring(0, extensionStart);    		
    	}
    	else {
    		return path;
    	}
    }
	
	public static void zip( HashMap fileMap, File targetZip ) throws IOException 
	{
        IOException heldException = null;
		ZipOutputStream zipStream = new ZipOutputStream( new FileOutputStream( targetZip ) );
		
		SimpleLogger.logInfo("zipping up archive: "+targetZip.getPath());
		
		for( Iterator i = fileMap.keySet().iterator(); i.hasNext(); )
		{
			String fileName = (String) i.next();
			File file = (File) fileMap.get(fileName);
			ZipEntry entry = new ZipEntry(fileName);
			
            try
            {
                zipStream.putNextEntry(entry);
                SimpleLogger.logInfo("adding file to zip: "+fileName);
                readFile(file,zipStream);           
            } 
            catch (IOException e)
            {
                // hold the exception till we finish writing out the file.
                // (otherwise it will be corrupt)
                heldException = e;
            }			
		}
		
		zipStream.close();
		SimpleLogger.logInfo("zip complete!");
        
        if( heldException != null )
        {
            // something happened writing the file
            throw heldException;
        }
	}

	public static void readFile(File srcFile, OutputStream destStream )	throws IOException 
	{
		FileInputStream srcStream = new FileInputStream(srcFile);
		
		byte[] buf = new byte[65535];
		int bytesRead;
		
		while (true) {
			bytesRead = srcStream.read(buf);
			if (bytesRead == -1) {
				break;
			} else {
				destStream.write(buf, 0, bytesRead);
			}
		}
	}

   public static void copyFile(File srcFile, File destFile, boolean recursive ) throws IOException 
   {
     if( recursive ) {
  	   if( !destFile.isDirectory() ) {
		   throw new IOException("Destination must be a directory.");		   
	   }
  	   copyFileR(srcFile,destFile);
     } else {
    	 copyFile(srcFile,destFile);
     }    	 
   }
   
   private static void copyFileR( File srcFile, File destDirectory ) throws IOException {	   
	   File destFile = new File( destDirectory.getAbsolutePath() + File.separator + srcFile.getName() );
	   if( srcFile.isDirectory() ) {
		   destFile.mkdir();
		   File[] fileList = srcFile.listFiles();
		   for( int i=0; i < fileList.length; i++ ) {
			   File file = fileList[i];
			   copyFileR(file, destFile);
		   }
	   } else {
		   copyFile( srcFile, destFile );
	   }
   }

   private static void copyFile(File srcFile, File destFile ) throws IOException 
   {
      File testFile = destFile.getParentFile();
      if (testFile.exists() == false)
      {
         testFile.mkdirs();
      }
       
      FileInputStream srcStream = null;
      FileOutputStream destStream = null;
     srcStream = new FileInputStream(srcFile);
     destStream = new FileOutputStream(destFile);
      
      byte[] buf = new byte[65535];
      int bytesRead;
      
      while (true)
      {
            bytesRead = srcStream.read(buf);
            if (bytesRead == -1)
            {
               break;
            }
            else
            {
               destStream.write(buf,0,bytesRead);
            }
      }
   }

	public static void writeFile(InputStream srcStream, File destFile)
			throws IOException 
	{
		File testFile = destFile.getParentFile();

		if (!testFile.exists())
			testFile.mkdirs();
		
		FileOutputStream destStream = new FileOutputStream(destFile);

		byte[] buf = new byte[65535];
		int bytesRead;

		while (true) {
			bytesRead = srcStream.read(buf);
			if (bytesRead == -1) {
				break;
			} else {
				destStream.write(buf, 0, bytesRead);
			}
		}
	}
    
    private static boolean deleteR(File dir)
    {
       if( !dir.isDirectory() ) return false;
       
       File listing[] = dir.listFiles();
       
       for( int i=0; i < listing.length; i++)
       {
           File file = listing[i];
           
           if( file.isDirectory() ) deleteR(file);
           else file.delete(); 
       }
       
       return dir.delete();
    }
    
    /**
     * Recursively delete the directory structure from this directory down, 
     * including this directory.
     * @param dir The directory to delete and remove.
     * @param deleteBase set to <code>true</code> to delete the target dir as well.
     * @return <code>true</code> for successful, <code>false</code> otherwise.
     */
    public static boolean recursiveDelete(File dir, boolean deleteBase )
    {
        boolean status = true;
        if( !dir.isDirectory() ) return false;
       
        File listing[] = dir.listFiles();
       
        for( int i=0; i < listing.length; i++)
        {
            File file = listing[i];
           
            if( file.isDirectory() ) status &= deleteR(file);
            else status &= file.delete(); 
        }
        
        if( deleteBase )
            status &= dir.delete();
       
        return status;
    }

}
