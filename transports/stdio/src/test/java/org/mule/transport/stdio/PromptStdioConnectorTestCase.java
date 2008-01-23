/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.stdio;

import org.mule.api.transport.Connector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.transport.stdio.PromptStdioConnector;

public class PromptStdioConnectorTestCase extends AbstractConnectorTestCase
{

    public String getTestEndpointURI()
    {
        return "stdio://System.out";
    }

    public Connector createConnector() throws Exception
    {
        Connector cnn = new PromptStdioConnector();
        cnn.setName("TestStdio");
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

}
