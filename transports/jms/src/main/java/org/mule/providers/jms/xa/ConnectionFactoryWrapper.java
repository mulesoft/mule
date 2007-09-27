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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicConnectionFactory;
import javax.jms.XATopicSession;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionFactoryWrapper
    implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ConnectionFactoryWrapper.class);

    protected final Object factory;
    protected final TransactionManager tm;

    public ConnectionFactoryWrapper(Object factory, TransactionManager tm)
    {
        this.factory = factory;
        this.tm = tm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createConnection()
     */
    public Connection createConnection() throws JMSException
    {
        XAConnection xac = ((XAConnectionFactory)factory).createXAConnection();
        Connection proxy = (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{Connection.class}, new ConnectionInvocationHandler(xac));
        return proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ConnectionFactory#createConnection(java.lang.String,
     *      java.lang.String)
     */
    public Connection createConnection(String username, String password) throws JMSException
    {
        XAConnection xac = ((XAConnectionFactory)factory).createXAConnection(username, password);
        Connection proxy = (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{Connection.class}, new ConnectionInvocationHandler(xac));
        return proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueConnectionFactory#createQueueConnection()
     */
    public QueueConnection createQueueConnection() throws JMSException
    {
        XAQueueConnection xaqc = ((XAQueueConnectionFactory)factory).createXAQueueConnection();
        QueueConnection proxy = (QueueConnection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{QueueConnection.class}, new ConnectionInvocationHandler(xaqc));
        return proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueConnectionFactory#createQueueConnection(java.lang.String,
     *      java.lang.String)
     */
    public QueueConnection createQueueConnection(String username, String password) throws JMSException
    {
        XAQueueConnection xaqc = ((XAQueueConnectionFactory)factory).createXAQueueConnection(username,
            password);
        QueueConnection proxy = (QueueConnection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{QueueConnection.class}, new ConnectionInvocationHandler(xaqc));
        return proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.TopicConnectionFactory#createTopicConnection()
     */
    public TopicConnection createTopicConnection() throws JMSException
    {
        XATopicConnection xatc = ((XATopicConnectionFactory)factory).createXATopicConnection();
        TopicConnection proxy = (TopicConnection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{TopicConnection.class}, new ConnectionInvocationHandler(xatc));
        return proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.TopicConnectionFactory#createTopicConnection(java.lang.String,
     *      java.lang.String)
     */
    public TopicConnection createTopicConnection(String username, String password) throws JMSException
    {
        XATopicConnection xatc = ((XATopicConnectionFactory)factory).createXATopicConnection(username,
            password);
        TopicConnection proxy = (TopicConnection)Proxy.newProxyInstance(Connection.class.getClassLoader(),
            new Class[]{TopicConnection.class}, new ConnectionInvocationHandler(xatc));
        return proxy;
    }

    public class ConnectionInvocationHandler implements InvocationHandler
    {

        private Object xac;

        public ConnectionInvocationHandler(Object xac)
        {
            this.xac = xac;
        }

        /**
         * Can be one of 3 types.
         * TODO check if we can portably cast it (JMS 1.1 vs 1.0.2b), see Jms102bSupport why
         * @return underlying XAConnection instance
         */
        public Object getTargetObject()
        {
            return xac;
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
            if (method.getName().equals("createSession"))
            {
                XASession xas = ((XAConnection)xac).createXASession();
                return Proxy.newProxyInstance(Session.class.getClassLoader(), new Class[]{Session.class},
                    new SessionInvocationHandler(xas, xas.getSession(), xas.getXAResource()));
            }
            else if (method.getName().equals("createQueueSession"))
            {
                XAQueueSession xaqs = ((XAQueueConnection)xac).createXAQueueSession();
                return Proxy.newProxyInstance(Session.class.getClassLoader(),
                    new Class[]{QueueSession.class}, new SessionInvocationHandler(xaqs, xaqs.getQueueSession(),
                        xaqs.getXAResource()));
            }
            else if (method.getName().equals("createTopicSession"))
            {
                XATopicSession xats = ((XATopicConnection)xac).createXATopicSession();
                return Proxy.newProxyInstance(Session.class.getClassLoader(),
                    new Class[]{TopicSession.class}, new SessionInvocationHandler(xats, xats.getTopicSession(),
                        xats.getXAResource()));
            }
            else
            {
                return method.invoke(xac, args);
            }
        }

        protected class SessionInvocationHandler implements InvocationHandler
        {

            private Object session;
            private Object xasession;
            private Object xares;
            private Transaction tx;

            public SessionInvocationHandler(Object xasession, Object session, Object xares)
            {
                this.xasession = xasession;
                this.session = session;
                this.xares = xares;
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
                Object result = method.invoke(session, args);

                if (result instanceof TopicSubscriber)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{TopicSubscriber.class}, new ConsumerProducerInvocationHandler(result));
                }
                else if (result instanceof QueueReceiver)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{QueueReceiver.class}, new ConsumerProducerInvocationHandler(result));
                }
                else if (result instanceof MessageConsumer)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{MessageConsumer.class}, new ConsumerProducerInvocationHandler(result));
                }
                else if (result instanceof TopicPublisher)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{TopicPublisher.class}, new ConsumerProducerInvocationHandler(result));
                }
                else if (result instanceof QueueSender)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{QueueSender.class}, new ConsumerProducerInvocationHandler(result));
                }
                else if (result instanceof MessageProducer)
                {
                    result = Proxy.newProxyInstance(Session.class.getClassLoader(),
                        new Class[]{MessageProducer.class}, new ConsumerProducerInvocationHandler(result));
                }
                return result;
            }

            protected void enlist() throws Exception
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Enlistment request: " + this);
                }
                if (tx == null && tm != null)
                {
                    tx = tm.getTransaction();
                    if (tx != null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Enlisting resource in xa transaction: " + xares);
                        }
                        XAResource xares = (XAResource)Proxy.newProxyInstance(XAResource.class
                            .getClassLoader(), new Class[]{XAResource.class},
                            new XAResourceInvocationHandler());
                        tx.enlistResource(xares);
                    }
                }
            }

            protected class XAResourceInvocationHandler implements InvocationHandler
            {

                /*
                 * (non-Javadoc)
                 * 
                 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
                 *      java.lang.reflect.Method, java.lang.Object[])
                 */
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                {
                    try
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Invoking " + method);
                        }
                        if (method.getName().equals("end"))
                        {
                            tx = null;
                        }

                        /*
                         * This has been added, since JOTM checks if the resource has
                         * actually been enlisted & tries to compare two proxy
                         * classes with eachother. Since the equals method is
                         * proxied, it will effectivly compare a proxy with the
                         * ConnectionFactory & this will fail. To solve this, if the
                         * object passed as a parameter is actually another proxy,
                         * call equals on the proxy passing this class as a
                         * parameter, effictively we would be comparing the two
                         * proxied classes.
                         */
                        if (method.getName().equals("equals"))
                        {
                            if (Proxy.isProxyClass(args[0].getClass()))
                            {
                                return new Boolean(args[0].equals(this));
                            }
                            else
                            {
                                return new Boolean(this.equals(args[0]));
                            }
                        }

                        return method.invoke(xares, args);
                    }
                    catch (InvocationTargetException e)
                    {
                        throw e.getCause();
                    }
                }

            }

            protected class ConsumerProducerInvocationHandler implements InvocationHandler
            {

                private Object target;

                public ConsumerProducerInvocationHandler(Object target)
                {
                    this.target = target;
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
                    if (!method.getName().equals("close"))
                    {
                        enlist();
                    }
                    return method.invoke(target, args);
                }
            }

        }
    }

}
