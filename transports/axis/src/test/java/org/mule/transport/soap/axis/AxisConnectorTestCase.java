/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
