/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import com.mockobjects.dynamic.AnyConstraintMatcher;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouterTestUtils
{
    private RouterTestUtils()
    {
    }

    /** @return a mock endpoint */
    public static Mock getMockEndpoint(OutboundEndpoint toMock)
    {
        Mock mockEndpoint = MuleTestUtils.getMockOutboundEndpoint();
        mockEndpoint.matchAndReturn("getEndpointURI", toMock.getEndpointURI());
        mockEndpoint.matchAndReturn("toString", toMock.toString());
        mockEndpoint.matchAndReturn("isSynchronous", toMock.isSynchronous());
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
        } ;
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
        } ;
    }
}