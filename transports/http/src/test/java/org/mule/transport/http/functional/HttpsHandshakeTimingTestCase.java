/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpServerConnection;
import org.mule.transport.http.HttpsConnector;
import org.mule.transport.http.HttpsMessageReceiver;
import org.mule.transport.ssl.MockHandshakeCompletedEvent;
import org.mule.transport.ssl.MockSslSocket;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.resource.spi.work.Work;

import org.junit.Test;

/**
 * Test for SSL handshake timeouts. Unfortunately, there is no easy way to blackbox-test this
 * as it would require a SSLSocket implementation that could actually add arbitrary delays to
 * the SSL handshake.
 * <p/>
 * The approach chosen here is based on reflection and massive subclassing/stubbing to make things
 * work. Yes, this is hacky and fragile but this seems to be the only reasonable alternative
 * for now.
 */
public class HttpsHandshakeTimingTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHttpsHandshakeExceedsTimeout() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();

        MockSslSocket socket = new MockSslSocket();
        Work work = messageReceiver.createWork(new HttpServerConnection(socket, messageReceiver.getEndpoint().getEncoding(), (HttpConnector) messageReceiver.getConnector()));
        assertNotNull(work);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        try
        {
            // note how preRouteMessage is invoked here without a prior handshakeComplete
            // which would count down the latch that's used in HttpsWorker
            invokePreRouteMessage(work, message);
            fail();
        }
        catch (InvocationTargetException ite)
        {
            assertTrue(ite.getCause() instanceof MessagingException);
            assertTrue(ite.getCause().getMessage().contains("handshake"));
        }
    }

    @Test
    public void testHttpsHandshakeCompletesBeforeProcessingMessage() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();

        MockSslSocket socket = new MockSslSocket();
        HttpServerConnection serverConnection = new HttpServerConnection(socket, messageReceiver.getEndpoint().getEncoding(), (HttpConnector) messageReceiver.getConnector());
        Work work = messageReceiver.createWork(serverConnection);
        assertNotNull(work);

        invokeHandshakeCompleted(serverConnection, socket);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        invokePreRouteMessage(work, message);
        assertNotNull(message.<Object>getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS));
    }

    private void invokeHandshakeCompleted(HttpServerConnection serverConnection, MockSslSocket socket) throws Exception
    {
        HandshakeCompletedEvent event = new MockHandshakeCompletedEvent(socket);
        serverConnection.handshakeCompleted(event);
    }

    private void invokePreRouteMessage(Work work, MuleMessage message) throws Exception
    {
        Method preRouteMessage = work.getClass().getDeclaredMethod("preRouteMessage", MuleMessage.class);
        assertNotNull(preRouteMessage);
        preRouteMessage.setAccessible(true);
        preRouteMessage.invoke(work, new Object[] {message});
    }

    private MockHttpsMessageReceiver setupMockHttpsMessageReceiver() throws CreateException
    {
        HttpsConnector httpsConnector = new HttpsConnector(muleContext);
        httpsConnector.setSslHandshakeTimeout(1000);

        Map<String, Object> properties = Collections.emptyMap();

        InboundEndpoint inboundEndpoint = mock(InboundEndpoint.class);
        when(inboundEndpoint.getConnector()).thenReturn(httpsConnector);
        when(inboundEndpoint.getProperties()).thenReturn(properties);

        Service service = mock(Service.class);
        return new MockHttpsMessageReceiver(httpsConnector, service, inboundEndpoint);
    }

    private static class MockHttpsMessageReceiver extends HttpsMessageReceiver
    {

        public MockHttpsMessageReceiver(Connector connector, FlowConstruct flowConstruct,
                                        InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flowConstruct, endpoint);
        }

        /**
         * Open up access for unit test
         */
        @Override
        public Work createWork(HttpServerConnection httpServerConnection) throws IOException
        {
            return super.createWork(httpServerConnection);
        }
    }
}
