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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AbstractWSDLServerTestCase extends FunctionalTestCase
{

    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port");

    private static final String WSDL_FILE_LOCATION = "/Test.wsdl";

    @Rule
    public ExternalResource mockServer = new ServerResource(dynamicPort);

    /**
     * JUnit rule to initialize and teardown the http server
     */
    public static class ServerResource extends ExternalResource
    {
        private TestServer server;
        private DynamicPort port;

        ServerResource(DynamicPort port)
        {
            this.port = port;
        }

        @Override
        protected void before() throws Throwable
        {
            server = new TestServer(port.getNumber());
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
    public static class TestServer
    {
        private int serverPort;
        private Server server;

        public TestServer(int serverPort)
        {
            this.serverPort = serverPort;
        }

        public void start() throws Exception
        {
            server = new Server(serverPort);
            String contentsType = APPLICATION_XML_UTF_8.toString();
            String contents = IOUtils.toString(this.getClass().getResourceAsStream(WSDL_FILE_LOCATION), UTF_8.name());
            server.setHandler(new TestHandler(contentsType, contents));
            server.start();
        }

        public void stop() throws Exception
        {
            server.stop();
        }
    }

    public static class TestHandler extends AbstractHandler
    {
        private final String contentsType;
        private final String contents;

        public TestHandler(String contentsType, String contents)
        {
            this.contentsType = contentsType;
            this.contents = contents;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            response.setContentType(contentsType);
            response.setContentLength(contents.length());
            response.getWriter().write(contents);
        }
    }

}
