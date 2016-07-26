/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

//TODO: MULE-9702 Remove once the tests are migrated.
public class BasicHttpTestCase extends MuleArtifactFunctionalTestCase
{
    @Rule
    public DynamicPort clientPort = new DynamicPort("clientPort");
    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    protected Server server;

    protected String method;
    protected String uri;
    private String query;
    private Map<String, String> headers = new HashMap<>();

    @Override
    protected String getConfigFile()
    {
        return "basic-http-config.xml";
    }

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
        if (server != null)
        {
            server.stop();
        }
    }

    protected Server createServer()
    {
        Server server = new Server(clientPort.getNumber());
        return server;
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
        query = baseRequest.getUri().getQuery();
        Enumeration<String> headerNames = baseRequest.getHeaderNames();
        while(headerNames.hasMoreElements())
        {
            String headerName = headerNames.nextElement();
            headers.put(headerName, baseRequest.getHeader(headerName));
        }
    }

    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("WOW");
    }

    @Test
    public void sendsRequest() throws Exception
    {
        MuleEvent response = flowRunner("client").withPayload("PEPE").run();
        assertThat(IOUtils.toString((InputStream) response.getMessage().getPayload()), is("WOW"));
        assertThat(method, is("GET"));
        assertThat(headers, hasEntry("X-Custom", "custom-value"));
        assertThat(query, is("query=param"));
    }

    @Test
    public void receivesRequest() throws Exception
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet getRequest = new HttpGet(String.format("http://localhost:%s/test?query=param", serverPort.getValue()));
        getRequest.addHeader("Y-Custom", "value-custom");
        try
        {
            CloseableHttpResponse response = httpClient.execute(getRequest);
            try
            {
                assertThat(IOUtils.toString(response.getEntity().getContent()), is("HEY"));
            }
            finally
            {
                response.close();
            }
        }
        finally
        {
            httpClient.close();
        }
    }

    protected static class RequestCheckerMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();
            Object payload = message.getPayload();
            assertThat(payload, is(nullValue()));
            assertThat(message.getAttributes(), instanceOf(HttpRequestAttributes.class));
            HttpRequestAttributes requestAttributes = (HttpRequestAttributes) message.getAttributes();
            assertThat(requestAttributes.getMethod(), is("GET"));
            assertThat(requestAttributes.getScheme(), is("http"));
            assertThat(requestAttributes.getVersion(), is("HTTP/1.1"));
            assertThat(requestAttributes.getRequestUri(), is("/test?query=param"));
            assertThat(requestAttributes.getListenerPath(), is("/test"));
            assertThat(requestAttributes.getQueryString(), is("query=param"));
            assertThat(requestAttributes.getQueryParams(), hasEntry("query", "param"));
            assertThat(requestAttributes.getHeaders(), hasEntry("y-custom", "value-custom"));
            assertThat(requestAttributes.getParts().entrySet(), is(empty()));
            return event;
        }
    }
}
