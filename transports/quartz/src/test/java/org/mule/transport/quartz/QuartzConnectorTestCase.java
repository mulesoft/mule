/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

public class QuartzConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        QuartzConnector c = new QuartzConnector(muleContext);
        c.setName("QuartzConnector");
        return c;
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "test";
    }

    @Override
    public String getTestEndpointURI()
    {
        return "quartz:/myService?repeatInterval=1000";
    }
}
