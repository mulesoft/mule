/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.ibeans.annotations.AbstractIBeansTestCase;
import org.mule.transport.ibean.IBeansConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IBeansConnectorFactoryTestCase extends AbstractIBeansTestCase
{
    @Test
    public void testCreateFromFactory() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(getEndpointURI());
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof IBeansConnector);
        assertEquals(getEndpointURI(), endpoint.getEndpointURI().toString());
    }

    public String getEndpointURI()
    {
        return "ibean://hostip.getHostInfo";
    }
}
