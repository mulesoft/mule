/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.api.context.MuleContextBuilder;
import org.mule.component.simple.EchoComponent;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.module.management.agent.JmxApplicationAgent;
import org.mule.module.management.agent.RmiRegistryAgent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JmxAgentTestCase extends AbstractMuleContextTestCase
{
    private static final String[] VALID_AUTH_TOKEN = {"mule", "mulepassword"};
    private static final String DOMAIN = "JmxAgentTest";

    private JMXServiceURL serviceUrl;
    private JmxApplicationAgent jmxAgent;

    @Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        super.configureMuleContext(contextBuilder);

        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setId(DOMAIN);
        contextBuilder.setMuleConfiguration(config);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        serviceUrl = new JMXServiceURL(JmxApplicationAgent.DEFAULT_REMOTING_URI);
        muleContext.getRegistry().registerAgent(new RmiRegistryAgent());
        jmxAgent = muleContext.getRegistry().lookupObject(JmxApplicationAgent.class);
        jmxAgent.setConnectorServerUrl(JmxApplicationAgent.DEFAULT_REMOTING_URI);
    }

    @Override
    protected void doTearDown()
    {
        if (jmxAgent != null)
        {
            jmxAgent.dispose();
        }
    }

    @Test
    public void testDefaultProperties() throws Exception
    {
        jmxAgent.setCredentials(getValidCredentials());
        muleContext.start();
    }

    @Test
    public void testSuccessfulRemoteConnection() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(getValidCredentials());
        muleContext.start();

        JMXConnector connector = null;
        try
        {
            Map<String, ?> props = Collections.singletonMap(JMXConnector.CREDENTIALS, VALID_AUTH_TOKEN);
            connector = JMXConnectorFactory.connect(serviceUrl, props);
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            // is it the right server?
            assertTrue(Arrays.asList(connection.getDomains()).toString(),
                    Arrays.asList(connection.getDomains()).contains("Mule." + DOMAIN));
        }
        finally
        {
            if (connector != null)
            {
                connector.close();
            }
        }
    }

    @Test
    public void testNoCredentialsProvided() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(getValidCredentials());
        muleContext.start();

        JMXConnector connector = null;
        try
        {
            connector = JMXConnectorFactory.connect(serviceUrl);
            fail("expected SecurityException");
        }
        catch (SecurityException e)
        {
            // expected
        }
        finally
        {
            if (connector != null)
            {
                connector.close();
            }
        }
    }

    @Test
    public void testNonRestrictedAccess() throws Exception
    {
        configureProperties();
        jmxAgent.setCredentials(null);
        muleContext.start();

        JMXConnector connector = null;
        try
        {
            connector = JMXConnectorFactory.connect(serviceUrl);
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            // is it the right server?
            assertTrue(Arrays.asList(connection.getDomains()).contains("Mule." + DOMAIN));
        }
        finally
        {
            if (connector != null)
            {
                connector.close();
            }
        }
    }

    protected Map<String, String> getValidCredentials()
    {
        final Map<String, String> credentials = new HashMap<String, String>(1);
        credentials.put(VALID_AUTH_TOKEN[0], VALID_AUTH_TOKEN[1]);

        return credentials;
    }

    protected void configureProperties()
    {
        // make multi-NIC dev box happy by sticking RMI clients to a single
        // local ip address
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                  new FixedHostRmiClientSocketFactory("127.0.0.1"));
        jmxAgent.setConnectorServerProperties(props);
    }

    @Test
    public void testServiceNameContainsColon() throws Exception
    {
        // create a service with an invalid name. It is registered in the registry as side effect
        // so the JmxAgent will pick it up while registring services
        getTestService("invalid:service:name", EchoComponent.class);

        // when registering services, the one we just put into the registry will be exposed
        // to the local MBean server, too. If a MalformedObjectNameException is thrown during
        // this operation, this test will fail
        TestJmxAgent agent = new TestJmxAgent();
        agent.setMuleContext(muleContext);
        agent.initialise();

        agent.registerServiceServices();
    }

    private static class TestJmxAgent extends JmxApplicationAgent
    {
        /**
         * Open up method for test access
         */
        @Override
        public void registerServiceServices() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
        {
            super.registerServiceServices();
        }
    }
}
