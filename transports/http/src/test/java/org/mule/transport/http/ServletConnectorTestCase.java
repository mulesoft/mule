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

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.http.servlet.ServletConnector;

public class ServletConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public Connector createConnector() throws Exception
    {
        ServletConnector c = new ServletConnector();
        c.setName("test");
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
