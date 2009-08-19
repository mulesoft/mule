/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.ftp.server.FTPTestClient;
import org.mule.transport.ftp.server.Server;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Abstract FTP test class. Sets up the ftp server and starts/stops it during the
 * test lifecycle.
 */
public abstract class AbstractFtpServerTestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test FTP message";
    
    private static final String DEFAULT_FTP_HOST = "localhost";
    private static int DEFAULT_TIMEOUT = 10000;
    public static final String FTP_SERVER_BASE_DIR = "target/ftpserver";
    
    private int timeout;
    private int ftpPort;
    private Server server = null;
    /**
     * We only test against an embedded server, but you can test against remote
     * servers by changing this value. Some tests may start/stop the ftp server so
     * you will have to implement remote start/stop as well.
     */
    private String ftpHost;
    private FTPTestClient ftpClient = null;
    private String ftpUser = "anonymous";
    private String ftpPassword = "password";

    public AbstractFtpServerTestCase(String ftpHost, int port, int timeout)
    {        
        this.ftpHost = ftpHost;
        this.ftpPort = port;
        this.timeout = timeout;
        ftpClient = new FTPTestClient(this.ftpHost, this.ftpPort, this.ftpUser, this.ftpPassword);                
    }
    
    public AbstractFtpServerTestCase(int port, int timeout)
    {
        this(DEFAULT_FTP_HOST, port, timeout);
    }

    public AbstractFtpServerTestCase(int port)
    {
        this(port, DEFAULT_TIMEOUT);
    }

    protected void startServer() throws Exception
    {
        server = new Server(ftpPort);
        // this is really ugly, but the above doesn't get to waiting.
        // need to improve this as part of ftp server work
        synchronized(this)
        {
            wait(500);
        }
    }

    protected void stopServer() throws Exception
    {
        // stop the server
        if (null != server)
        {
            server.stop();
        }
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // make sure we start out with a clean ftp server base
        createFtpServerBaseDir();

        startServer();
        if (!ftpClient.testConnection())
        {
            throw new IOException("could not connect to ftp server");
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        ftpClient.disconnect(); // we dont need the connection anymore for this test
        stopServer();

        deleteFtpServerBaseDir();
        
        super.doTearDown();
    }

    private void createFtpServerBaseDir()
    {
        deleteFtpServerBaseDir();
        File ftpBaseDir = new File(FTP_SERVER_BASE_DIR);
        ftpBaseDir.mkdirs();
    }

    private void deleteFtpServerBaseDir()
    {
        File ftpServerBase = new File(FTP_SERVER_BASE_DIR);
        FileUtils.deleteTree(ftpServerBase);
    }

    protected int getTimeout()
    {
        return timeout;
    }

    public void setFtpHost(String ftpHost)
    {
        this.ftpHost = ftpHost;
    }

    public String getFtpHost()
    {
        return ftpHost;
    }
    
    public String getFtpUser()
    {
        return ftpUser;
    }

    public String getFtpPassword()
    {
        return ftpPassword;
    }

    public int getFtpPort()
    {
        return ftpPort;
    }

    public FTPTestClient getFtpClient()
    {
        return ftpClient;
    }
    
    /**
     * Return the endpoint denoted by the ftp configuration
     * @return
     */
    public String getMuleFtpEndpoint()
    {
        return "ftp://" + getFtpUser() + ":" + getFtpPassword() + "@" + getFtpHost() + ":" + getFtpPort();       
    }

    protected void createFileOnFtpServer(String fileName) throws IOException
    {        
        File outFile = new File(FTP_SERVER_BASE_DIR, fileName);
        assertFalse(outFile.exists());
        
        Writer outWriter = new FileWriter(outFile);
        outWriter.write(TEST_MESSAGE);
        outWriter.close();
    }
}
