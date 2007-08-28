/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.http.jetty.JettyConnector;

public class JettyNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{

    public JettyNamespaceHandlerTestCase()
    {
        super("jetty");
    }

    public void testConnectorProperties()
    {
        JettyConnector connector =
                (JettyConnector) managementContext.getRegistry().lookupConnector("jettyConnector");
        testBasicProperties(connector);
    }

}