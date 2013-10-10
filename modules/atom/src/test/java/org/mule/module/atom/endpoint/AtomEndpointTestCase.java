/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.endpoint;

import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.ServiceException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.file.FileConnector;
import org.mule.transport.http.HttpPollingConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple test that endpoints get parsed correctly
 */
public class AtomEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHttpInboundEndpointCreation() throws Exception
    {
        String uri = "atom:http://blog.com/atom";
        EndpointFactory factory = muleContext.getEndpointFactory();
        InboundEndpoint in = factory.getEndpointBuilder(uri).buildInboundEndpoint();
        assertNotNull(in);
        assertEquals("atom:http", in.getEndpointURI().getFullScheme());
        assertEquals("http", in.getProtocol());
        assertTrue(in.getConnector() instanceof HttpPollingConnector);
        assertTrue(in instanceof AtomInboundEndpoint);
    }

    @Test
    public void testHttpOutboundEndpointCreation() throws Exception
    {
        String uri = "atom:http://blog.com/atom";
        EndpointFactory factory = muleContext.getEndpointFactory();
        try
        {
            factory.getEndpointBuilder(uri).buildOutboundEndpoint();
            fail("ATOM outbound endpoints are not supported");
        }
        catch (UnsupportedOperationException e)
        {
            //exprected
        }
    }

    @Test
    public void testFileInboundEndpointCreation() throws Exception
    {
        String uri = "atom:file://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();
        InboundEndpoint in = factory.getEndpointBuilder(uri).buildInboundEndpoint();
        assertNotNull(in);
        assertEquals("atom:file", in.getEndpointURI().getFullScheme());
        assertEquals("file", in.getProtocol());
        assertTrue(in.getConnector() instanceof FileConnector);
        assertTrue(in instanceof AtomInboundEndpoint);
    }

    @Test
    public void testFileOutboundEndpointCreation() throws Exception
    {
        String uri = "atom:file://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();
        try
        {
            factory.getEndpointBuilder(uri).buildOutboundEndpoint();
            fail("ATOM outbound endpoints are not supported");
        }
        catch (UnsupportedOperationException e)
        {
            //exprected
        }
    }

    @Test
    public void testXXInboundEndpointCreation() throws Exception
    {
        String uri = "atom:xxx://./src/foo";
        EndpointFactory factory = muleContext.getEndpointFactory();
        try
        {
            factory.getEndpointBuilder(uri).buildInboundEndpoint();
            fail("xxx is not a valid transport");
        }
        catch (ServiceException e)
        {
            //expected
        }
    }
}
