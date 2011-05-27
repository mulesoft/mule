/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.config;

import org.mule.tck.DynamicPortTestCase;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxAgentAuthenticationTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jmx-authentication-config.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testAccessJmxServerWithoutCredentials() throws Exception
    {
        try
        {
            JMXServiceURL serviceUrl = createServiceUrl();
            JMXConnectorFactory.connect(serviceUrl);
            fail("Accessing a secured jmx server without credentials must fail");
        }
        catch (SecurityException se)
        {
            // this one is expected
        }
    }

    public void testAccessJmxServerWithValidCredentials() throws Exception
    {
        JMXConnector connector = connectToJmx("jsmith", "foo");

        // to test the connection, access an MBean that is present by default
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName name = new ObjectName("java.lang:type=Runtime");
        ObjectInstance instance = connection.getObjectInstance(name);
        assertNotNull(instance);
    }

    public void testAccessJmxServerWithInvalidCredentials() throws Exception
    {
        try
        {
            connectToJmx("invalid", "user");
            fail("Accessing a secured jmx server with invalid credentials must fail");
        }
        catch (SecurityException se)
        {
            // this one was expected
        }
    }

    private JMXConnector connectToJmx(String user, String password) throws IOException
    {
        JMXServiceURL serviceUrl = createServiceUrl();

        String[] authToken = new String[] { user, password };
        Map<String, ?> environment = Collections.singletonMap(JMXConnector.CREDENTIALS, authToken);

        return JMXConnectorFactory.connect(serviceUrl, environment);
    }

    private JMXServiceURL createServiceUrl() throws MalformedURLException
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://localhost:%d/server",
            getPorts().get(0));
        return new JMXServiceURL(url);
    }
}
