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

import com.mockobjects.dynamic.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
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

    private static class MockSslSocket extends SSLSocket
    {
        public void addHandshakeCompletedListener(HandshakeCompletedListener listener)
        {
            // not needed
        }

        public boolean getEnableSessionCreation()
        {
            return false;
        }

        public String[] getEnabledCipherSuites()
        {
            return null;
        }

        public String[] getEnabledProtocols()
        {
            return null;
        }

        public boolean getNeedClientAuth()
        {
            return false;
        }

        public SSLSession getSession()
        {
            return null;
        }

        public String[] getSupportedCipherSuites()
        {
            return null;
        }

        public String[] getSupportedProtocols()
        {
            return null;
        }

        public boolean getUseClientMode()
        {
            return false;
        }

        public boolean getWantClientAuth()
        {
            return false;
        }

        public void removeHandshakeCompletedListener(HandshakeCompletedListener listener)
        {
            // not needed
        }

        public void setEnableSessionCreation(boolean flag)
        {
            // not needed
        }

        public void setEnabledCipherSuites(String[] suites)
        {
            // not needed
        }

        public void setEnabledProtocols(String[] protocols)
        {
            // not needed
        }

        public void setNeedClientAuth(boolean need)
        {
            // not needed
        }

        public void setUseClientMode(boolean mode)
        {
            // not needed
        }

        public void setWantClientAuth(boolean want)
        {
            // not needed
        }

        public void startHandshake() throws IOException
        {
            // not needed
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            return null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException
        {
            return null;
        }

        @Override
        public SocketAddress getRemoteSocketAddress()
        {
            return new InetSocketAddress("localhost", 12345);
        }
    }
    
    private static class MockHandshakeCompletedEvent extends HandshakeCompletedEvent
    {
        public MockHandshakeCompletedEvent(SSLSocket socket)
        {
            super(socket, null);
        }

        @Override
        public Certificate[] getLocalCertificates()
        {
            return new Certificate[0];
        }

        @Override
        public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException
        {
            return new Certificate[0];
        }
    }

}
