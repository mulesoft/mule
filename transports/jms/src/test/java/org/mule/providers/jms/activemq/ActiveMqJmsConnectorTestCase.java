/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.activemq;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.JmsTopicResolver;
import org.mule.providers.jms.xa.ConnectionFactoryWrapper;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQXAConnectionFactory;

public class ActiveMqJmsConnectorTestCase extends AbstractMuleTestCase
{
    public void testConfigurationDefaults()
    {
        ActiveMqJmsConnector c = new ActiveMqJmsConnector();
        assertFalse(c.isEagerConsumer());
        JmsTopicResolver resolver = c.getTopicResolver();
        assertNotNull("Topic resolver must not be null.", resolver);
        assertTrue("Wrong topic resolver configured on the connector.",
                   resolver instanceof DefaultJmsTopicResolver);
    }

    public void testReflectiveXaCleanup() throws Exception
    {
        ActiveMQXAConnectionFactory factory = new ActiveMQXAConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");

        ConnectionFactoryWrapper wrapper = new ConnectionFactoryWrapper(factory, new TestTransactionManagerFactory().create());
        // can be a proxy
        Connection connection = wrapper.createConnection();
        assertNotNull(connection);
        assertTrue(Proxy.isProxyClass(connection.getClass()));

        try
        {
            final Class clazz = connection.getClass();
            Method cleanupMethod;
            if (Proxy.isProxyClass(clazz))
            {
                ConnectionFactoryWrapper.ConnectionInvocationHandler handler =
                        (ConnectionFactoryWrapper.ConnectionInvocationHandler) Proxy.getInvocationHandler(connection);
                // this is really an XA connection
                connection = (Connection) handler.getTargetObject();
                Class realConnectionClass = connection.getClass();
                cleanupMethod = realConnectionClass.getMethod("cleanup", null);
            }
            else
            {
                cleanupMethod = clazz.getMethod("cleanup", null);
            }


            if (cleanupMethod != null)
            {
                cleanupMethod.invoke(connection, null);
            }
        }
        finally
        {
            connection.close();
        }

        // there should be no errors
    }
}