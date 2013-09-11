/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.xa;

import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XATopicConnection;
import javax.jms.XATopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionFactoryWrapper
        implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory
{
    /**
     * logger used by this class
     */
    protected static final transient Log logger = LogFactory.getLog(ConnectionFactoryWrapper.class);

    protected final Object factory;
    private Boolean sameRMOverrideValue;

    public ConnectionFactoryWrapper(Object factory)
    {
        this(factory, null);
    }

    public ConnectionFactoryWrapper(Object factory, Boolean sameRMOverrideValue)
    {
        this.factory = factory;
        this.sameRMOverrideValue = sameRMOverrideValue;
    }

    public Connection createConnection() throws JMSException
    {
        XAConnection xac = ((XAConnectionFactory) factory).createXAConnection();
        Connection proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                               new Class[]{Connection.class},
                                                               new ConnectionInvocationHandler(xac, sameRMOverrideValue));
        return proxy;
    }

    public Connection createConnection(String username, String password) throws JMSException
    {
        XAConnection xac = ((XAConnectionFactory) factory).createXAConnection(username, password);
        Connection proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                               new Class[]{Connection.class},
                                                               new ConnectionInvocationHandler(xac, sameRMOverrideValue));
        return proxy;
    }

    public QueueConnection createQueueConnection() throws JMSException
    {
        XAQueueConnection xaqc = ((XAQueueConnectionFactory) factory).createXAQueueConnection();
        QueueConnection proxy = (QueueConnection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                                         new Class[]{QueueConnection.class},
                                                                         new ConnectionInvocationHandler(xaqc, sameRMOverrideValue));
        return proxy;
    }

    public QueueConnection createQueueConnection(String username, String password) throws JMSException
    {
        XAQueueConnection xaqc = ((XAQueueConnectionFactory) factory).createXAQueueConnection(username,
                                                                                              password);
        QueueConnection proxy = (QueueConnection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                                         new Class[]{QueueConnection.class},
                                                                         new ConnectionInvocationHandler(xaqc, sameRMOverrideValue));
        return proxy;
    }

    public TopicConnection createTopicConnection() throws JMSException
    {
        XATopicConnection xatc = ((XATopicConnectionFactory) factory).createXATopicConnection();
        TopicConnection proxy = (TopicConnection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                                         new Class[]{TopicConnection.class},
                                                                         new ConnectionInvocationHandler(xatc, sameRMOverrideValue));
        return proxy;
    }

    public TopicConnection createTopicConnection(String username, String password) throws JMSException
    {
        XATopicConnection xatc = ((XATopicConnectionFactory) factory).createXATopicConnection(username,
                                                                                              password);
        TopicConnection proxy = (TopicConnection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                                         new Class[]{TopicConnection.class},
                                                                         new ConnectionInvocationHandler(xatc, sameRMOverrideValue));
        return proxy;
    }

}
