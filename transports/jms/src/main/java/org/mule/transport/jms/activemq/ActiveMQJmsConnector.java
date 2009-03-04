/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.activemq;

import org.mule.transport.ConnectException;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.xa.TargetInvocationHandler;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

/**
 * ActiveMQ 4.x-specific JMS connector.
 */
public class ActiveMQJmsConnector extends JmsConnector
{
    public static final String ACTIVEMQ_CONNECTION_FACTORY_CLASS = "org.apache.activemq.ActiveMQConnectionFactory";
    public static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";

    private String brokerURL = DEFAULT_BROKER_URL;

    /**
     * Constructs a new ActiveMQJmsConnector.
     */
    public ActiveMQJmsConnector()
    {
        setEagerConsumer(false);
        // TODO MULE-1409 better support for ActiveMQ 4.x temp destinations
    }

    protected ConnectionFactory getDefaultConnectionFactory()
    {
        try
        {
            ConnectionFactory connectionFactory = (ConnectionFactory)
                    ClassUtils.instanciateClass(ACTIVEMQ_CONNECTION_FACTORY_CLASS, getBrokerURL());
            applyVendorSpecificConnectionFactoryProperties(connectionFactory);
            return connectionFactory;
        }
        catch (InvocationTargetException itex)
        {
            Throwable target = itex.getCause();
            handleException(target instanceof Exception ? (Exception) target : new Exception(target));
        }
        catch (Exception e)
        {
            handleException(e);
        }
        return null;
    }

    protected void applyVendorSpecificConnectionFactoryProperties(ConnectionFactory connectionFactory)
    {
        try
        {
            Method getRedeliveryPolicyMethod = connectionFactory.getClass().getMethod("getRedeliveryPolicy");
            Object redeliveryPolicy = getRedeliveryPolicyMethod.invoke(connectionFactory);
            Method setMaximumRedeliveriesMethod = redeliveryPolicy.getClass().getMethod("setMaximumRedeliveries", Integer.TYPE);
            // redelivery = deliveryCount - 1, but AMQ is considering the first delivery attempt as a redelivery (wrong!). adjust for it
            setMaximumRedeliveriesMethod.invoke(redeliveryPolicy, getMaxRedelivery() + 1);
        }
        catch (Exception e)
        {
            logger.error("Can not set MaxRedelivery parameter to RedeliveryPolicy " + e);
        }
    }

    /**
     * Will additionally try to cleanup the ActiveMq connection, otherwise there's a deadlock on shutdown.
     */
    protected void doDisconnect() throws ConnectException
    {
        try
        {
            Connection connection = getConnection();
            if (connection == null)
            {
                return;
            }

            final Class clazz = connection.getClass();
            Method cleanupMethod;
            if (Proxy.isProxyClass(clazz))
            {
                TargetInvocationHandler handler =
                        (TargetInvocationHandler) Proxy.getInvocationHandler(connection);
                // this is really an XA connection, bypass the java.lang.reflect.Proxy as it
                // can't delegate to non-interfaced methods (like proprietary 'cleanup' one)
                // TODO check if CGlib will manage to enhance the AMQ connection class,
                // there are no final methods, but a number of private ones, though
                connection = (Connection) handler.getTargetObject();
                Class realConnectionClass = connection.getClass();
                cleanupMethod = realConnectionClass.getMethod("cleanup", (Class[])null);
            }
            else
            {
                cleanupMethod = clazz.getMethod("cleanup", (Class[])null);
            }

            try
            {
                if (cleanupMethod != null)
                {
                    cleanupMethod.invoke(connection, (Object[])null);
                }
            }
            finally
            {
                connection.close();
            }
        }
        catch (Exception e)
        {
            throw new ConnectException(e, this);
        }
        finally
        {
            //Is this necessary? It causes a NPE in certain situations
            setConnection(null);
        }
    }

    public String getBrokerURL()
    {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL)
    {
        this.brokerURL = brokerURL;
    }
}
