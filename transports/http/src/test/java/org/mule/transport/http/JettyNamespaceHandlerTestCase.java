/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.jetty.JettyHttpConnector;

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

}