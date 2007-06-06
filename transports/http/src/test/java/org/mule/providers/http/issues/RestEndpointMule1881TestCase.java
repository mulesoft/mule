/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.issues;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.http.jetty.JettyConnector;
import org.mule.tck.AbstractMuleTestCase;

public class RestEndpointMule1881TestCase extends AbstractMuleTestCase
{
    
    public void testJettyRestEndpointCreation() throws Exception
    {
        MuleEndpoint ep = new MuleEndpoint("jetty:rest://localhost:8080/loanbroker", true);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);

    }

    public void testJettyHttpEndpointCreation() throws Exception
    {
        MuleEndpoint ep = new MuleEndpoint("jetty:http://localhost:8080/loanbroker", true);
        assertNotNull(ep);
        assertTrue(ep.getConnector() instanceof JettyConnector);
    }

}
