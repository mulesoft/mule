/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management;

import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;

public class JmxAgentDefaultConfigurationWithRMITestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "default-with-rmi-management-config.xml";
    }

    @Test
    public void testDefaultJmxAgent() throws Exception
    {
        FixedHostRmiClientSocketFactory rmiSocketFactory = new FixedHostRmiClientSocketFactory();
        Socket socket = rmiSocketFactory.createSocket("localhost", port.getNumber());
        socket.close();
    }

}
