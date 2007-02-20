/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.http.servlet.ServletConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class ServletConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
    {
        ServletConnector c = new ServletConnector();
        c.setName("test");
        c.initialise(managementContext);
        return c;
    }

    public String getTestEndpointURI()
    {
        return "servlet://testServlet";
    }

    public Object getValidMessage() throws Exception
    {
        return HttpRequestMessageAdapterTestCase.getMockRequest("test message");
    }

    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        // there is no DispatcherFactory for the servlet connector
    }

    public void testConnectorMessageDispatcher() throws Exception
    {
        // therefore we have no dispatchers
    }

}
