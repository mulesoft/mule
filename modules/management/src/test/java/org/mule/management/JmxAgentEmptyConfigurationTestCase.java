/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.tck.junit4.FunctionalTestCase;

import java.net.ConnectException;
import java.net.Socket;

import org.junit.Test;

public class JmxAgentEmptyConfigurationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "empty-management-config.xml";
    }

    @Test(expected = ConnectException.class)
    public void testDefaultJmxAgent() throws Exception
    {
        FixedHostRmiClientSocketFactory rmiSocketFactory = new FixedHostRmiClientSocketFactory();
        Socket socket = rmiSocketFactory.createSocket("localhost", 1099);
        socket.close();
    }
}
