/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nameable;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

/**
 * <code>AbstractInboundEndpointNameableConnectorTestCase</code> tests common behaviour of all endpoints which are
 * identified by name
 */
public abstract class AbstractInboundEndpointNameableConnectorTestCase extends AbstractConnectorTestCase
{

    private static final String ENDPOINT1_NAME = "endpoint1";
    
    private static final String ENDPOINT2_NAME = "endpoint2";

    @Test
    public void testConnectorWithMoreThanOneListenerWithSameAddress() throws Exception
    {

        Connector connector = getConnectorAndAssert();

        Service service = getTestService("anApple", Apple.class);

        EndpointBuilder builderEndpoint1 = new EndpointURIEndpointBuilder(getTestEndpointURI(), muleContext);
        builderEndpoint1.setName(ENDPOINT1_NAME);

        InboundEndpoint endpoint1 =
                muleContext.getEndpointFactory().getInboundEndpoint(builderEndpoint1);

        EndpointBuilder builderEndpoint2 = new EndpointURIEndpointBuilder(getTestEndpointURI(), muleContext);
        builderEndpoint1.setName(ENDPOINT2_NAME);

        InboundEndpoint endpoint2 =
                muleContext.getEndpointFactory().getInboundEndpoint(builderEndpoint2);

        // In case no exception is raised, the registration of both endpoints
        // was successful (same uri, different name)
        connector.registerListener(endpoint1, getSensingNullMessageProcessor(), service);
        connector.registerListener(endpoint2, getSensingNullMessageProcessor(), service);
        
    }
    
}
