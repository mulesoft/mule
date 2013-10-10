/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
