/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.issues;

import org.mule.providers.soap.axis.AxisConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

public class EndpointRetrievalMule2021TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "endpoint-retrieval-mule-2021-test.xml";
    }

    public void testLookupEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpoint("Endpoint",managementContext);
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpoint("axis:http://localhost:18081/mule/Service?method=toString",managementContext);
        // Null expected because lookupEndpoint does not create endpoints.
        assertEquals(null,endpoint2);
    }
    
    public void testLookupOutboundEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupOutboundEndpoint("Endpoint",managementContext);
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupOutboundEndpoint("axis:http://localhost:18081/mule/Service?method=toString",managementContext);
        assertEndpointOk(endpoint2);
    }

    public void testLookupInboundEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupInboundEndpoint("Endpoint",managementContext);
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupInboundEndpoint("axis:http://localhost:18081/mule/Service?method=toString",managementContext);
        assertEndpointOk(endpoint2);
    }

    public void testLookupResponseEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupResponseEndpoint("Endpoint",managementContext);
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupResponseEndpoint("axis:http://localhost:18081/mule/Service?method=toString",managementContext);
        assertEndpointOk(endpoint2);
    }

    private void assertEndpointOk(UMOImmutableEndpoint endpoint)
    {
        assertNotNull("Endpoint is null", endpoint);
        UMOConnector connector = endpoint.getConnector();
        assertTrue("Connector not AXIS", connector instanceof AxisConnector);
    }

}
