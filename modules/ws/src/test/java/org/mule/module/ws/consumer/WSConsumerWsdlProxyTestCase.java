/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import com.google.common.net.MediaType;

/**
 * This tests "mocks" a proxy server through which a wsdl file is served.
 *
 */
public class WSConsumerWsdlProxyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");
    
    private static final String WSDL_FILE_LOCATION = "/Test.wsdl";

    private static final String EXPECTED_OPERATION = "noParamsWithHeader";

    private MockProxyServer proxyServer = new MockProxyServer(dynamicPort.getNumber());


    @After
    public void after() throws Exception
    {
        proxyServer.stop();
    }

    @Override
    protected String getConfigFile()
    {
        return "ws-consumer-wsdl-proxy.xml";
    }

    @Test
    public void wsConsumerConfigGetWsdlThroughProxy() throws Exception
    {
        WSConsumer consumer = muleContext.getRegistry().lookupObject(WSConsumer.class);
        assertThat(EXPECTED_OPERATION, equalTo(consumer.getOperation()));
    }

    /**
     * Implementation of an http proxy fake server
     */
    private static class MockProxyServer
    {

        private int proxyServerPort;
        private HttpServer server;

        public MockProxyServer(int proxyServerPort)
        {
            try
            {
                this.proxyServerPort = proxyServerPort;
                start();
            }
            catch (Exception e)
            {
                fail("Could not construct mock proxy server");
            }
        }

        public void start() throws IOException
        {
            server = HttpServer.createSimpleServer("/", proxyServerPort);
            server.getServerConfiguration().addHttpHandler(new HttpHandler()
            {
                public void service(Request request, Response response) throws Exception
                {
                    response.setContentType(MediaType.APPLICATION_XML_UTF_8.toString());
                    String contents = IOUtils.toString(this.getClass().getResourceAsStream(WSDL_FILE_LOCATION),
                                                       UTF_8.name());
                    response.setContentLength(contents.length());
                    response.getWriter().write(contents);
                }
            });

            server.start();
        }

        public void stop() throws Exception
        {
            server.shutdownNow();
        }

    }

}
