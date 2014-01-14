/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jms;

import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.util.proxy.TargetInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import bitronix.tm.resource.jms.DualSessionWrapper;
import bitronix.tm.resource.jms.JmsConnectionHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for JMS Connection that will return a proxy for JMS transacted sessions in order to make them participate in XA transactions.
 */
public class BitronixConnectionInvocationHandler implements TargetInvocationHandler
{

    private static final Logger logger = LoggerFactory.getLogger(BitronixConnectionInvocationHandler.class);
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
        if (connection instanceof JmsConnectionHandle)
        {
            JmsConnectionHandle jmsConnectionHandle = (JmsConnectionHandle) connection;
            try
            {
                return jmsConnectionHandle.getXAConnection();
            }
            catch (JMSException e)
            {
                // The connection is already closed, no reference to XAConnection available.
                if (BitronixConnectionInvocationHandler.logger.isDebugEnabled())
                {
                    BitronixConnectionInvocationHandler.logger.debug("Failed to get XA connection", e);
                }
            }
        }
        return connection;
    }
}
