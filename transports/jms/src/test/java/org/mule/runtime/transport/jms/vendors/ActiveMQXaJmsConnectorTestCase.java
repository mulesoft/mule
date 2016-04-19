/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.vendors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.proxy.TargetInvocationHandler;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.xa.DefaultXAConnectionFactoryWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.junit.Test;

public class ActiveMQXaJmsConnectorTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "activemq-xa.xml";
    }

    @Test
    public void testReflectiveXaCleanup() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);
        
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQXAConnectionFactory);

        DefaultXAConnectionFactoryWrapper wrapper = new DefaultXAConnectionFactoryWrapper(cf);
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
                TargetInvocationHandler handler =
                        (TargetInvocationHandler) Proxy.getInvocationHandler(connection);
                // this is really an XA connection
                connection = (Connection) handler.getTargetObject();
                Class realConnectionClass = connection.getClass();
                cleanupMethod = realConnectionClass.getMethod("cleanup", (Class[])null);
            }
            else
            {
                cleanupMethod = clazz.getMethod("cleanup", (Class[])null);
            }


            if (cleanupMethod != null)
            {
                cleanupMethod.invoke(connection, (Object[])null);
            }
        }
        finally
        {
            connection.close();
        }

        // there should be no errors
    }
}
