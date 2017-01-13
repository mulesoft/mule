/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

public class AbstractWSDLServerTestCase extends FunctionalTestCase
{

    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port");

    private static final String WSDL_FILE_LOCATION = "/Test.wsdl";

    @ClassRule
    public static ExternalResource mockServer = new ServerResource(dynamicPort);

    /**
     * JUnit rule to initialize and teardown the http server
     */
    public static class ServerResource extends ExternalResource
    {
        private Server server;
        private DynamicPort port;

        ServerResource(DynamicPort port)
        {
            this.port = port;
        }

        @Override
        protected void before() throws Throwable
        {
            server = new Server(dynamicPort.getNumber());
            server.start();
        }

        @Override
        protected void after()
        {
            try
            {
                server.stop();
            }
            catch (Exception e)
            {
                throw new RuntimeException("server stop failed");
            }
        }
    }

    /**
     * Implementation of an http fake server
     */
    public static class Server
    {

        private int serverPort;
        private HttpServer server;

        public Server(int serverPort)
        {
            this.serverPort = serverPort;
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
