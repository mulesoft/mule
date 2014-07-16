/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp.server;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ftp.AbstractFtpServerTestCase;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Various tests against the FTPClient/Server we use in Mule ftp transport tests.
 * This is to make sure basic ftp functionality works with our current ftp
 * client/server before we throw Mule into the mix.
 */
public class FTPServerClientTest extends AbstractMuleTestCase
{

    private static final String adminUser = "admin";
    private static final String adminPassword = "admin";

    private Server ftpServer = null;
    private FTPTestClient ftpClient = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Before
    public void setUp() throws Exception
    {
        new File(AbstractFtpServerTestCase.FTP_SERVER_BASE_DIR).mkdirs();
        ftpServer = new Server(dynamicPort.getNumber());
    }
    
    /**
     * Create a directory and delete it
     * @throws IOException
     */
    @Test
    public void testCreateDeleteDir() throws IOException
    {
        ftpClient = new FTPTestClient("localhost", dynamicPort.getNumber(), adminUser, adminPassword);
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
    @Test
    public void testCreateDeleteFile() throws IOException
    {
        ftpClient = new FTPTestClient("localhost", dynamicPort.getNumber(), adminUser, adminPassword);
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
    @Test
    public void testRecursiveDelete() throws IOException
    {                        
        ftpClient = new FTPTestClient("localhost", dynamicPort.getNumber(), adminUser, adminPassword);
        
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
        
    @After
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
