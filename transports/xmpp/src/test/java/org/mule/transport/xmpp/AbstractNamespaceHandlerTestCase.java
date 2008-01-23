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

import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.xmpp.XmppConnector;

public abstract class AbstractNamespaceHandlerTestCase extends FunctionalTestCase
{

    private String protocolName;

    public AbstractNamespaceHandlerTestCase(String protocolName)
    {
        this.protocolName = protocolName;
    }

    protected String getConfigResources()
    {
        return protocolName + "-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        XmppConnector connector = (XmppConnector) muleContext.getRegistry().lookupConnector(protocolName + "Connector");
        assertNotNull(connector);
    }

    public void testEndpoints() throws EndpointException, InitialisationException
    {
        ImmutableEndpoint simpleEndpoint = muleContext.getRegistry()
            .lookupEndpointBuilder("simpleEndpoint")
            .buildOutboundEndpoint();
        assertEquals(protocolName + "://mule:secret@localhost:1234/recipient", simpleEndpoint.getEndpointURI()
            .toString());

        ImmutableEndpoint groupChatEndpoint = muleContext.getRegistry()
            .lookupEndpointBuilder("groupChatEndpoint")
            .buildOutboundEndpoint();
        assertEquals(protocolName + "://mule:secret@localhost:1234/recipient", groupChatEndpoint.getEndpointURI()
            .toString());
        assertNotNull(groupChatEndpoint.getProperty("groupChat"));
        assertTrue(groupChatEndpoint.getProperty("groupChat") instanceof String);
        assertEquals("true", groupChatEndpoint.getProperty("groupChat"));
        assertNotNull(groupChatEndpoint.getProperty("nickname"));
        assertTrue(groupChatEndpoint.getProperty("nickname") instanceof String);
        assertEquals("bob", groupChatEndpoint.getProperty("nickname"));
    }

}
