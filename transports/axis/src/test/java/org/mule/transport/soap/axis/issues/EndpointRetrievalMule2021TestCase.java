/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis.issues;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.soap.axis.AxisConnector;

public class EndpointRetrievalMule2021TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "endpoint-retrieval-mule-2021-test.xml";
    }

    public void testLookupEndpoint() throws MuleException
    {
        Object endpoint1 = muleContext.getRegistry().lookupObject("Endpoint");
        // This returns the builder rather than the endpoint
        assertTrue(endpoint1 instanceof EndpointBuilder);
        assertFalse(endpoint1 instanceof ImmutableEndpoint);

        EndpointBuilder endpointBuiler = muleContext.getRegistry().lookupEndpointBuilder("Endpoint");
        // There should however be an endpoint builder with this id/name
        assertNotNull(endpointBuiler);

        ImmutableEndpoint endpoint2 = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(
            "axis:http://localhost:18081/mule/Service?method=toString");
        // Null expected because lookupEndpoint does not create endpoints from uri's.
        assertNull(endpoint2);
    }

    public void testGetOutboundEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    public void testGetInboundEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    public void testGetResponseEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "axis:http://localhost:18081/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    private void assertEndpointOk(ImmutableEndpoint endpoint)
    {
        assertNotNull("Endpoint is null", endpoint);
        Connector connector = endpoint.getConnector();
        assertTrue("Connector not AXIS", connector instanceof AxisConnector);
    }

}
