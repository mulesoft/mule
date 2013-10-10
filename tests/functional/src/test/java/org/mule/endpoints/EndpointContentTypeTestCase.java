/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoints;

import static org.junit.Assert.assertEquals;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/** Test configuration of content-type in various endpoints */
public class EndpointContentTypeTestCase  extends AbstractServiceAndFlowTestCase
{   
    public EndpointContentTypeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "content-type-setting-endpoint-configs-service.xml"},
            {ConfigVariant.FLOW, "content-type-setting-endpoint-configs-flow.xml"}
        });
    }      
    
    @Test
    public void testContentType()  throws Exception
    {
        InboundEndpoint inbound = muleContext.getRegistry().lookupObject("inbound");
        assertEquals("text/xml", inbound.getMimeType());
        assertEquals("utf-8", inbound.getEncoding());
        OutboundEndpoint outbound = muleContext.getRegistry().lookupObject("outbound");
        assertEquals("application/json", outbound.getMimeType());
        assertEquals("iso-8859-2", outbound.getEncoding());
        EndpointBuilder global = muleContext.getRegistry().lookupEndpointBuilder("global");
        InboundEndpoint created = global.buildInboundEndpoint();
        assertEquals("application/xml", created.getMimeType());
        assertEquals("iso-8859-1", created.getEncoding());
    }
}
