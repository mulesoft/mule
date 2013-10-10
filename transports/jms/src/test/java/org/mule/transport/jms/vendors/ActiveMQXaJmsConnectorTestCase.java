/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.vendors;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.xa.ConnectionFactoryWrapper;
import org.mule.transport.jms.xa.TargetInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActiveMQXaJmsConnectorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
