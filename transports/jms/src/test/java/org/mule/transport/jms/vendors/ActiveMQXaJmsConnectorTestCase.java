/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.vendors;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.xa.ConnectionFactoryWrapper;
import org.mule.transport.jms.xa.ConnectionInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQXAConnectionFactory;

public class ActiveMQXaJmsConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "activemq-xa.xml";
    }

    public void testReflectiveXaCleanup() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);
        
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQXAConnectionFactory);

        ConnectionFactoryWrapper wrapper = new ConnectionFactoryWrapper(cf);
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
                ConnectionInvocationHandler handler =
                        (ConnectionInvocationHandler) Proxy.getInvocationHandler(connection);
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