/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServletNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
