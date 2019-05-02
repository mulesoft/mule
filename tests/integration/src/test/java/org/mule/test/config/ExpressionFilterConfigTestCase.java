/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ExpressionFilterConfigTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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

    @Test
    public void testConfigNonBooleanReturnsFalse() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint4");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();
        assertThat(ep.getFilter(), instanceOf(ExpressionFilter.class));
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertThat(filter.isNonBooleanReturnsTrue(), is(false));

        assertThat(filter.accept(new DefaultMuleMessage("yes", muleContext)), is(false));

        // boolean values are not changed
        assertThat(filter.accept(new DefaultMuleMessage("false", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("FaLsE", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("true", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("TrUe", muleContext)), is(true));
    }

    @Test
    public void testConfigNonBooleanReturnsTrue() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint5");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();
        assertThat(ep.getFilter(), instanceOf(ExpressionFilter.class));
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertThat(filter.isNonBooleanReturnsTrue(), is(true));

        assertThat(filter.accept(new DefaultMuleMessage("yes", muleContext)), is(true));

        // boolean values are not changed
        assertThat(filter.accept(new DefaultMuleMessage("false", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("FaLsE", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("true", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("TrUe", muleContext)), is(true));
    }
}
