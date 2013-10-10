/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SftpConnectorFactoryTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCreateFromFactory() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            getEndpointURI());
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof SftpConnector);
        assertEquals(getEndpointURI(), endpoint.getEndpointURI().getAddress());
    }

    public String getEndpointURI()
    {
        return "sftp://ms/data";
    }
}
