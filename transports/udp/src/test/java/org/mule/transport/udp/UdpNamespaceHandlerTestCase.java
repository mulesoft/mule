/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class UdpNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
