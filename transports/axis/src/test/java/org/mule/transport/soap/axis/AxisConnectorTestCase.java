/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.soap.axis.AxisConnector;

/**
 * Test case for AxisConnector's basic behaviour
 */
public class AxisConnectorTestCase extends AbstractConnectorTestCase
{

    public String getTestEndpointURI()
    {
        return "axis:http://localhost:38009/axis";
    }

    public Connector createConnector() throws Exception
    {
        AxisConnector c = new AxisConnector(muleContext);
        c.setName("axisConnector");
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

}
