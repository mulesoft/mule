/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static junit.framework.TestCase.fail;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestMaxConnectionsTestCase extends AbstractMuleTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private Latch messageArrived = new Latch();
    private Latch messageHold = new Latch();

    private Server server;

    private GrizzlyHttpClient client;

    @After
    public void stopServer() throws Exception
    {
        server.stop();
    }

    @Before
    public void setup() throws Exception
    {
        server = new Server(httpPort.getNumber());
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                onRequestReceived();
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("TEST");
                baseRequest.setHandled(true);
            }
        });

        server.start();

        GrizzlyHttpClientConfiguration configuration = new GrizzlyHttpClientConfiguration.Builder()
                .setMaxConnections(1)
                .setMaxConnectionWaitTime(0)
                .build();

        client = new GrizzlyHttpClient(configuration);
        client.initialise();
    }

    @After
    public void tearDown() throws Exception
    {
        client.stop();
        server.stop();
    }

    @Test
    public void clientLimitsMaxOutboundConnections() throws Exception
    {
        Thread t1 = processAsynchronously();
        messageArrived.await();
        try
        {
            sendRequest();
            fail("Max connections should be reached.");
        }
        catch(Exception e)
        {
            // Expected
        }
        messageHold.release();
        t1.join();
    }

    private Thread processAsynchronously()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    sendRequest();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        return thread;
    }

    private void sendRequest() throws Exception
    {
        HttpRequest request = new HttpRequestBuilder().setUri(String.format("http://localhost:%s/", httpPort.getNumber()))
                .setQueryParams(new ParameterMap())
                .setMethod(HttpConstants.Methods.GET.name())
                .build();

        client.send(request, 1000, false, null);
    }

    private void onRequestReceived() throws IOException
    {
        messageArrived.release();
        try
        {
            messageHold.await();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

}


