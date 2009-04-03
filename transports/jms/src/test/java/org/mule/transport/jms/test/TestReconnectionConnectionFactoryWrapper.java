/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.test;

import java.lang.reflect.Method;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

/**
 * Interface for testing JMS reconnections. Implementing classes should extend a
 * particular JMS providers ConnectionFactory and throw a JMS exception when
 * isEnabled() == false and the mule server is trying to connect to it.
 */
public interface TestReconnectionConnectionFactoryWrapper
{
    public abstract void init();

    public abstract QueueConnection createQueueConnection() throws JMSException;

    public abstract QueueConnection createQueueConnection(String userName, String password)
        throws JMSException;

    public abstract TopicConnection createTopicConnection() throws JMSException;

    public abstract TopicConnection createTopicConnection(String userName, String password)
        throws JMSException;

    // For InvocationHandler interface
    public abstract Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

    public abstract Object getTargetObject();

    public abstract void setEnabled(boolean enabled);

    public abstract boolean isEnabled();
    
    public abstract List getCalledMethods();
    
    public abstract void closeConnection();
}
