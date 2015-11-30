/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.endpoint.OutboundEndpoint;

public class RouterTestUtils
{
    private RouterTestUtils()
    {
        super();
    }

    public static OutboundEndpoint createMockEndpoint(OutboundEndpoint toMock)
    {
        OutboundEndpoint endpoint = mock(OutboundEndpoint.class);
        when(endpoint.getEndpointURI()).thenReturn(toMock.getEndpointURI());
        when(endpoint.getAddress()).thenReturn(toMock.getAddress());
        when(endpoint.toString()).thenReturn(toMock.toString());
        when(endpoint.getExchangePattern()).thenReturn(toMock.getExchangePattern());
        when(endpoint.getProperties()).thenReturn(toMock.getProperties());
        when(endpoint.getFilter()).thenReturn(toMock.getFilter());
        when(endpoint.getName()).thenReturn(toMock.getName());
        when(endpoint.getResponseMessageProcessors()).thenReturn(toMock.getResponseMessageProcessors());
        return endpoint;
    }
}
