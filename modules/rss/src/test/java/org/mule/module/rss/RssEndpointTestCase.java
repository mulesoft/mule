/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.ServiceException;
import org.mule.module.rss.endpoint.RssInboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.file.FileConnector;
import org.mule.transport.http.HttpPollingConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RssEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHttpInboundEndpointCreation() throws Exception
    {
        String uri = "rss:http://blog.com/rss";
        EndpointFactory factory = muleContext.getEndpointFactory();
        InboundEndpoint in = factory.getEndpointBuilder(uri).buildInboundEndpoint();
        assertNotNull(in);
        assertEquals("rss:http", in.getEndpointURI().getFullScheme());
        assertEquals("http", in.getProtocol());
        assertTrue(in.getConnector() instanceof HttpPollingConnector);
        assertTrue(in instanceof RssInboundEndpoint);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHttpOutboundEndpointCreation() throws Exception
    {
        String uri = "rss:http://blog.com/rss";
        EndpointFactory factory = muleContext.getEndpointFactory();

        factory.getEndpointBuilder(uri).buildOutboundEndpoint();
    }

    @Test
    public void testFileInboundEndpointCreation() throws Exception
    {
        String uri = "rss:file://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();
        InboundEndpoint in = factory.getEndpointBuilder(uri).buildInboundEndpoint();
        assertNotNull(in);
        assertEquals("rss:file", in.getEndpointURI().getFullScheme());
        assertEquals("file", in.getProtocol());
        assertTrue(in.getConnector() instanceof FileConnector);
        assertTrue(in instanceof RssInboundEndpoint);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFileOutboundEndpointCreation() throws Exception
    {
        String uri = "rss:file://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();

        factory.getEndpointBuilder(uri).buildOutboundEndpoint();
    }

    @Test(expected = ServiceException.class)
    public void testXXInboundEndpointCreation() throws Exception
    {
        String uri = "rss:xxx://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();

        factory.getEndpointBuilder(uri).buildInboundEndpoint();
    }
}
