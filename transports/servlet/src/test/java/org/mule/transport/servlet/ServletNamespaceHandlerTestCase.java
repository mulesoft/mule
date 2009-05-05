/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.tck.FunctionalTestCase;
import org.mule.api.endpoint.EndpointBuilder;

public class ServletNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "servlet-namespace-config.xml";
    }

    public void testElements() throws Exception
    {
        ServletConnector connector =
                (ServletConnector) muleContext.getRegistry().lookupConnector("servletConnector");

        assertEquals("foo", connector.getServletUrl());

        EndpointBuilder b = muleContext.getRegistry().lookupEndpointBuilder("ep");
        assertNotNull(b);
        assertEquals("foo/bar", b.buildInboundEndpoint().getEndpointURI().getAddress());
    }

}