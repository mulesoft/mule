/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp.server;

import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Ftp client wrapper for working with an FTP server.
 */
public class FTPTestClient
{
    protected final transient Log logger = LogFactory.getLog(this.getClass());
    FTPClient ftpClient = null;   
    String server = null;
    int port;
    String user = null;
    String password = null;
    public static final int TIMEOUT = 5000; // just to make sure we don't hang the test on invalid connection attempts
        
    public FTPTestClient(String server, int port, String user, String password)
    {
        super();
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
        ftpClient = new FTPClient();
    }

    public boolean testConnection() throws IOException
    {
        connect();
        return verifyStatusCode(ftpClient.noop());        
    }
    
    /**
     * Get a list of file names in a given directory for admin
     * @return List of files/directories
     * @throws IOException
     */
    public String[] getFileList(String path) throws IOException
    {
        connect();
        return ftpClient.listNames(path);
    }

    /**
     * Create a directory
     * @param dir
     * @return true if successful, false if not
     * @throws IOException
     */
    public boolean makeDir(String dir) throws IOException
    {
        connect();
        return verifyStatusCode(ftpClient.mkd(dir));
    }
    
    /**
     * Delete a directory
     * @param dir The directory to delete
     * @return true if successful, false if not
     * @throws IOException
     */
    public boolean deleteDir(String dir) throws IOException
    {
        connect();
        return verifyStatusCode(ftpClient.rmd(dir));
    }
    
    /**
     * Check that the status code is successful (between 200 and 299)
     * @param status The status code to check
     * @return true if status is successful, false if not
     */ 
    private boolean verifyStatusCode(int status)
    {
        if(status >= 200 && status < 300)
        {
            return true;
        }
        return false;
    }

    /**
     * Upload a file to the ftp server
     * @param fileName The file to upload
     * @return true if successful, false if not
     * @throws IOException
     */
    public boolean putFile(String fileName, String targetDir) throws IOException
    {
        connect();       
        File file = new File(IOUtils.getResourceAsUrl(fileName, getClass()).getFile()); //hacky way to get the file shortname        
        return ftpClient.storeFile(targetDir + "/" + file.getName(), IOUtils.getResourceAsStream(fileName, getClass()));
    }
    
    /**
     * Check if a directory exists by trying to go to it
     * @param path The directory to try
     * @return True if the directory exists, false if not
     * @throws IOException
     */
    public boolean dirExists(String path) throws IOException
    {
        connect();
        String cwd = ftpClient.printWorkingDirectory(); //store the current working dir so we can go back to it
        boolean dirExists = ftpClient.changeWorkingDirectory(path);
        ftpClient.changeWorkingDirectory(cwd); // go back to the cwd
        return dirExists;
    }
    
    /**
     * Delete all files and subdirectories. Note: extra slashes are ignored by the
     * ftp server, so I didn't bother to filter them out
     * 
     */
    public void recursiveDelete(String path) throws IOException
    {
        connect();
        String cwd = ftpClient.printWorkingDirectory(); //store the current working dir so we can go back to it
        System.out.println("CWD: " + cwd);
        ftpClient.changeWorkingDirectory(path);
        System.out.println("Changed CWD: " + path);
        
        FTPFile[] fileObjs = ftpClient.listFiles();
        for(int i = 0; i < fileObjs.length; i++)
        {
            if(fileObjs[i].isFile()) //delete the file
            {
                ftpClient.deleteFile(fileObjs[i].getName());
            }
            else if(fileObjs[i].isDirectory() && (getFileList(ftpClient.printWorkingDirectory() + "/" + fileObjs[i].getName()).length > 0))
            {
                recursiveDelete(ftpClient.printWorkingDirectory() + "/" + fileObjs[i].getName());
                deleteDir(ftpClient.printWorkingDirectory() + "/" + fileObjs[i].getName()); //safe to delete dir now that it's empty
            }
            else if(fileObjs[i].isDirectory()) //delete the empty directory
            {
                deleteDir(ftpClient.printWorkingDirectory() + "/" + fileObjs[i].getName());                
            }
            // ignore file if not a file or a dir
        }
        ftpClient.changeWorkingDirectory(cwd); // go back to the cwd
    }
    
    /**
     * Initiate a connection to the ftp server
     * @throws IOException
     */
    protected void connect() throws IOException
    {
        if(!ftpClient.isConnected())
        {
            ftpClient = new FTPClient();
            ftpClient.setDefaultTimeout(TIMEOUT);
            ftpClient.connect(server, port);
            ftpClient.login(user, password);
        }
    }

    /**
     * Check if the ftp client is connected
     * @return true if connected, false if not
     */
    public boolean isConnected()
    {
        return ftpClient.isConnected();
    }

    /**
     * Disconnect the ftp client
     * @throws IOException
     */
    public void disconnect() throws IOException
    {
        ftpClient.disconnect();
    }

    /**
     * Check if a file exists on the ftp server
     * @param file The name of the file to check
     * @return true if file exists, false if not
     * @throws IOException
     */
    public boolean fileExists(String file) throws IOException
    {
        return (ftpClient.listFiles(file).length > 0);
    }

    /**
     * Delete a single file.
     * @param name The file to delete
     * @return true if successful, false if not
     * @throws IOException
     */
    public boolean deleteFile(String name) throws IOException
    {
        return ftpClient.deleteFile(name);
    }

    /**
     * Verify that a number of files exist on the ftp server
     * @param directory The remote directory to check
     * @param timeout The max time to wait
     * @return true if the file count matches before the timeout, false if not
     * @throws InterruptedException 
     * @throws IOException 
     */
    public boolean expectFileCount(String directory, int count, long timeout) throws InterruptedException, IOException
    {
        long endTime = System.currentTimeMillis() + timeout;
        int iteration = 1;
        while(System.currentTimeMillis() < endTime)
        {
            logger.debug("checking file list, iteration :" + iteration);
            if(getFileList(directory).length == count)
            {
                logger.debug("found expected file count : " + count);
                return true;
            }            
            Thread.sleep(1000);
            ++iteration;
        }
        return false;
    }
}
