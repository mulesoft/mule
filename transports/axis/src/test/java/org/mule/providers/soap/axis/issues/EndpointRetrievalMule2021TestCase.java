/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.issues;

import org.mule.providers.soap.axis.AxisConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

public class EndpointRetrievalMule2021TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "endpoint-retrieval-mule-2021-test.xml";
    }

    public void testGetEndpointFromUri() throws UMOException
    {
        UMOEndpoint endpoint1 = managementContext.getRegistry().getEndpointFromUri("Endpoint");
        assertEndpointOk(endpoint1);
        UMOEndpointURI uri = endpoint1.getEndpointURI();
        UMOEndpoint endpoint2 = managementContext.getRegistry().getEndpointFromUri(uri);
        assertEndpointOk(endpoint2);
    }

    public void testGetEndpointFromName() throws UMOException
    {
        UMOEndpoint endpoint1 = managementContext.getRegistry().getEndpointFromName("Endpoint");
        assertEndpointOk(endpoint1);
        UMOEndpointURI uri = endpoint1.getEndpointURI();
        UMOEndpoint endpoint2 = managementContext.getRegistry().getEndpointFromUri(uri);
        assertEndpointOk(endpoint2);
    }

    public void testGetOrCreateEndpointForUri() throws UMOException
    {
        UMOEndpoint endpoint1 = managementContext.getRegistry().getEndpointFromUri("Endpoint");
        assertEndpointOk(endpoint1);
        UMOEndpointURI uri = endpoint1.getEndpointURI();
        UMOEndpoint endpoint2 = managementContext.getRegistry().getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertEndpointOk(endpoint2);
    }

    public void testGetOrCreateEndpointForUriFromName() throws UMOException
    {
        UMOEndpoint endpoint1 = managementContext.getRegistry().getEndpointFromName("Endpoint");
        assertEndpointOk(endpoint1);
        UMOEndpointURI uri = endpoint1.getEndpointURI();
        UMOEndpoint endpoint2 = managementContext.getRegistry().getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertEndpointOk(endpoint2);
    }

    private void assertEndpointOk(UMOEndpoint endpoint)
    {
        assertNotNull("Endpoint is null", endpoint);
        UMOConnector connector = endpoint.getConnector();
        assertTrue("Connector not AXIS", connector instanceof AxisConnector);
    }

}
