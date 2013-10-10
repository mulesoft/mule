/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.multicast;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MulticastNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "multicast-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        MulticastConnector c = (MulticastConnector)muleContext.getRegistry().lookupConnector("multicastConnector");
        assertNotNull(c);

        assertEquals(1234, c.getReceiveBufferSize());
        assertEquals(2345, c.getTimeout());
        assertEquals(3456, c.getSendBufferSize());
        assertEquals(5678, c.getTimeToLive());
        assertEquals(true, c.isBroadcast());
        assertEquals(true, c.isLoopback());
        assertEquals(false, c.isKeepSendSocketOpen());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}
