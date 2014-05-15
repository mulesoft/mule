
package org.mule.transport.http;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class HttpOutboundEndpointPerformanceTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Mock
    private Flow flow;

    protected Server jetty;
    protected OutboundEndpoint endpoint;

    @Override
    public int getTestTimeoutSecs()
    {
        return 180;
    }

    @Before
    public void before() throws Exception
    {
        final BlockingChannelConnector connector = new BlockingChannelConnector();
        jetty = new Server();
        jetty.addConnector(connector);
        jetty.setHandler(new EchoHandler());
        jetty.setThreadPool(new org.eclipse.jetty.util.thread.QueuedThreadPool(500));
        jetty.start();

        endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            "http://localhost:" + connector.getLocalPort() + "/echo");
        muleContext.start();
    }

    @After
    public void after() throws Exception
    {
        jetty.stop();
        endpoint.getConnector().stop();
        muleContext.dispose();
    }

    @Test
    @PerfTest(duration = 40000, threads = 200, warmUp = 10000)
    public void send() throws MuleException, Exception
    {
        endpoint.process(createMuleEvent("1"));
    }

    protected DefaultMuleEvent createMuleEvent(Object payload) throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(payload, muleContext),
            MessageExchangePattern.REQUEST_RESPONSE, flow);
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
