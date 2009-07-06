/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.http.HttpsConnector;
import org.mule.transport.http.HttpsMessageReceiver;
import org.mule.transport.ssl.MockHandshakeCompletedEvent;
import org.mule.transport.ssl.MockSslSocket;

import com.mockobjects.dynamic.Mock;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.resource.spi.work.Work;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Test for SSL handshake timeouts. Unfortunately, there is no easy way to blackbox-test this 
 * as it would require a SSLSocket implementation that could actually add arbitrary delays to 
 * the SSL handshake.
 * <p/>
 * The approach chosen here is based on reflection and massive subclassing/stubbing to make things
 * work. Yes, this is hacky and fragile but this seems to be the only reasonable alternative
 * for now.
 */
public class HttpsHandshakeTimingTestCase extends AbstractMuleTestCase
{
    public void testHttpsHandshakeExceedsTimeout() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();
        
        MockSslSocket socket = new MockSslSocket();
        Work work = messageReceiver.createWork(socket);
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
        
    public void testHttpsHandshakeCompletesBeforeProcessingMessage() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();
        
        MockSslSocket socket = new MockSslSocket();
        Work work = messageReceiver.createWork(socket);
        assertNotNull(work);

        invokeHandshakeCompleted(work, socket);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        invokePreRouteMessage(work, message);
        assertNotNull(message.getProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS));
    }

    private void invokeHandshakeCompleted(Work work, MockSslSocket socket) throws Exception
    {
        Method handshakeCompleted = work.getClass().getDeclaredMethod("handshakeCompleted", HandshakeCompletedEvent.class);
        assertNotNull(handshakeCompleted);
        handshakeCompleted.setAccessible(true);
        HandshakeCompletedEvent event = new MockHandshakeCompletedEvent(socket);
        handshakeCompleted.invoke(work, new Object[] { event });
    }

    private void invokePreRouteMessage(Work work, MuleMessage message) throws Exception
    {
        Method preRouteMessage = work.getClass().getDeclaredMethod("preRouteMessage", MuleMessage.class);
        assertNotNull(preRouteMessage);
        preRouteMessage.setAccessible(true);
        preRouteMessage.invoke(work, new Object[] { message });
    }

    private MockHttpsMessageReceiver setupMockHttpsMessageReceiver() throws CreateException
    {
        HttpsConnector httpsConnector = new HttpsConnector();
        httpsConnector.setSslHandshakeTimeout(1000);
        
        Map properties = Collections.emptyMap();
        
        Mock mockEndpoint = new Mock(InboundEndpoint.class);
        mockEndpoint.expectAndReturn("getConnector", httpsConnector);
        mockEndpoint.expectAndReturn("getEncoding", new DefaultMuleConfiguration().getDefaultEncoding());
        mockEndpoint.expectAndReturn("getProperties", properties);
        mockEndpoint.expectAndReturn("getProperties", properties);
        InboundEndpoint inboundEndpoint = (InboundEndpoint) mockEndpoint.proxy();
        
        Mock mockService = new Mock(Service.class);
        mockService.expectAndReturn("getResponseRouter", null);
        mockService.expectAndReturn("getResponseRouter", null);
        Service service = (Service) mockService.proxy();
        
        MockHttpsMessageReceiver messageReceiver = new MockHttpsMessageReceiver(httpsConnector, service, inboundEndpoint);
        return messageReceiver;
    }

    private static class MockHttpsMessageReceiver extends HttpsMessageReceiver
    {
        public MockHttpsMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
        {
            super(connector, service, endpoint);
        }

        /**
         * Open up access for unit test
         */
        @Override
        public Work createWork(Socket socket) throws IOException
        {
            return super.createWork(socket);
        }
    }

}
