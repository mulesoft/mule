/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.issues;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.soap.axis.AxisConnector;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EndpointRetrievalMule2021TestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "endpoint-retrieval-mule-2021-test.xml";
    }

    @Test
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
            "axis:http://localhost:" + dynamicPort.getNumber() + "/mule/Service?method=toString");
        // Null expected because lookupEndpoint does not create endpoints from uri's.
        assertNull(endpoint2);
    }

    @Test
    public void testGetOutboundEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getEndpointFactory().getOutboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getEndpointFactory().getOutboundEndpoint(
            "axis:http://localhost:" + dynamicPort.getNumber() + "/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    @Test
    public void testGetInboundEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getEndpointFactory().getInboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getEndpointFactory().getInboundEndpoint(
            "axis:http://localhost:" + dynamicPort.getNumber() + "/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    @Test
    public void testGetResponseEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getEndpointFactory().getInboundEndpoint(
            "Endpoint");
        assertEndpointOk(endpoint1);
        ImmutableEndpoint endpoint2 = muleContext.getEndpointFactory().getInboundEndpoint(
            "axis:http://localhost:" + dynamicPort.getNumber() + "/mule/Service?method=toString");
        assertEndpointOk(endpoint2);
    }

    private void assertEndpointOk(ImmutableEndpoint endpoint)
    {
        assertNotNull("Endpoint is null", endpoint);
        Connector connector = endpoint.getConnector();
        assertTrue("Connector not AXIS", connector instanceof AxisConnector);
    }

}
