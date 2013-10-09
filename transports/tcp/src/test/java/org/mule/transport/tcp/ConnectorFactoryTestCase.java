/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConnectorFactoryTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testCreate() throws Exception
    {
        ImmutableEndpoint endpoint = 
            muleContext.getEndpointFactory().getInboundEndpoint("tcp://7877");
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertEquals("tcp://localhost:7877", endpoint.getEndpointURI().getAddress());
    }
}
