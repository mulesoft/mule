/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.xa;

import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.transaction.xa.XAResource;

import bitronix.tm.resource.jms.DualSessionWrapper;
import bitronix.tm.resource.jms.MessageConsumerWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for JMS sessions to make thme participate in XA transactions.
 */
public class BitronixSessionInvocationHandler implements TargetInvocationHandler, XaTransaction.MuleXaObject
{

    protected static final transient Log logger = LogFactory.getLog(BitronixSessionInvocationHandler.class);
    private final DualSessionWrapper sessionWrapper;
    private boolean reuseSession;

    public BitronixSessionInvocationHandler(DualSessionWrapper sessionWrapper)
    {
        this.sessionWrapper = sessionWrapper;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(this + " Invoking " + method);
            }
            if (method.getDeclaringClass().equals(XaTransaction.MuleXaObject.class))
            {
                return method.invoke(this,args);
            }
            //TODO: BTM-132. Consumers are not getting closed never
            if (method.getName().equals("createConsumer"))
            {
                MessageConsumerWrapper messageConsumerWrapper = (MessageConsumerWrapper) method.invoke(sessionWrapper, args);
                return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                              new Class[] {MessageConsumer.class},
                                              new BitronixMessageConsumerInvocationHandler(messageConsumerWrapper));

            }
            return method.invoke(sessionWrapper, args);
        }
        catch (Exception e)
        {
            System.out.println("e");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception
    {
        sessionWrapper.close();
        sessionWrapper.getSession().close();
    }

    @Override
    public void setReuseObject(boolean reuseObject)
    {
        this.reuseSession = reuseObject;
    }

    @Override
    public boolean isReuseObject()
    {
        return reuseSession;
    }

    @Override
    public boolean enlist() throws TransactionException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        XAResource xaResource = sessionWrapper.getXAResource();
        ////TODO remove this logic once BTM-133
        if (xaResource == null)
        {

            try
            {
                sessionWrapper.getSession();
            }
            catch (JMSException e)
            {
                throw new TransactionException(e);
            }
        }
        return ((XaTransaction)tx).enlistResource(sessionWrapper.getXAResource());
    }

    @Override
    public boolean delist() throws Exception
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        return ((XaTransaction)tx).delistResource(sessionWrapper.getXAResource(),XAResource.TMSUCCESS);
    }

    @Override
    public Object getTargetObject()
    {
        return sessionWrapper;
    }
}
