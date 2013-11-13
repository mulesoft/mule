/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.multicast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MulticastNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
