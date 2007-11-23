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
import org.mule.umo.endpoint.UMOEndpointBuilder;
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
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpoint("Endpoint");
        // Null expected because lookupEndpoint does not create endpoints from global endpoint name.        
        assertNull(endpoint1);

        UMOEndpointBuilder endpointBuiler = managementContext.getRegistry().lookupEndpointBuilder("Endpoint");      
        // There should however be an endpoint builder with this id/name
        assertNotNull(endpointBuiler);
        
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        // Null expected because lookupEndpoint does not create endpoints from uri's.
        assertNull(endpoint2);
    }

    public void testGetOutboundEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    public void testGetInboundEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    public void testGetResponseEndpoint() throws UMOException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpointFactory().getResponseEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpointFactory().getResponseEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    private void assertEndpointOk(UMOImmutableEndpoint endpoint)
    {
        assertNotNull("Endpoint is null", endpoint);
        UMOConnector connector = endpoint.getConnector();
        assertTrue("Connector not AXIS", connector instanceof AxisConnector);
    }

}
