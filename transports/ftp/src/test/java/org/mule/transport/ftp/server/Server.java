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
import java.net.URL;
import java.util.Map;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

/**
 * A wrapper for the Apache ftpServer.  This will progress into a provider of its own,
 * but for now is necessary to avoid duplicating code in FTP tests using FTPClient.
 */
public class Server
{
    public static final int DEFAULT_PORT = 60196; //default for most/all tests

    private FtpServer server;

    /**
     * Initialize the ftp server on a given port
     * 
     * @param port The port to start the server on. Note, you need special
     *            permissions on *nux to open port 22, so we usually choose a very
     *            high port number.
     * @throws Exception
     */
    public Server(int port) throws Exception
    {
        this(port, null);
    }

    public Server(int port, Ftplet ftplet) throws Exception
    {
        FtpServerFactory serverFactory = new FtpServerFactory();        
        
        setupListenerFactory( serverFactory, port);                
        setupUserManagerFactory(serverFactory);
        setupFtplet(serverFactory, ftplet);
        
        server = serverFactory.createServer();
        server.start();
    }

    private void setupListenerFactory(FtpServerFactory serverFactory, int port)
    {
        ListenerFactory listenerFactory = new ListenerFactory();
        // set the port of the listener
        listenerFactory.setPort(port);
        listenerFactory.setIdleTimeout(60000);
        // replace the default listener
        serverFactory.addListener("default", listenerFactory.createListener());
    }

    private void setupUserManagerFactory(FtpServerFactory serverFactory) throws IOException
    {
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        URL usersFile = IOUtils.getResourceAsUrl("users.properties", getClass());
        if (usersFile == null)
        {
            throw new IOException("users.properties file not found in the classpath");
        }
        userManagerFactory.setFile(new File(usersFile.getFile()));
        serverFactory.setUserManager(userManagerFactory.createUserManager());
    }

    private void setupFtplet(FtpServerFactory serverFactory, Ftplet ftplet)
    {
        if (ftplet == null)
        {
            return;
        }
        
        Map<String, Ftplet> ftplets = serverFactory.getFtplets();
        ftplets.put("MuleFtplet", ftplet);
        serverFactory.setFtplets(ftplets);
    }

    /**
     * Stop the ftp server TODO DZ: we may want to put a port check + wait time in
     * here to make sure that the port is released before we continue. Windows tends
     * to hold on to ports longer than it should.
     */
    public void stop()
    {        
        server.stop();
    }
}
