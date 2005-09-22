/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.Arrays;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class TcpSyncTestCase extends NamedTestCase
{

    private static final String endpointUri = "tcp://localhost:4544";

    protected void setUp() throws Exception
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("tcp-sync.xml");
    }

    protected UMOMessage send(Object payload) throws Exception
    {
        UMOMessage message = new MuleMessage(payload);
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(new MuleEndpointURI(endpointUri),
                                                                      UMOEndpoint.ENDPOINT_TYPE_SENDER);
        MuleSession session = new MuleSession();
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        event.setTimeout(60000);
        return event.getSession().sendEvent(event);
    }

    public void testSendString() throws Exception
    {
        UMOMessage message = send("data");
        assertNotNull(message);
        String response = message.getPayloadAsString();
        assertEquals("data", response);
    }

    public void testSyncResponseOfBufferSize() throws Exception
    {
        TcpConnector tcp = (TcpConnector) MuleManager.getInstance().lookupConnector("tcpConnector");
        byte[] data = new byte[tcp.getBufferSize()];
        UMOMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

    public void testSyncResponseVeryBig() throws Exception
    {
        byte[] data = new byte[1024 * 1024];
        UMOMessage message = send(data);
        assertNotNull(message);
        byte[] response = message.getPayloadAsBytes();
        assertEquals(data.length, response.length);
        assertTrue(Arrays.equals(data, response));
    }

}
