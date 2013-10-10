/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.AnyConstraintMatcher;
import com.mockobjects.dynamic.Mock;

public class RouterTestUtils
{
    private RouterTestUtils()
    {
        super();
    }

    /** @return a mock endpoint */
    public static Mock getMockEndpoint(OutboundEndpoint toMock)
    {
        Mock mockEndpoint = MuleTestUtils.getMockOutboundEndpoint();
        mockEndpoint.matchAndReturn("getEndpointURI", toMock.getEndpointURI());
        mockEndpoint.matchAndReturn("getAddress", toMock.getEndpointURI().getUri().toString());
        mockEndpoint.matchAndReturn("toString", toMock.toString());
        mockEndpoint.matchAndReturn("getExchangePattern", toMock.getExchangePattern());
        mockEndpoint.matchAndReturn("getProperties", toMock.getProperties());
        mockEndpoint.matchAndReturn("getFilter", toMock.getFilter());
        mockEndpoint.matchAndReturn("getName", toMock.getName());
        mockEndpoint.matchAndReturn("getResponseTransformers", toMock.getResponseTransformers());
        mockEndpoint.matchAndReturn("hashCode", System.identityHashCode(mockEndpoint));
        return mockEndpoint;
    }

    /** @return an object that verifies that the argument list was a single MuleEvent */
    public static AnyConstraintMatcher getArgListCheckerMuleEvent()
    {
        return new AnyConstraintMatcher()
        {
            @Override
            public boolean matches(Object[] args)
            {
                return args.length == 1 && args[0] instanceof MuleEvent;
            }
        };
    }

    /** @return an object that verifies that the argument list was a single MuleEvent */
    public static AnyConstraintMatcher getArgListCheckerFlowConstruct()
    {
        return new AnyConstraintMatcher()
        {
            @Override
            public boolean matches(Object[] args)
            {
                return args.length == 1 && (args[0] == null || args[0] instanceof FlowConstruct);
            }
        };
    }
}
