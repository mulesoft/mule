/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.xa;

import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transaction.IllegalTransactionStateException;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transaction.XaTransaction;
import org.mule.runtime.core.util.proxy.TargetInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TopicPublisher;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicSession;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionInvocationHandler implements TargetInvocationHandler
{
    protected static final transient Log logger = LogFactory.getLog(SessionInvocationHandler.class);

    private XASession xaSession;
    private XAResource xaResource;
    private volatile boolean enlisted = false;
    private volatile boolean reuseObject = false;
    private final Session session;

    private SessionInvocationHandler(XASession xaSession, Session session, Boolean sameRMOverrideValue)
    {
        super();
        this.xaSession = xaSession;
        this.session = session;
        this.xaResource = new XAResourceWrapper(xaSession.getXAResource(), this, sameRMOverrideValue);
    }
    
    public SessionInvocationHandler(XASession xaSession, Boolean sameRMOverrideValue) throws JMSException
    {
        this(xaSession, xaSession.getSession(), sameRMOverrideValue);
    }

    public SessionInvocationHandler(XAQueueSession xaSession, Boolean sameRMOverrideValue) throws JMSException
    {
        this(xaSession, xaSession.getQueueSession(), sameRMOverrideValue);
    }

    public SessionInvocationHandler(XATopicSession xaSession, Boolean sameRMOverrideValue) throws JMSException
    {
        this(xaSession, xaSession.getTopicSession(), sameRMOverrideValue);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(this + " Invoking " + method);
        }

        // processing method from MuleXaObject
        if (XaTransaction.MuleXaObject.DELIST_METHOD_NAME.equals(method.getName()))
        {
            return delist();
        }
        else if (XaTransaction.MuleXaObject.ENLIST_METHOD_NAME.equals(method.getName()))
        {
            return enlist();
        }
        else if (XaTransaction.MuleXaObject.SET_REUSE_OBJECT_METHOD_NAME.equals(method.getName()))
        {
            reuseObject = (Boolean) args[0];
            return null;
        }
        else if (XaTransaction.MuleXaObject.IS_REUSE_OBJECT_METHOD_NAME.equals(method.getName()))
        {
            return reuseObject;
        }
        else if (XaTransaction.MuleXaObject.GET_TARGET_OBJECT_METHOD_NAME.equals(method.getName()))
        {
            return getTargetObject();
        }
        else if (XaTransaction.MuleXaObject.CLOSE_METHOD_NAME.equals(method.getName()))
        {
            // some jms implementation need both sessions closed, some not
            try
            {
                session.close();
                xaSession.close();
            }
            catch (Exception ex)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing the session and the xaSession failed", ex);
                }
            }
            return null;
        }
        
        Object result = method.invoke(session, args);

        if (result instanceof TopicSubscriber)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{TopicSubscriber.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        else if (result instanceof QueueReceiver)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{QueueReceiver.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        else if (result instanceof MessageConsumer)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{MessageConsumer.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        else if (result instanceof TopicPublisher)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{TopicPublisher.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        else if (result instanceof QueueSender)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{QueueSender.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        else if (result instanceof MessageProducer)
        {
            result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                                            new Class[]{MessageProducer.class}, new ConsumerProducerInvocationHandler(this, result));
        }
        return result;
    }

    public boolean enlist() throws Exception
    {
        if (isEnlisted())
        {
            return false;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Enlistment request: " + this);
        }

        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        if (!isEnlisted())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Enlisting resource " + xaResource + " in xa transaction " + transaction);
            }

            enlisted = ((XaTransaction) transaction).enlistResource(xaResource);
        }
        
        return enlisted;
    }

    public boolean delist() throws Exception
    {
        if (!isEnlisted())
        {
            return false;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Delistment request: " + this);
        }

        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
        }

        if (isEnlisted())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Delisting resource " + xaResource + " in xa transaction " + transaction);
            }

            enlisted = !((XaTransaction) transaction).delistResource(xaResource, XAResource.TMSUCCESS);
        }
        return !isEnlisted();
    }

    public boolean isEnlisted()
    {
        return enlisted;
    }

    public void setEnlisted(boolean enlisted)
    {
        this.enlisted = enlisted;
    }

    @Override
    public Object getTargetObject()
    {
        return xaSession;
    }

    public XAResource getXAResource()
    {
        return xaResource;
    }

}
