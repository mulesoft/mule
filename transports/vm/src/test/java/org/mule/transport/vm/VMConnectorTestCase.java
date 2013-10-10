/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
