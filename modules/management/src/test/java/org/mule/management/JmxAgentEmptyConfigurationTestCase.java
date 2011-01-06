/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management;

import java.net.ConnectException;
import java.net.Socket;

import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.tck.FunctionalTestCase;

public class JmxAgentEmptyConfigurationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "empty-management-config.xml";
    }

    public void testDefaultJmxAgent() throws Exception
    {
        FixedHostRmiClientSocketFactory rmiSocketFactory = new FixedHostRmiClientSocketFactory();
        try
        {
            Socket socket = rmiSocketFactory.createSocket("localhost", 1099);
            socket.close();
            fail("Should not connect");
        }
        catch (ConnectException e)
        {
            // expected behavior
        }
    }
}
