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

import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOTransaction;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
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

public class SessionInvocationHandler implements InvocationHandler
{
    protected final transient Log logger = LogFactory.getLog(getClass());

    private XASession xaSession;
    private XAResource xaResource;
    private volatile boolean enlisted = false;
    private final Reference underlyingObject;

    public SessionInvocationHandler(XASession xaSession) throws JMSException
    {
        this.xaSession = xaSession;
        underlyingObject = new WeakReference(xaSession.getSession());
        this.xaResource = new XAResourceWrapper(xaSession.getXAResource(), this);
    }

    public SessionInvocationHandler(XAQueueSession xaSession) throws JMSException
    {
        this.xaSession = xaSession;
        underlyingObject = new WeakReference(xaSession.getQueueSession());
        this.xaResource = new XAResourceWrapper(xaSession.getXAResource(), this);
    }

    public SessionInvocationHandler(XATopicSession xaSession) throws JMSException
    {
        this.xaSession = xaSession;
        underlyingObject = new WeakReference(xaSession.getTopicSession());
        this.xaResource = new XAResourceWrapper(xaSession.getXAResource(), this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Invoking " + method);
        }
        if (underlyingObject.get() == null)
        {
            throw new IllegalStateException("Underlying xaSession is null, XASession " + xaSession);
        }
        Object result = method.invoke(underlyingObject.get(), args);

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

    protected void enlist() throws Exception
    {
        if (isEnlisted())
        {
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Enlistment request: " + this);
        }

        UMOTransaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            throw new IllegalStateException(CoreMessages.notMuleTransactionAvailable().toString());
        }
        if (!(transaction instanceof XaTransaction))
        {
            throw new IllegalStateException(CoreMessages.notMuleXaTransaction(transaction).toString());
        }

        if (!isEnlisted())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Enlisting resource " + xaResource + " in xa transaction " + transaction);
            }

            enlisted = ((XaTransaction) transaction).enlistResource(xaResource);
        }
    }

    public boolean isEnlisted()
    {
        return enlisted;
    }

    public void setEnlisted(boolean enlisted)
    {
        this.enlisted = enlisted;
    }

    public XASession getTargetObject()
    {
        return xaSession;
    }

    public XAResource getXAResource()
    {
        return xaResource;
    }


}
