/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

public class VMConnectorTestCase extends AbstractConnectorTestCase
{
    public Connector createConnector() throws Exception
    {
        VMConnector conn = new VMConnector(muleContext);
        conn.setName("TestVM");
        return conn;
    }

    public String getTestEndpointURI()
    {
        return "vm://test.queue";
    }

    public Object getValidMessage() throws Exception
    {
        return TEST_MESSAGE;
    }
}
