/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class JmsConnectionFactoryTestCase extends AbstractMuleTestCase
{

    /**
     * Test providerProperties set on JmsConnector are not passed to the underlying
     * ConnectionFactory.
     */
    public void testProviderPropertiesNotPassed() throws Exception
    {
        // needed so that when the connecotr is initialsied the manage eventing
        // model will be available. This is used by Jms for reconnection
        // notifications
        managementContext.initialise();

        JmsConnector connector = new JmsConnector();
        Map providerProperties = new HashMap(1);
        final String testProviderProperty = "providerProperty";
        final String testValue = "TEST_VALUE";
        providerProperties.put(testProviderProperty, testValue);
        connector.setJndiProviderProperties(providerProperties);

        ConnectionFactory cf = new TestConnectionFactory();
        connector.setConnectionFactory(cf);

        connector.initialise(managementContext);

        assertEquals("Provider properties should not be passed to the ConnectionFactory.", "NOT_SET",
            ((TestConnectionFactory)cf).getProviderProperty());
    }

    /**
     * Test connectionFactoryProperties set on JmsConnector are actually passed to
     * the underlying ConnectionFactory.
     */
    public void testConnectionFactoryPropertiesPassed() throws Exception
    {
        // needed so that when the connecotr is initialsied the manage eventing
        // model will be available. This is used by Jms for reconnection
        // notifications
        managementContext.initialise();

        JmsConnector connector = new JmsConnector();
        Map connectionFactoryProperties = new HashMap(1);
        final String testConnectionFactoryProperty = "connectionFactoryProperty";
        final String testValue = "TEST_VALUE";
        connectionFactoryProperties.put(testConnectionFactoryProperty, testValue);
        connector.setConnectionFactoryProperties(connectionFactoryProperties);

        ConnectionFactory cf = new TestConnectionFactory();
        connector.setConnectionFactory(cf);

        connector.initialise(managementContext);
        connector.start();

        assertEquals("ConnectionFactory properties should be passed to the ConnectionFactory.", "TEST_VALUE",
            ((TestConnectionFactory)cf).getConnectionFactoryProperty());
    }

    public static final class TestConnectionFactory implements QueueConnectionFactory
    {
        private String providerProperty = "NOT_SET";
        private String connectionFactoryProperty = "NOT_SET";

        public Connection createConnection() throws JMSException
        {
            return null;
        }

        public Connection createConnection(String string, String string1) throws JMSException
        {
            return null;
        }

        public String getProviderProperty()
        {
            return providerProperty;
        }

        /**
         * Should NOT be called.
         */
        public void setProviderProperty(final String providerProperty)
        {
            throw new IllegalStateException("Should never be called.");
        }

        public String getConnectionFactoryProperty()
        {
            return connectionFactoryProperty;
        }

        /**
         * MUST be called
         */
        public void setConnectionFactoryProperty(final String connectionFactoryProperty)
        {
            this.connectionFactoryProperty = connectionFactoryProperty;
        }

        public QueueConnection createQueueConnection() throws JMSException
        {
            return null;
        }

        public QueueConnection createQueueConnection(String string, String string1) throws JMSException
        {
            return null;
        }
    }

}
