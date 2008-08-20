/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.tck.FunctionalTestCase;

public class JettyNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jetty-namespace-config.xml";
    }

    public void testConnectorProperties()
    {
        JettyHttpConnector connector = (JettyHttpConnector) muleContext.getRegistry().lookupConnector("jettyConnector");
        assertNotNull(connector.getConfigFile());
        assertEquals("jetty-config.xml", connector.getConfigFile());
    }

    /* See MULE-3603
    public void testEndpointConfig() throws MuleException
    {
        InboundEndpoint endpoint = 
            muleContext.getRegistry().lookupEndpointBuilder("endpoint").buildInboundEndpoint();
        assertNotNull(endpoint);
        // is the following test correct? 
        // Can't test it now, the config for the endpoint isn't even valid
        assertEquals("http://localhost:60223/", endpoint.getEndpointURI().getAddress());
    }
    */

}