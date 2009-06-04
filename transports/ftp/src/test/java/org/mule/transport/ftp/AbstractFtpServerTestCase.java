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

import java.io.IOException;

/**
 * Abstract FTP test class. Sets up the ftp server and starts/stops it during the
 * test lifecycle.
 */
public abstract class AbstractFtpServerTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test FTP message";
    private static int DEFAULT_TIMEOUT = 10000;
    private int timeout;
    private int ftpPort;
    private Server server = null;
    /**
     * We only test against an embedded server, but you can test against remote
     * servers by changing this value. Some tests may start/stop the ftp server so
     * you will have to implement remote start/stop as well.
     */
    private String ftpHost = "localhost";
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
        this.ftpPort = port;
        this.timeout = timeout;
        ftpClient = new FTPTestClient(this.ftpHost, this.ftpPort, this.ftpUser, this.ftpPassword);
    }

    public AbstractFtpServerTestCase(int port)
    {
        this(port, DEFAULT_TIMEOUT);
        ftpClient = new FTPTestClient(this.ftpHost, this.ftpPort, this.ftpUser, this.ftpPassword);
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
        startServer();
        if(!ftpClient.testConnection())
        {
            throw new IOException("could not connect to ftp server");
        }
        ftpClient.recursiveDelete("/"); //make sure there are no files on the ftpserver when we start
    }

    @Override
    protected void doTearDown() throws Exception
    {
        ftpClient.disconnect(); // we dont need the connection anymore for this test
        stopServer();
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
}
