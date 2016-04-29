/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.xa;

import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transaction.XaTransaction;
import org.mule.runtime.core.util.proxy.TargetInvocationHandler;

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

public class ConnectionInvocationHandler implements TargetInvocationHandler
{
    private Object xaConnection;
    private Boolean sameRMOverrideValue;

    public ConnectionInvocationHandler(Object xac)
    {
        this(xac, null);
    }

    public ConnectionInvocationHandler(Object xac, Boolean sameRMOverrideValue)
    {
        this.xaConnection = xac;
        this.sameRMOverrideValue = sameRMOverrideValue;
    }

    @Override
    public Object getTargetObject()
    {
        return xaConnection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (DefaultXAConnectionFactoryWrapper.logger.isDebugEnabled())
        {
            DefaultXAConnectionFactoryWrapper.logger.debug("Invoking " + method);
        }
        
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        
        if (method.getName().equals("createSession"))
        {
            if (tx != null)
            {
                XASession xas = ((XAConnection) xaConnection).createXASession();
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
                    new Class[]{ Session.class, XaTransaction.MuleXaObject.class },
                    new SessionInvocationHandler(xas, sameRMOverrideValue));
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
                    new SessionInvocationHandler(xaqs, sameRMOverrideValue));
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
                    new SessionInvocationHandler(xats, sameRMOverrideValue));
            }
            else
            {
                return ((XATopicConnection) xaConnection).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
        else if (method.getName().equals("getHoldObject"))
        {
            return xaConnection;
        }
        else
        {
            return method.invoke(xaConnection, args);
        }
    }
}
