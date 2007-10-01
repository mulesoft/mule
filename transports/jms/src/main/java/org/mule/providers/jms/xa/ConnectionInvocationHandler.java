/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicSession;
import javax.jms.XAConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicSession;

public class ConnectionInvocationHandler implements InvocationHandler
{

    private Object xaConnection;

    public ConnectionInvocationHandler(Object xac)
    {
        this.xaConnection = xac;
    }

    /**
     * Can be one of 3 types.
     * TODO check if we can portably cast it (JMS 1.1 vs 1.0.2b), see Jms102bSupport why
     *
     * @return underlying XAConnection instance
     */
    public Object getTargetObject()
    {
        return xaConnection;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (ConnectionFactoryWrapper.logger.isDebugEnabled())
        {
            ConnectionFactoryWrapper.logger.debug("Invoking " + method);
        }
        if (method.getName().equals("createSession"))
        {
            XASession xas = ((XAConnection) xaConnection).createXASession();
            return Proxy.newProxyInstance(Session.class.getClassLoader(), new Class[]{Session.class},
                                          new SessionInvocationHandler(xas));
        }
        else if (method.getName().equals("createQueueSession"))
        {
            XAQueueSession xaqs = ((XAQueueConnection) xaConnection).createXAQueueSession();
            return Proxy.newProxyInstance(Session.class.getClassLoader(),
                                          new Class[]{QueueSession.class}, new SessionInvocationHandler(xaqs));
        }
        else if (method.getName().equals("createTopicSession"))
        {
            XATopicSession xats = ((XATopicConnection) xaConnection).createXATopicSession();
            return Proxy.newProxyInstance(Session.class.getClassLoader(),
                                          new Class[]{TopicSession.class}, new SessionInvocationHandler(xats));
        }
        else
        {
            return method.invoke(xaConnection, args);
        }
    }

}
