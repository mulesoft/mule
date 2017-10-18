/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.activemq;

import org.mule.api.MuleContext;
import org.mule.transport.ConnectException;
import org.mule.transport.jms.JmsConnector;
import org.mule.util.ClassUtils;
import org.mule.util.proxy.TargetInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

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
    public ActiveMQJmsConnector(MuleContext context)
    {
        super(context);
        setEagerConsumer(false);
        // TODO MULE-1409 better support for ActiveMQ 4.x temp destinations
    }

    protected ConnectionFactory getDefaultConnectionFactory() throws Exception
    {
        ConnectionFactory connectionFactory = (ConnectionFactory)
                ClassUtils.instanciateClass(ACTIVEMQ_CONNECTION_FACTORY_CLASS, getBrokerURL());
        applyVendorSpecificConnectionFactoryProperties(connectionFactory);
        return connectionFactory;
    }

    protected void applyVendorSpecificConnectionFactoryProperties(ConnectionFactory connectionFactory)
    {
        try
        {
            configurePrefetchPolicy(connectionFactory);

            Method getRedeliveryPolicyMethod = connectionFactory.getClass().getMethod("getRedeliveryPolicy");
            Object redeliveryPolicy = getRedeliveryPolicyMethod.invoke(connectionFactory);
           
            configureMaximumRedeliveriesMethod(redeliveryPolicy);
            
            configureMaximumRedeliveryDelay(redeliveryPolicy);
            
            configureInitialRedeliveryDelay(redeliveryPolicy);
           
            configureRedeliveryDelay(redeliveryPolicy);
        }
        catch (Exception e)
        {
            logger.error("Can not set MaxRedelivery parameter to RedeliveryPolicy " + e);
        }
    }

    private void configurePrefetchPolicy(ConnectionFactory connectionFactory) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method getPrefetchPolicy = connectionFactory.getClass().getMethod("getPrefetchPolicy");
        Object prefetchPolicy = getPrefetchPolicy.invoke(connectionFactory);
        Method setQueuePrefetch = prefetchPolicy.getClass().getMethod("setQueuePrefetch", Integer.TYPE);
        int maxQueuePrefetch = getMaxQueuePrefetch();
        if (maxQueuePrefetch != PREFETCH_DEFAULT)
        {
            setQueuePrefetch.invoke(prefetchPolicy, maxQueuePrefetch);
        }
    }

    private void configureMaximumRedeliveriesMethod(Object redeliveryPolicy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method setMaximumRedeliveriesMethod = redeliveryPolicy.getClass().getMethod("setMaximumRedeliveries", Integer.TYPE);
        int maxRedelivery = getMaxRedelivery();
        if (maxRedelivery != REDELIVERY_IGNORE )
        {
            // redelivery = deliveryCount - 1, but AMQ is considering the first delivery attempt as a redelivery (wrong!). adjust for it
            maxRedelivery++;
        }
        setMaximumRedeliveriesMethod.invoke(redeliveryPolicy, maxRedelivery);
    }

    private void configureRedeliveryDelay(Object redeliveryPolicy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        int redeliveryDelay = getRedeliveryDelay();
        Method setRedeliveryDelay = redeliveryPolicy.getClass().getMethod("setRedeliveryDelay", Long.TYPE);
        if (redeliveryDelay != DEFAULT_REDELIVERY_DELAY)
        {
            setRedeliveryDelay.invoke(redeliveryPolicy, 0L);
        }
    }

    private void configureInitialRedeliveryDelay(Object redeliveryPolicy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method setInitialRedeliveryDelay = redeliveryPolicy.getClass().getMethod("setInitialRedeliveryDelay", Long.TYPE);
        int initialRedeliveryDelay = getInitialRedeliveryDelay();
        if (initialRedeliveryDelay != DEFAULT_INITIAL_REDELIVERY_DELAY)
        {
            setInitialRedeliveryDelay.invoke(redeliveryPolicy, 0L);
        }
    }

    private void configureMaximumRedeliveryDelay(Object redeliveryPolicy) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method setMaximumRedeliveryDelay = redeliveryPolicy.getClass().getMethod("setMaximumRedeliveryDelay", Long.TYPE);           
        int  maximumRedeliveryDelay = getMaximumRedeliveryDelay();
        if (maximumRedeliveryDelay != DEFAULT_MAX_REDELIVERY_DELAY)
        {
            setMaximumRedeliveryDelay.invoke(redeliveryPolicy, 0L);                
        }
    }

    /**
     * Will additionally try to cleanup the ActiveMq connection, otherwise there's a deadlock on shutdown.
     */
    protected void doDisconnect() throws Exception
    {
        try
        {
            Connection connection = getConnection();
            if (connection == null)
            {
                return;
            }

            final Class clazz = connection.getClass();
            Method cleanupMethod = null;
            if (Proxy.isProxyClass(clazz))
            {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(connection);

                // When using caching-connection-factory, the connections are proxy objects that do nothing on the
                // close and stop methods so that they remain open when returning to the cache. In that case, we don't
                // need to do any custom cleanup, as the connections will be closed when destroying the cache. The
                // type of the invocation handler for these connections is SharedConnectionInvocationHandler.

                if (invocationHandler instanceof TargetInvocationHandler)
                {
                    // this is really an XA connection, bypass the java.lang.reflect.Proxy as it
                    // can't delegate to non-interfaced methods (like proprietary 'cleanup' one)
                    // TODO check if CGlib will manage to enhance the AMQ connection class,
                    // there are no final methods, but a number of private ones, though
                    TargetInvocationHandler targetInvocationHandler = (TargetInvocationHandler) invocationHandler;
                    connection = (Connection) targetInvocationHandler.getTargetObject();
                    Class realConnectionClass = connection.getClass();
                    cleanupMethod = realConnectionClass.getMethod("cleanup", (Class[])null);
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("InvocationHandler of the JMS connection proxy is of type %s, not doing " +
                                                   "any extra cleanup", invocationHandler.getClass().getName()));
                    }
                }
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
            catch (InvocationTargetException ex)
            {
                logger.warn("Exception cleaning up JMS connection: " + ex.getMessage());        
            }
            finally
            {
                try
                {
                    connection.close();
                }
                catch (JMSException ex)
                {
                    logger.warn("Exception closing JMS connection: " + ex.getMessage());
                }
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
