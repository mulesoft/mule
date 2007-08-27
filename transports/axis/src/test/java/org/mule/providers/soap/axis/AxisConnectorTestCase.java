/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * Test case for AxisConnector's basic behaviour
 */
public class AxisConnectorTestCase extends AbstractConnectorTestCase
{

    public String getTestEndpointURI()
    {
        return "axis:http://localhost:38009/axis";
    }

    public UMOConnector createConnector() throws Exception
    {
        AxisConnector c = new AxisConnector();
        c.setName("axisConnector");
        c.setManagementContext(managementContext);
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

}
