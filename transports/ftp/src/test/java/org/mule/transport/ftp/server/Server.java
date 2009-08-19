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

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
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
        FtpServerFactory serverFactory = new FtpServerFactory();        
        ListenerFactory factory = new ListenerFactory();                
        // set the port of the listener
        factory.setPort(port);
        // replace the default listener
        serverFactory.addListener("default", factory.createListener());
        factory.setIdleTimeout(60000);
                
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        URL usersFile = IOUtils.getResourceAsUrl("users.properties", getClass());
        if(usersFile == null)
        {
            throw new IOException("users.properties file not found in the classpath");
        }
        
        userManagerFactory.setFile(new File(usersFile.getFile()));
        serverFactory.setUserManager(userManagerFactory.createUserManager());

        // start the server
        server = serverFactory.createServer();
        server.start();
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
