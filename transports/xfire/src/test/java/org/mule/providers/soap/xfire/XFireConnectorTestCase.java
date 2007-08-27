/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class XFireConnectorTestCase extends AbstractConnectorTestCase
{

    public String getTestEndpointURI()
    {
        return "xfire:http://localhost:38009/xfire";
    }

    // @Override
    public UMOConnector createConnector() throws Exception
    {
        XFireConnector c = new XFireConnector();
        c.setName("xfireConnector");
        c.setManagementContext(managementContext);
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

}
