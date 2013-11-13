/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ServletNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "servlet-namespace-config.xml";
    }

    @Test
    public void testElements() throws Exception
    {
        ServletConnector connector =
                (ServletConnector) muleContext.getRegistry().lookupConnector("servletConnector");

        assertEquals("foo", connector.getServletUrl());
        EndpointBuilder b = muleContext.getRegistry().lookupEndpointBuilder("ep");
        assertNotNull(b);
        assertEquals("foo/bar", b.buildInboundEndpoint().getEndpointURI().getAddress());
        assertTrue(connector.isUseCachedHttpServletRequest());
    }
}
