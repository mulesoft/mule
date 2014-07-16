/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util.ftp;

import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.apache.ftpserver.ftplet.Ftplet;

public class FtpServer
{
    public static final String FTP_SERVER_BASE_DIR = "target/ftpserver";
    
    private String ftpHost;
    private int ftpPort;
    private String ftpUser;
    private String ftpPassword;
    private Server server;
    private FtpClient ftpClient;
    
    private Ftplet ftplet = new MuleFtplet(new MuleFtplet.Callback()
    {
        @Override
        public void fileUploadCompleted()
        {
        }
        @Override
        public void fileMoveCompleted()
        {
        }
    });

    public FtpServer(String ftpHost, int ftpPort, String ftpUser, String ftpPassword)
    {
        this.ftpHost = ftpHost;
        this.ftpPort = ftpPort;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
    }

    public void start() throws Exception
    {
        ftpClient = new FtpClient(this.ftpHost, this.ftpPort, this.ftpUser, this.ftpPassword);
        // make sure we start out with a clean ftp server base
        createFtpServerBaseDir();

        startServer();
        if (!ftpClient.testConnection())
        {
            throw new IOException("could not connect to ftp server");
        }
    }

    public void stop() throws Exception
    {
        Thread.sleep(500);

        ftpClient.disconnect(); // we dont need the connection anymore for this test
        stopServer();

        deleteFtpServerBaseDir();
    }

    public FtpClient getFtpClient()
    {
        return ftpClient;
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

    private void startServer() throws Exception
    {
        server = new Server(ftpPort, ftplet);
        // this is really ugly, but the above doesn't get to waiting.
        // need to improve this as part of ftp server work
        synchronized(this)
        {
            wait(500);
        }
    }

    private void stopServer() throws Exception
    {
        // stop the server
        if (null != server)
        {
            server.stop();
        }
    }
}
