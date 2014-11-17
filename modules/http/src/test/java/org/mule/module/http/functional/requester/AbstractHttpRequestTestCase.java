/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.collections.map.MultiValueMap;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public class AbstractHttpRequestTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    public static final String DEFAULT_RESPONSE = "<h1>Response</h1>";

    protected Server server;

    protected String method;
    protected String uri;
    protected MultiValueMap headers = new MultiValueMap();
    protected String body;

    @Before
    public void startServer() throws Exception
    {
        server = createServer();
        server.setHandler(createHandler(server));
        server.start();
    }

    @After
    public void stopServer() throws Exception
    {
        server.stop();
    }

    protected Server createServer()
    {
        return new Server(httpPort.getNumber());
    }

    protected AbstractHandler createHandler(Server server)
    {
        return new TestHandler();
    }

    private class TestHandler extends AbstractHandler
    {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {

            handleRequest(baseRequest, request, response);

            baseRequest.setHandled(true);
        }
    }


    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        extractBaseRequestParts(baseRequest);
        writeResponse(response);
    }

    protected void extractBaseRequestParts(Request baseRequest) throws IOException
    {
        method = baseRequest.getMethod();
        uri = baseRequest.getUri().getCompletePath();

        extractHeadersFromBaseRequest(baseRequest);

        body = IOUtils.toString(baseRequest.getInputStream());
    }

    protected void extractHeadersFromBaseRequest(Request baseRequest)
    {
        for (String headerName : (List<String>) EnumerationUtils.toList(baseRequest.getHeaderNames()))
        {
            Enumeration<String> headerValues = baseRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements())
            {
                headers.put(headerName, headerValues.nextElement());
            }
        }
    }

    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(DEFAULT_RESPONSE);
    }

    public String getFirstReceivedHeader(String headerName)
    {
        return (String) headers.getCollection(headerName).iterator().next();
    }
}
