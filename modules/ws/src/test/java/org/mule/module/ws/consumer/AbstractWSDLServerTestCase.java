/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.After;
import org.junit.Rule;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

public class AbstractWSDLServerTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("portNoReply");

    private static final String WSDL_FILE_LOCATION = "/Test.wsdl";

    private MockServer server = new MockServer(dynamicPort.getNumber());


    @After
    public void after() throws Exception
    {
        server.stop();
    }

    /**
     * Implementation of an http fake server
     */
    private static class MockServer
    {

        private int serverPort;
        private HttpServer server;

        public MockServer(int serverPort)
        {
            try
            {
                this.serverPort = serverPort;
                start();
            }
            catch (Exception e)
            {
                fail("Could not construct mock server");
            }
        }

        public void start() throws IOException
        {
            server = HttpServer.createSimpleServer("/", serverPort);
            server.getServerConfiguration().addHttpHandler(new HttpHandler()
            {
                public void service(Request request, Response response) throws Exception
                {
                    response.setContentType(APPLICATION_XML_UTF_8.toString());
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
