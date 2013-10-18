/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
