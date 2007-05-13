/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.util.Properties;

import org.apache.ftpserver.ftplet.Configuration;
import org.apache.ftpserver.config.PropertiesConfiguration;
import org.apache.ftpserver.interfaces.FtpServerContext;
import org.apache.ftpserver.ConfigurableFtpServerContext;
import org.apache.ftpserver.FtpServer;

/**
 * An initial wrapper for the Apache ftpServer.  This will progress into a provider of its own,
 * but for now is necessary to avoid duplicating code in FTP tests using FTPClient.
 */
public class Server
{

    private static final String FTP_STATE_KEY = "ftp-state-key-";
    private FtpServer server;
    private ServerState state;
    private int port;

    public Server(int port) throws Exception
    {
        this.port = port;
        state = new InOutState();

        // this must be set BEFORE the configuration is created
        // is is accessed BEFORE server startup
        System.getProperties().put(FTP_STATE_KEY + port, state);

        Properties properties = new Properties();
        properties.setProperty("config.listeners.default.port", Integer.toString(port));
        properties.setProperty("config.file-system-manager.class", FileManager.class.getName());
        properties.setProperty("config.file-system-manager.stateFromSystemProperties", FTP_STATE_KEY + port);
        properties.setProperty("config.connection-manager.default-idle-time", "1");
        properties.setProperty("config.connection-manager.max-login", "1000");
        properties.setProperty("config.connection-manager.max-anonymous-login", "1000");
        Configuration config = new PropertiesConfiguration(properties);
        FtpServerContext context = new ConfigurableFtpServerContext(config);


        server = new FtpServer(context);
        server.start();
    }

    public void awaitStart(long ms) throws InterruptedException
    {
        state.awaitStart(ms);
    }

    public NamedPayload awaitUpload(long ms) throws InterruptedException
    {
        return state.awaitUpload(ms);
    }

    public void stop()
    {
        server.stop();
        System.getProperties().remove(FTP_STATE_KEY + port);
    }

}
