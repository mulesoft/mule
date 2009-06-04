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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Various tests against the FTPClient/Server we use in Mule ftp transport tests.
 * This is to make sure basic ftp functionality works with our current ftp
 * client/server before we throw Mule into the mix.
 */
public class FTPServerClientTest extends TestCase
{
    Server ftpServer = null;
    FTPTestClient ftpClient = null;
    public static final int PORT = 60198;
    private static final String adminUser = "admin";
    private static final String adminPassword = "admin";
    
    /**
     * Initialize the ftp server
     */
    public void setUp()
    {
        try
        {
            ftpServer = new Server(PORT);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Create a directory and delete it
     * @throws IOException
     */
    public void testCreateDeleteDir() throws IOException
    {
        ftpClient = new FTPTestClient("localhost", PORT, adminUser, adminPassword);
        String dir = "/foo/";
        assertTrue("unable to create directory: " + dir, ftpClient.makeDir(dir));
        //verify directory was created
        assertTrue("Directory '" + dir + "' does not exist", ftpClient.dirExists(dir));
        assertTrue("unable to delete directory: " + dir, ftpClient.deleteDir(dir));
        assertFalse("Directory '" + dir + "' still exists", ftpClient.dirExists(dir));
    }
    
    /**
     * Create a file and delete it
     * @throws IOException
     */    
    public void testCreateDeleteFile() throws IOException
    {
        ftpClient = new FTPTestClient("localhost", PORT, adminUser, adminPassword);
        File testFile = File.createTempFile("fake", "file");
        ftpClient.putFile(testFile.getAbsolutePath(),"/");
        assertTrue("Could not find file :" + testFile.getName(), ftpClient.fileExists(testFile.getName()));
        ftpClient.deleteFile(testFile.getName());
        assertFalse("file was not deleted :" + testFile.getName(), ftpClient.fileExists(testFile.getName()));
    }

    /**
     * Create a bunch of files/dirs then recursively delete them
     * @throws IOException
     */
    public void testRecursiveDelete() throws IOException
    {                        
        ftpClient = new FTPTestClient("localhost", PORT, adminUser, adminPassword);
        
        assertTrue(ftpClient.makeDir("dir1/"));
        ftpClient.dirExists("dir1/");
        assertTrue(ftpClient.makeDir("/dir1/dir21/"));
        ftpClient.dirExists("/dir1/dir21/");
        assertTrue(ftpClient.makeDir("/dir1/dir22/"));
        ftpClient.dirExists("/dir1/dir22/");
        assertTrue(ftpClient.makeDir("/dir1/dir21/dir3/"));
        ftpClient.dirExists("/dir1/dir21/dir3/");
        
        //TODO DZ: we should really be using files with data in them for more realistic testing  
        File testFile0 = File.createTempFile("testFile0", "file");
        File testFile1 = File.createTempFile("testFile1", "file");
        File testFile2 = File.createTempFile("testFile2", "file");
        File testFile3 = File.createTempFile("testFile3", "file");
        File testFile4 = File.createTempFile("testFile4", "file");
        File testFile5 = File.createTempFile("testFile5", "file");
        File testFile6 = File.createTempFile("testFile6", "file");        
        File testFile7 = File.createTempFile("testFile7", "file");
        File testFile8 = File.createTempFile("testFile8", "file");
        File testFile9 = File.createTempFile("testFile9", "file");
               
        assertTrue(ftpClient.putFile(testFile0.getAbsolutePath(), "/"));
        ftpClient.fileExists("/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile1.getAbsolutePath(), "/"));
        ftpClient.fileExists("/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile2.getAbsolutePath(), "/dir1/"));
        ftpClient.fileExists("/dir1/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile3.getAbsolutePath(), "/dir1/"));
        ftpClient.fileExists("/dir1/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile4.getAbsolutePath(), "/dir1/dir21/"));
        ftpClient.fileExists("/dir1/dir21/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile5.getAbsolutePath(), "/dir1/dir21/"));
        ftpClient.fileExists("/dir1/dir21/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile6.getAbsolutePath(), "/dir1/dir22/"));
        ftpClient.fileExists("/dir1/dir22/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile7.getAbsolutePath(), "/dir1/dir22/"));
        ftpClient.fileExists("/dir1/dir22/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile8.getAbsolutePath(), "/dir1/dir21/dir3/"));
        ftpClient.fileExists("/dir1/dir21/dir3/" + testFile0.getName());
        assertTrue(ftpClient.putFile(testFile9.getAbsolutePath(), "/dir1/dir21/dir3/"));
        ftpClient.fileExists("/dir1/dir21/dir3/" + testFile0.getName());
               
        ftpClient.recursiveDelete("/"); //there should be no files left over after this command
        assertEquals("there are still files left over", 0, ftpClient.getFileList("/").length);
    }
        
    /**
     * Stop the ftp server and disconnect the client
     */
    public void tearDown()
    {
        if(ftpServer != null)
        {
            ftpServer.stop();
        }                
        
        if(ftpClient != null && ftpClient.isConnected())
        {
            try
            {
                ftpClient.disconnect();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }                
    }
}
