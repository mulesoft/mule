/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.impl.MuleMessage;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class VMConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector createConnector() throws Exception
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
        return new MuleMessage("TestMessage");
    }

}
