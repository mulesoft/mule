/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExpressionFilterConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/expression-filter-config.xml";
    }

    @Test
    public void testConfig1() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint1");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("payload-type", filter.getEvaluator());
        assertEquals("java.lang.String", filter.getExpression());
        assertNull(filter.getCustomEvaluator());
        assertFalse(filter.isNullReturnsTrue());
    }

    @Test
    public void testConfig2() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint2");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("header", filter.getEvaluator());
        assertEquals("foo=bar", filter.getExpression());
        assertNull(filter.getCustomEvaluator());
        assertTrue(filter.isNullReturnsTrue());
    }

    @Test
    public void testConfig3() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint3");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("custom", filter.getEvaluator());
        assertEquals("a.b.c", filter.getExpression());
        assertEquals("something", filter.getCustomEvaluator());
        assertFalse(filter.isNullReturnsTrue());
    }
}
