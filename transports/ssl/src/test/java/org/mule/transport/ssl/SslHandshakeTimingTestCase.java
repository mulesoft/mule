/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

/**
 * Whitebox test for the SSL latch in SslMessageReceiver. The use of reflection here is hacky
 * but the alternative would be stubbing large parts of the JSSE classes in order to influence
 * timing while establishing the SSL handshake (wich sounds even hackier than this test).
 */
public class SslHandshakeTimingTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testSslHandshakeTimeout() throws Exception
    {
        SslMessageReceiver receiver = setupMockSslMessageReciever();

        // note how we call preRoute without a prior handshakeCompleted ... this must
        // run into a timeout
        try
        {
            MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
            callPreRoute(receiver, message);
            fail();
        }
        catch (InvocationTargetException ite)
        {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof IllegalStateException);
        }
    }

    @Test
    public void testSslHandshakeSuccessful() throws Exception
    {
        SslMessageReceiver receiver = setupMockSslMessageReciever();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        receiver.handshakeCompleted(new MockHandshakeCompletedEvent());
        callPreRoute(receiver, message);

        assertNotNull(message.getOutboundProperty(SslConnector.PEER_CERTIFICATES));
        assertNotNull(message.getOutboundProperty(SslConnector.LOCAL_CERTIFICATES));
    }

    private SslMessageReceiver setupMockSslMessageReciever() throws Exception
    {
        SslConnector connector = new SslConnector(muleContext);
        connector.setSslHandshakeTimeout(1000);

        Map<String, Object> properties = Collections.emptyMap();

        InboundEndpoint endpoint = mock(InboundEndpoint.class);
        when(endpoint.getProperties()).thenReturn(properties);
        when(endpoint.getConnector()).thenReturn(connector);
        when(endpoint.getEncoding()).thenReturn(new DefaultMuleConfiguration().getDefaultEncoding());
        when(endpoint.getMuleContext()).thenReturn(muleContext);

        Service service = mock(Service.class);
        return new SslMessageReceiver(connector, service, endpoint);
    }

    private void callPreRoute(SslMessageReceiver receiver, MuleMessage message) throws Exception
    {
        Method preRouteMessage = receiver.getClass().getDeclaredMethod("preRoute", MuleMessage.class);
        assertNotNull(preRouteMessage);
        preRouteMessage.setAccessible(true);

        preRouteMessage.invoke(receiver, new Object[] { message });
    }
}
