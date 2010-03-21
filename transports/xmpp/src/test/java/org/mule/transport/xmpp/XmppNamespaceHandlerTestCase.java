/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.xmpp.transformers.ObjectToXmppPacket;
import org.mule.transport.xmpp.transformers.XmppPacketToObject;

public class XmppNamespaceHandlerTestCase extends FunctionalTestCase
{
    public XmppNamespaceHandlerTestCase()
    {
        super();
        
        // no need to connect to the Jabber server
        setStartContext(false);
    }
    
    @Override
    protected String getConfigResources()
    {
        return "xmpp-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        XmppConnector connector = (XmppConnector) muleContext.getRegistry().lookupConnector("xmppConnector");
        assertNotNull(connector);
        assertEquals("localhost", connector.getHost());
        assertEquals(1234, connector.getPort());
        assertEquals("mule", connector.getUser());
        assertEquals("secret", connector.getPassword());
        assertEquals("MuleAtWork", connector.getResource());
        assertTrue(connector.isCreateAccount());
    }

    public void testSendingMessageEndpoint() throws Exception
    {
        OutboundEndpoint endpoint = lookupOutboundEndpoint("sendingMessageEndpoint");
        assertEquals("xmpp://MESSAGE/recipient@jabberhost", endpoint.getEndpointURI().toString());
        assertEquals("TheSubject", endpoint.getProperty(XmppConnector.XMPP_SUBJECT));
    }
    
    public void testReceivingChatEndpoint() throws Exception
    {
        InboundEndpoint endpoint = lookupInboundEndpoint("receivingChatEndpoint");
        assertEquals("xmpp://CHAT/sender@jabberhost", endpoint.getEndpointURI().toString());
    }
    
    public void testTransformers() throws Exception
    {
        Transformer transformer = lookupTransformer("ObjectToXmpp");
        assertNotNull(transformer);
        assertEquals(ObjectToXmppPacket.class, transformer.getClass());

        transformer = lookupTransformer("XmppToObject");
        assertNotNull(transformer);
        assertEquals(XmppPacketToObject.class, transformer.getClass());
    }
    
    private OutboundEndpoint lookupOutboundEndpoint(String name) throws Exception
    {
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(name);
        return endpointBuilder.buildOutboundEndpoint();
    }

    private InboundEndpoint lookupInboundEndpoint(String name) throws Exception
    {
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(name);
        return endpointBuilder.buildInboundEndpoint();
    }

    private EndpointBuilder lookupEndpointBuilder(String name)
    {
        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(name);
        assertNotNull(endpointBuilder);
        return endpointBuilder;
    }
    
    private Transformer lookupTransformer(String name)
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(name);
        assertNotNull(transformer);
        return transformer;
    }
}
