/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.issues;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.http.jetty.JettyConnector;

public class RestEndpointMule1881TestCase extends AbstractMuleTestCase
{
    
    public void testJettyRestEndpointCreation() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jetty:rest://localhost:8080/loanbroker");
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }

    public void testJettyHttpEndpointCreation() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jetty:rest://localhost:8080/loanbroker");
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }

}
