/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.config;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

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

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JmxAgentAuthenticationTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "jmx-authentication-config.xml";
    }

    @Test(expected = SecurityException.class)
    public void testAccessJmxServerWithoutCredentials() throws Exception
    {
        JMXServiceURL serviceUrl = createServiceUrl();
        JMXConnectorFactory.connect(serviceUrl);
    }

    @Test
    public void testAccessJmxServerWithValidCredentials() throws Exception
    {
        JMXConnector connector = connectToJmx("jsmith", "foo");

        // to test the connection, access an MBean that is present by default
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName name = new ObjectName("java.lang:type=Runtime");
        ObjectInstance instance = connection.getObjectInstance(name);
        assertNotNull(instance);
    }

    @Test(expected = SecurityException.class)
    public void testAccessJmxServerWithInvalidCredentials() throws Exception
    {
        connectToJmx("invalid", "user");
    }

    private JMXConnector connectToJmx(String user, String password) throws IOException
    {
        JMXServiceURL serviceUrl = createServiceUrl();

        String[] authToken = new String[] {user, password};
        Map<String, ?> environment = Collections.singletonMap(JMXConnector.CREDENTIALS, authToken);

        return JMXConnectorFactory.connect(serviceUrl, environment);
    }

    private JMXServiceURL createServiceUrl() throws MalformedURLException
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://localhost:%d/server",
                                   dynamicPort.getNumber());
        return new JMXServiceURL(url);
    }
}
