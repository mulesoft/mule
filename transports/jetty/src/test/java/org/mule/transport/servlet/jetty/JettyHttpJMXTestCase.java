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

public class JettyHttpJMXTestCase extends FunctionalTestCase
{
	
    @Override
    protected String getConfigResources()
    {
        return "jetty-http-enable-jmx-test.xml";
    }

    public void testJmx()
    {
        JettyHttpConnector conn = (JettyHttpConnector) muleContext.getRegistry().lookupConnector("jetty");
        assertEquals(1, conn.getHttpServer().getConnectors().length);
        assertTrue(conn.getHttpServer().getConnectors()[0].getStatsOn());
    }
}
