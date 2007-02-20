/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
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

    public UMOConnector getConnector() throws Exception
    {
        XFireConnector c = new XFireConnector();
        c.initialise(managementContext);
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }
}
