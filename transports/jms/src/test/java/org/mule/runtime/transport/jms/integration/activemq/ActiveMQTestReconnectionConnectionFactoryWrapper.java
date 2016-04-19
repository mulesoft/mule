/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration.activemq;

import org.mule.runtime.core.util.proxy.TargetInvocationHandler;
import org.mule.runtime.transport.jms.test.TestReconnectionConnectionFactoryWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.Closeable;
import org.apache.activemq.StreamConnection;
import org.apache.activemq.management.StatsCapable;
import org.apache.activemq.transport.TransportListener;

public class ActiveMQTestReconnectionConnectionFactoryWrapper extends ActiveMQConnectionFactory
    implements TargetInvocationHandler, TestReconnectionConnectionFactoryWrapper
{
    private static List<Object> calledMethods;
    private static volatile boolean enabled = true;
    private static Connection connection;

    @Override
    public void init()
    {
        enabled = true;
        calledMethods = new CopyOnWriteArrayList<Object>();
    }

    public ActiveMQTestReconnectionConnectionFactoryWrapper()
    {
        init();
    }

    public ActiveMQTestReconnectionConnectionFactoryWrapper(String brokerURL)
    {
        super(brokerURL);
        init();
    }

    public ActiveMQTestReconnectionConnectionFactoryWrapper(URI brokerURL)
    {
        super(brokerURL);
        init();
    }

    public ActiveMQTestReconnectionConnectionFactoryWrapper(String userName, String password, URI brokerURL)
    {
        super(userName, password, brokerURL);
        init();
    }

    public ActiveMQTestReconnectionConnectionFactoryWrapper(String userName, String password, String brokerURL)
    {
        super(userName, password, brokerURL);
        init();
    }

    @Override
    public QueueConnection createQueueConnection() throws JMSException
    {
        registration();
        connection = super.createQueueConnection();
        return (QueueConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    @Override
    public QueueConnection createQueueConnection(String user, String passwd) throws JMSException
    {
        registration();
        connection = super.createQueueConnection(user, passwd);
        return (QueueConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException
    {
        registration();
        connection = super.createTopicConnection();
        return (TopicConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    @Override
    public TopicConnection createTopicConnection(String user, String passwd) throws JMSException
    {
        registration();
        connection = super.createTopicConnection(user, passwd);
        return (TopicConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    // For InvocationHandler interface
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        registration();
        return method.invoke(connection, args);
    }

    @Override
    public Object getTargetObject()
    {
        return connection;
    }

    /**
     * If enabled == true, do nothing. If not, throw a JMSException to simulate a
     * connection error to mule.
     *
     * @throws JMSException
     */
    private void registration() throws JMSException
    {
        //synchronized (connection)
        //{
            calledMethods.add(new Date());
            if (!isEnabled())
            {
                if (connection.getExceptionListener() != null)
                {
                    try
                    {
                        connection.getExceptionListener().onException(new JMSException("Disabled"));
                    }
                    catch (Exception e)
                    {
                        throw new JMSException("Disabled");
                    }
                }
                else
                {
                    throw new JMSException("Disabled");
                }
            }
        //}
    }

    @Override
    public List<Object> getCalledMethods()
    {
        return calledMethods;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        ActiveMQTestReconnectionConnectionFactoryWrapper.enabled = enabled;
    }

    @Override
    public void closeConnection()
    {
        try
        {
            connection.close();
        }
        catch (Exception e)
        {
            System.out.println("Error while closing connection: " + e.getMessage());
        }
    }
}
