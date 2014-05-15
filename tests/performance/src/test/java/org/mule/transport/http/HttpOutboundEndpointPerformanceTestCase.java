/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.ChainedThreadingProfile;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.BlockingChannelConnector;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.IO;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class HttpOutboundEndpointPerformanceTestCase extends AbstractMuleTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Mock
    private Flow flow;

    protected static Server jetty;
    protected static OutboundEndpoint endpoint;
    protected static byte[] payload;
    protected static MuleContext muleContext;

    @Override
    public int getTestTimeoutSecs()
    {
        return 180;
    }

    @BeforeClass
    public static void before() throws Exception
    {
        payload = createPayload(2048);

        final BlockingChannelConnector connector = new BlockingChannelConnector();
        jetty = new Server();
        jetty.addConnector(connector);
        jetty.setHandler(new EchoHandler());
        jetty.setThreadPool(new org.eclipse.jetty.util.thread.QueuedThreadPool(500));
        jetty.start();

        muleContext = new DefaultMuleContextFactory().createMuleContext();

        HttpConnector httpConnector = new HttpConnector(muleContext);
        httpConnector.setName("500Threads");
        ThreadingProfile tp = new ChainedThreadingProfile();
        tp.setMaxThreadsActive(500);
        httpConnector.setDispatcherThreadingProfile(tp);
        muleContext.getRegistry().registerConnector(httpConnector);

        EndpointBuilder builder = muleContext.getEndpointFactory().getEndpointBuilder(
            "http://localhost:" + connector.getLocalPort() + "/echo?connectorName");
        builder.setConnector(httpConnector);
        endpoint = builder.buildOutboundEndpoint();;
        muleContext.start();
    }

    @AfterClass
    public static void after() throws Exception
    {
        jetty.stop();
        endpoint.getConnector().stop();
        muleContext.dispose();
    }

    @Test
    @PerfTest(duration = 20000, threads = 10, warmUp = 10000)
    public void send10Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    @Test
    @PerfTest(duration = 20000, threads = 20, warmUp = 10000)
    public void send20Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    @Test
    @PerfTest(duration = 20000, threads = 50, warmUp = 10000)
    public void send50Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    @Test
    @PerfTest(duration = 20000, threads = 100, warmUp = 10000)
    public void send100Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    @Test
    @PerfTest(duration = 40000, threads = 200, warmUp = 10000)
    public void send200Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    @Test
    @PerfTest(duration = 40000, threads = 500, warmUp = 10000)
    public void send500Threads() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent()).getMessageAsBytes();
    }

    protected DefaultMuleEvent createMuleEvent() throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(payload, muleContext),
            MessageExchangePattern.REQUEST_RESPONSE, flow);
    }

    protected static byte[] createPayload(int length)
    {
        final byte[] content = new byte[length];
        final int r = Math.abs(content.hashCode());
        for (int i = 0; i < content.length; i++)
        {
            content[i] = (byte) ((r + i) % 96 + 32);
        }
        return content;
    }

    static class EchoHandler extends AbstractHandler
    {

        @Override
        public void handle(final String target,
                           final Request baseRequest,
                           final HttpServletRequest request,
                           final HttpServletResponse response) throws IOException, ServletException
        {
            final ByteArrayOutputStream2 buffer = new ByteArrayOutputStream2();
            final InputStream instream = request.getInputStream();
            if (instream != null)
            {
                IO.copy(instream, buffer);
                buffer.flush();
            }
            final byte[] content = buffer.getBuf();
            final int len = buffer.getCount();

            response.setStatus(200);
            response.setContentLength(len);

            final OutputStream outstream = response.getOutputStream();
            outstream.write(content, 0, len);
            outstream.flush();
        }

    }

}
