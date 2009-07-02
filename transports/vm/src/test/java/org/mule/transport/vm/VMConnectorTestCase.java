/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.DefaultMuleMessage;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

public class VMConnectorTestCase extends AbstractConnectorTestCase
{
    public Connector createConnector() throws Exception
    {
        VMConnector conn = new VMConnector();
        conn.setName("TestVM");
        return conn;
    }

    public String getTestEndpointURI()
    {
        return "vm://test.queue";
    }

    public Object getValidMessage() throws Exception
    {
        return new DefaultMuleMessage("TestMessage", muleContext);
    }

}
