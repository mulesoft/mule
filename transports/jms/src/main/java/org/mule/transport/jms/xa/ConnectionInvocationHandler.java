/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.xa;

import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;

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

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (ConnectionFactoryWrapper.logger.isDebugEnabled())
        {
            ConnectionFactoryWrapper.logger.debug("Invoking " + method);
        }
        
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        
        if (method.getName().equals("createSession"))
        {
            if (tx != null)
            {
                XASession xas = ((XAConnection) xaConnection).createXASession();
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
                    new Class[]{ Session.class, XaTransaction.MuleXaObject.class },
                    new SessionInvocationHandler(xas));
            }
            else
            {
                return ((XAConnection) xaConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
        else if (method.getName().equals("createQueueSession"))
        {
            if (tx != null)
            {
                XAQueueSession xaqs = ((XAQueueConnection) xaConnection).createXAQueueSession();
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{ QueueSession.class, XaTransaction.MuleXaObject.class }, 
                    new SessionInvocationHandler(xaqs));
            }
            else
            {
                return ((XAQueueConnection) xaConnection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
        else if (method.getName().equals("createTopicSession"))
        {
            if (tx != null)
            {
                XATopicSession xats = ((XATopicConnection) xaConnection).createXATopicSession();
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{ TopicSession.class, XaTransaction.MuleXaObject.class }, 
                    new SessionInvocationHandler(xats));
            }
            else
            {
                return ((XATopicConnection) xaConnection).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
        else
        {
            return method.invoke(xaConnection, args);
        }
    }

}
