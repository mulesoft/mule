/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.xa;

import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.Session;

import bitronix.tm.resource.jms.DualSessionWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for JMS Connection that will return a proxy for JMS transacted sessions in order to make them participate in XA transactions.
 */
public class BitronixConnectionInvocationHandler implements TargetInvocationHandler
{

    protected static final transient Log logger = LogFactory.getLog(BitronixConnectionInvocationHandler.class);
    private Connection connection;

    public BitronixConnectionInvocationHandler(Connection connecton)
    {
        this.connection = connecton;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (BitronixConnectionInvocationHandler.logger.isDebugEnabled())
        {
            BitronixConnectionInvocationHandler.logger.debug("Invoking " + method);
        }

        Transaction tx = TransactionCoordination.getInstance().getTransaction();

        if (method.getName().equals("createSession"))
        {
            if (tx != null)
            {
                DualSessionWrapper xas = (DualSessionWrapper) connection.createSession((Boolean) args[0], (Integer) args[1]);
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                              new Class[] {Session.class, XaTransaction.MuleXaObject.class},
                                              new BitronixSessionInvocationHandler(xas));
            }
            else
            {
                return (connection).createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        }
        else
        {
            return method.invoke(connection, args);
        }
    }

    @Override
    public Object getTargetObject()
    {
        return connection;
    }
}
