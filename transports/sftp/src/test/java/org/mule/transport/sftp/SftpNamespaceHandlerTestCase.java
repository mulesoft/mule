/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class SftpNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "sftp-namespace-config.xml";
    }

    @Test
    public void testSftpConnectorConfig() throws Exception
    {
        SftpConnector c = (SftpConnector) muleContext.getRegistry().lookupConnector("sftpConnector");
        assertNotNull(c);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        assertTrue(c.isAutoDelete());
        assertEquals(c.getPollingFrequency(), 15000);
        assertEquals(1234, c.getFileAge());
        assertEquals("uploading", c.getTempDirOutbound());
        assertEquals(42, c.getMaxConnectionPoolSize());
    }

    @Test
    public void testSftpEndpointConfig() throws Exception
    {
        ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject("inboundEndpoint");
        SftpConnector connector = (SftpConnector) inboundEndpoint.getConnector();
        Object[] receivers = connector.getReceivers().values().toArray();
        SftpMessageReceiver receiver = (SftpMessageReceiver) receivers[0];
        assertEquals(10000, receiver.getFrequency());
    }
}
