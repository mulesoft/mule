/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.xmpp.transformers.ObjectToXmppPacket;
import org.mule.transport.xmpp.transformers.XmppPacketToObject;

import org.junit.Test;

public class XmppNamespaceHandlerTestCase extends FunctionalTestCase
{
    public XmppNamespaceHandlerTestCase()
    {
        // no need to connect to the Jabber server
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "xmpp-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        XmppConnector connector = (XmppConnector) muleContext.getRegistry().lookupConnector("xmppConnector");
        assertNotNull(connector);
        assertEquals("localhost", connector.getHost());
        assertEquals(1234, connector.getPort());
        assertEquals("jabberService", connector.getServiceName());
        assertEquals("mule", connector.getUser());
        assertEquals("secret", connector.getPassword());
        assertEquals("MuleAtWork", connector.getResource());
        assertTrue(connector.isCreateAccount());
    }

    @Test
    public void testSendingMessageEndpoint() throws Exception
    {
        OutboundEndpoint endpoint = lookupOutboundEndpoint("sendingMessageEndpoint");
        assertEquals("xmpp://MESSAGE/recipient@jabberhost", endpoint.getEndpointURI().toString());
        assertEquals("TheSubject", endpoint.getProperty(XmppConnector.XMPP_SUBJECT));
    }

    @Test
    public void testReceivingChatEndpoint() throws Exception
    {
        InboundEndpoint endpoint = lookupInboundEndpoint("receivingChatEndpoint");
        assertEquals("xmpp://CHAT/sender@jabberhost", endpoint.getEndpointURI().toString());
    }

    @Test
    public void testTransformers() throws Exception
    {
        Transformer transformer = lookupTransformer("ObjectToXmpp");
        assertNotNull(transformer);
        assertEquals(ObjectToXmppPacket.class, transformer.getClass());

        transformer = lookupTransformer("XmppToObject");
        assertNotNull(transformer);
        assertEquals(XmppPacketToObject.class, transformer.getClass());
    }

    private OutboundEndpoint lookupOutboundEndpoint(String endpointName) throws Exception
    {
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(endpointName);
        return endpointBuilder.buildOutboundEndpoint();
    }

    private InboundEndpoint lookupInboundEndpoint(String endpointName) throws Exception
    {
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(endpointName);
        return endpointBuilder.buildInboundEndpoint();
    }

    private EndpointBuilder lookupEndpointBuilder(String endpointName)
    {
        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(endpointName);
        assertNotNull(endpointBuilder);
        return endpointBuilder;
    }

    private Transformer lookupTransformer(String transformerName)
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(transformerName);
        assertNotNull(transformer);
        return transformer;
    }
}
