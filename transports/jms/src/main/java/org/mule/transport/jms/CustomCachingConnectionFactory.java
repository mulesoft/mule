/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.api.Closeable;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;

import org.slf4j.Logger;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;

public class CustomCachingConnectionFactory extends CachingConnectionFactory implements Closeable, Disposable
{

    Logger LOGGER = getLogger(CustomCachingConnectionFactory.class);

    private final String username;
    private final String password;
    private ExceptionListener exceptionListener = new NullExceptionListener();

    public CustomCachingConnectionFactory(ConnectionFactory targetConnectionFactory, String username, String password)
    {
        super(targetConnectionFactory);
        this.username = username;
        this.password = password;
    }

    public CustomCachingConnectionFactory(ConnectionFactory targetConnectionFactory, String username, String password, ExceptionListener muleExceptionListener)
    {
        this(targetConnectionFactory, username, password);
        this.exceptionListener = muleExceptionListener;
    }

    @Override
    public Connection createConnection(String username, String password) throws JMSException
    {
        throw new javax.jms.IllegalStateException(
                "CustomCachingConnectionFactory does not support creating a connection with username and password. Provide the desired username and password when the instance is defined");
    }

    @Override
    protected Connection doCreateConnection() throws JMSException
    {
        if (username == null && password == null)
        {
            return super.doCreateConnection();
        }
        else
        {
            ConnectionFactory cf = getTargetConnectionFactory();
            if (Boolean.FALSE.equals(getPubSubMode()) && cf instanceof QueueConnectionFactory)
            {
                return ((QueueConnectionFactory) cf).createQueueConnection(username, password);
            }
            else if (Boolean.TRUE.equals(getPubSubMode()) && cf instanceof TopicConnectionFactory)
            {
                return ((TopicConnectionFactory) cf).createTopicConnection(username, password);
            }
            else
            {
                return getTargetConnectionFactory().createConnection(username, password);
            }
        }
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    private boolean getPubSubMode()
    {
        try
        {
            Field pubSubModeField = SingleConnectionFactory.class.getDeclaredField("pubSubMode");
            pubSubModeField.setAccessible(true);

            Object value = pubSubModeField.get(this);
            return value == null ? false : ((Boolean) value);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to determine value of pubSubMode field", e);
        }
    }

    @Override
    public void onException(JMSException ex)
    {
        // Fist delegate exception to MuleRuntime
        LOGGER.error("Delegating exception to MuleRuntime:", ex);
        exceptionListener.onException(ex);
        // Then delegate to Spring AggregatedExceptionListener
        LOGGER.error("Delegating exception to Spring connection factory: {}", ex);
        super.onException(ex);
    }

    @Override
    public void close() throws MuleException
    {
        resetConnection();
    }

    @Override
    public void dispose()
    {
        destroy();
    }

    @Override
    protected Session getSession(Connection con, Integer mode) throws JMSException
    {
        if (exceptionListener instanceof JmsConnector &&
            ((JmsConnector) exceptionListener).isHandlingException())
        {
            throw new JMSException("Cannot acquire session while exception is being handled");
        }
        return super.getSession(con, mode);
    }

    @Override
    protected Session createSession(Connection con, Integer mode) throws JMSException
    {
        if (exceptionListener instanceof JmsConnector &&
            ((JmsConnector) exceptionListener).isHandlingException())
        {
            throw new JMSException("Cannot create session while exception is being handled");
        }
        return super.createSession(con, mode);
    }
    
}