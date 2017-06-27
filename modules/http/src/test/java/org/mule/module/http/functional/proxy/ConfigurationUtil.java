/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.proxy;

import org.mule.util.FileUtils;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * <code>ConfigurationUtil</code> is a utility class for configuring servers
 * used in multiple tests.
 */
public class ConfigurationUtil
{
    public static void configureHttpsServer(Server server, int port, Class callingClass)
    {
        SslContextFactory sslContextFactory = new SslContextFactory();

        try
        {
            sslContextFactory.setKeyStorePath(FileUtils.getResourcePath("tls/serverKeystore", callingClass));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        sslContextFactory.setKeyStorePassword("mulepassword");
        sslContextFactory.setKeyManagerPassword("mulepassword");

        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setPort(port);
        server.addConnector(connector);
    }
}