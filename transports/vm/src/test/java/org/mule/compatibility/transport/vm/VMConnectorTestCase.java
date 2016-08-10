/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractConnectorTestCase;
import org.mule.compatibility.transport.vm.VMConnector;

public class VMConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        VMConnector conn = new VMConnector(muleContext);
        conn.setName("TestVM");
        return conn;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "vm://test.queue";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return TEST_MESSAGE;
    }
}
