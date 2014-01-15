/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jms;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.bti.transaction.TransactionManagerWrapper;

import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import bitronix.tm.resource.jms.PoolingConnectionFactory;

/**
 * Wrapper for JMS ConnectionFactory that will return a proxy for JMS Connections to make them work inside xa transactions.
 */
public class BitronixConnectionFactoryWrapper implements ConnectionFactory, Initialisable
{

    private final PoolingConnectionFactory factory;
    private final MuleContext muleContext;


    public BitronixConnectionFactoryWrapper(PoolingConnectionFactory factory, MuleContext muleContext)
    {
        this.factory = factory;
        this.muleContext = muleContext;
    }

    public Connection createConnection() throws JMSException
    {
        Connection xac = factory.createConnection();
        Connection proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                               new Class[] {Connection.class},
                                                               new BitronixConnectionInvocationHandler(xac));
        return proxy;
    }

    public Connection createConnection(String username, String password) throws JMSException
    {
        Connection xac = factory.createConnection(username, password);
        Connection proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                               new Class[] {Connection.class},
                                                               new BitronixConnectionInvocationHandler(xac));
        return proxy;
    }

    public void close()
    {
        factory.close();
    }

    public PoolingConnectionFactory getWrappedConnectionFactory()
    {
        return factory;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (!(muleContext.getTransactionManager() instanceof TransactionManagerWrapper))
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Cannot use a Bitronix connection factory pool without " +
                                                                                 "using Bitronix transaction manager"), this);
        }
    }
}
