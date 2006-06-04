/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.http.servlet.ServletConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ServletConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        ServletConnector c = new ServletConnector();
        c.setName("test");
        c.initialise();
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

    public void testConnectorMessageDispatcher() throws Exception
    {
        // there is no dispatcher for the servlet connector
    }

}
