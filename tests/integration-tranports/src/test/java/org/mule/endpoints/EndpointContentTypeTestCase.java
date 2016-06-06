/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoints;

import static org.junit.Assert.assertEquals;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;

import org.junit.Test;

/** Test configuration of content-type in various endpoints */
public class EndpointContentTypeTestCase  extends FunctionalTestCase
{   

    @Override
    protected String getConfigFile()
    {
        return  "content-type-setting-endpoint-configs-flow.xml";
    }

    @Test
    public void testContentType()  throws Exception
    {
        Flow flowService = muleContext.getRegistry().lookupObject("service");
        InboundEndpoint inbound = (InboundEndpoint) flowService.getMessageSource();
        assertEquals("text/xml", inbound.getMimeType());
        assertEquals("utf-8", inbound.getEncoding());
        OutboundEndpoint outbound = (OutboundEndpoint) flowService.getMessageProcessors().get(0);
        assertEquals("application/json", outbound.getMimeType());
        assertEquals("iso-8859-2", outbound.getEncoding());
        EndpointBuilder global = lookupEndpointBuilder(muleContext.getRegistry(), "global");
        InboundEndpoint created = global.buildInboundEndpoint();
        assertEquals("application/xml", created.getMimeType());
        assertEquals("iso-8859-1", created.getEncoding());
    }
}
