/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

public class JmxAgentTestCase extends AbstractMuleTestCase
{
    private static final String[] VALID_AUTH_TOKEN = {"mule", "mulepassword"};
    private static final String DOMAIN = "JmxAgentTest";

    private JmxAgent jmxAgent;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        RmiRegistryAgent rmiRegistryAgent = new RmiRegistryAgent();
        jmxAgent = new JmxAgent();
        jmxAgent.setConnectorServerUrl(JmxAgent.DEFAULT_REMOTING_URI);
        managementContext.getRegistry().registerAgent(rmiRegistryAgent);
        managementContext.setId(DOMAIN);
    }

    public void testDefaultProperties() throws Exception
    {
        jmxAgent.setCredentials(getValidCredentials());
        managementContext.getRegistry().registerAgent(jmxAgent);
        managementContext.start();
    }

    public void testSuccessfulRemoteConnection() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(getValidCredentials());
        managementContext.getRegistry().registerAgent(jmxAgent);
        managementContext.start();

        JMXServiceURL serviceUrl = new JMXServiceURL(JmxAgent.DEFAULT_REMOTING_URI);
        Map props = new HashMap(1);
        props.put(JMXConnector.CREDENTIALS, VALID_AUTH_TOKEN);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, props);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        // is it the right server?
        assertTrue(Arrays.asList(connection.getDomains()).contains("Mule." + DOMAIN));
    }

    public void testNoCredentialsProvided() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(getValidCredentials());
        managementContext.getRegistry().registerAgent(jmxAgent);
        managementContext.start();

        JMXServiceURL serviceUrl = new JMXServiceURL(JmxAgent.DEFAULT_REMOTING_URI);
        try
        {
            JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    public void testNonRestrictedAccess() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(null);
        managementContext.getRegistry().registerAgent(jmxAgent);
        managementContext.start();

        JMXServiceURL serviceUrl = new JMXServiceURL(JmxAgent.DEFAULT_REMOTING_URI);
        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        // is it the right server?
        assertTrue(Arrays.asList(connection.getDomains()).contains("Mule." + DOMAIN));
    }

    protected Map getValidCredentials()
    {
        final Map credentials = new HashMap(1);
        credentials.put(VALID_AUTH_TOKEN[0], VALID_AUTH_TOKEN[1]);

        return credentials;
    }

    protected void configureProperties()
    {
        // make multi-NIC dev box happy by sticking RMI clients to a single
        // local ip address
        Map props = new HashMap();
        props.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                  new FixedHostRmiClientSocketFactory("127.0.0.1"));
        jmxAgent.setConnectorServerProperties(props);
    }
}
