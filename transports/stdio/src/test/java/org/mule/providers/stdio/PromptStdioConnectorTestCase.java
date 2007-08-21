/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stdio;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class PromptStdioConnectorTestCase extends AbstractConnectorTestCase
{

    public String getTestEndpointURI()
    {
        return "stdio://System.out";
    }

    public UMOConnector createConnector() throws Exception
    {
        UMOConnector cnn = new PromptStdioConnector();
        cnn.setName("TestStdio");
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

}
