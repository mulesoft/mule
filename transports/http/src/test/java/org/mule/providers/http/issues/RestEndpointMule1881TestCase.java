/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.issues;

import org.mule.providers.http.jetty.JettyConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class RestEndpointMule1881TestCase extends AbstractMuleTestCase
{
    
    public void testJettyRestEndpointCreation() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().createInboundEndpoint(
            "jetty:rest://localhost:8080/loanbroker", managementContext);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }

    public void testJettyHttpEndpointCreation() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().createInboundEndpoint(
            "jetty:rest://localhost:8080/loanbroker", managementContext);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }

}
