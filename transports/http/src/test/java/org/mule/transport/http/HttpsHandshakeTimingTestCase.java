/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.ssl.MockHandshakeCompletedEvent;
import org.mule.transport.ssl.MockSslSocket;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;

import javax.net.ssl.HandshakeCompletedEvent;

import org.junit.Test;
import org.mockito.Answers;

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

    @Test(expected = MessagingException.class)
    public void testHttpsHandshakeExceedsTimeout() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();

        MockSslSocket socket = new MockSslSocket();
        HttpMessageProcessTemplate messageProcessTemplate = messageReceiver.createMessageProcessTemplate(new HttpServerConnection(socket, messageReceiver.getEndpoint().getEncoding(), (HttpConnector) messageReceiver.getConnector()));

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        messageProcessTemplate.beforeRouteEvent(getTestEvent(message));
    }

    @Test
    public void testHttpsHandshakeCompletesBeforeProcessingMessage() throws Exception
    {
        MockHttpsMessageReceiver messageReceiver = setupMockHttpsMessageReceiver();

        MockSslSocket socket = new MockSslSocket();
        socket.setInputStream(new ByteArrayInputStream("GET /path/to/file/index.html HTTP/1.0\n\n\n".getBytes()));
        HttpServerConnection serverConnection = new HttpServerConnection(socket, "utf-8", (HttpConnector) messageReceiver.getConnector());
        HttpMessageProcessTemplate messageContext = messageReceiver.createMessageProcessTemplate(serverConnection);

        invokeHandshakeCompleted(serverConnection, socket);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        messageContext.acquireMessage();
        serverConnection.readRequest();
        MuleEvent muleEvent = messageContext.beforeRouteEvent(getTestEvent(message));
        assertNotNull(muleEvent.getMessage().<Object>getOutboundProperty(HttpsConnector.LOCAL_CERTIFICATES));
        assertNotNull(muleEvent.getMessage().<Object>getOutboundProperty(HttpsConnector.PEER_CERTIFICATES));
    }

    private void invokeHandshakeCompleted(HttpServerConnection serverConnection, MockSslSocket socket) throws Exception
    {
        HandshakeCompletedEvent event = new MockHandshakeCompletedEvent(socket);
        serverConnection.handshakeCompleted(event);
    }

    private MockHttpsMessageReceiver setupMockHttpsMessageReceiver() throws CreateException
    {
        HttpsConnector httpsConnector = new HttpsConnector(muleContext);
        httpsConnector.setSslHandshakeTimeout(1000);

        Map<String, Object> properties = Collections.emptyMap();

        InboundEndpoint inboundEndpoint = mock(InboundEndpoint.class, Answers.RETURNS_DEEP_STUBS.get());
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
    }
}
