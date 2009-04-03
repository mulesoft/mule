/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.test.TestReconnectionConnectionFactoryWrapper;
import org.mule.transport.jms.xa.TargetInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.Closeable;
import org.apache.activemq.StreamConnection;
import org.apache.activemq.management.StatsCapable;
import org.apache.activemq.transport.TransportListener;

public class ActiveMQTestReconnectionConnectionFactoryWrapper extends ActiveMQConnectionFactory
    implements TargetInvocationHandler, TestReconnectionConnectionFactoryWrapper
{
    private static List calledMethods;
    private static volatile boolean enabled = true;
    private static Connection connection;

    public void init()
    {
        enabled = true;
        calledMethods = new CopyOnWriteArrayList();
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

    /*
     * (non-Javadoc)
     * @seecom.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#
     * createQueueConnection()
     */
    public QueueConnection createQueueConnection() throws JMSException
    {
        registration();
        connection = super.createQueueConnection();
        return (QueueConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    /*
     * (non-Javadoc)
     * @seecom.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#
     * createQueueConnection(java.lang.String, java.lang.String)
     */
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException
    {
        registration();
        connection = super.createQueueConnection(userName, password);
        return (QueueConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    /*
     * (non-Javadoc)
     * @seecom.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#
     * createTopicConnection()
     */
    public TopicConnection createTopicConnection() throws JMSException
    {
        registration();
        connection = super.createTopicConnection();
        return (TopicConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    /*
     * (non-Javadoc)
     * @seecom.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#
     * createTopicConnection(java.lang.String, java.lang.String)
     */
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException
    {
        registration();
        connection = super.createTopicConnection(userName, password);
        return (TopicConnection) Proxy.newProxyInstance(
            ActiveMQTestReconnectionConnectionFactoryWrapper.class.getClassLoader(), new Class[]{Connection.class,
                TopicConnection.class, QueueConnection.class, StatsCapable.class, Closeable.class,
                StreamConnection.class, TransportListener.class}, this);
    }

    // For InvocationHandler interface
    /*
     * (non-Javadoc)
     * @see
     * com.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#invoke
     * (java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        registration();
        return method.invoke(connection, args);
    }

    /*
     * (non-Javadoc)
     * @seecom.mulesource.qatests.util.jms.activemq.TestReconnectionFactoryWrapper#
     * getTargetObject()
     */
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

    public List getCalledMethods()
    {
        return calledMethods;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        ActiveMQTestReconnectionConnectionFactoryWrapper.enabled = enabled;
    }

    public void closeConnection()
    {
        try
        {
            this.connection.close();
        }
        catch (Exception e)
        {
            System.out.println("Error while closing connection: " + e.getMessage());
        }
    }
}
