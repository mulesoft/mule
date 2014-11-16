/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.util.FileUtils;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

public class HttpRequestCustomTlsConfigTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-custom-tls-config.xml";
    }

    @Test
    public void configureTlsFromGlobalContext() throws Exception
    {
        runFlow("testFlowGlobalContext", TEST_MESSAGE);
        assertThat(body, equalTo(TEST_MESSAGE));
    }

    @Test
    public void configureTlsFromNestedContext() throws Exception
    {
        runFlow("testFlowNestedContext", TEST_MESSAGE);
        assertThat(body, equalTo(TEST_MESSAGE));
    }

    @Override
    protected Server createServer()
    {
        Server server = new Server();
        SslContextFactory sslContextFactory = new SslContextFactory();

        try
        {
            sslContextFactory.setKeyStorePath(FileUtils.getResourcePath("serverKeystore", getClass()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        sslContextFactory.setKeyStorePassword("mulepassword");
        sslContextFactory.setKeyManagerPassword("mulepassword");

        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setPort(httpPort.getNumber());
        server.addConnector(connector);

        return server;
    }
}
