/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UdpNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "udp-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        UdpConnector c = (UdpConnector)muleContext.getRegistry().lookupConnector("udpConnector");
        assertNotNull(c);

        assertEquals(1234, c.getReceiveBufferSize());
        assertEquals(2345, c.getTimeout());
        assertEquals(3456, c.getSendBufferSize());
        assertEquals(true, c.isBroadcast());
        assertEquals(false, c.isKeepSendSocketOpen());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}
