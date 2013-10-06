/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.xa;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;

/**
 *
 */
public class BitronixJmsXaConnectionFactoryProvider implements ConnectionFactory,XAConnectionFactory
{

    private final XAConnectionFactory xaConnectionFactory;
    private final ConnectionFactory connectionFactory;
    public static ConnectionFactory xaConnectionFactoryThreadLocal;

    public BitronixJmsXaConnectionFactoryProvider()
    {
        this.connectionFactory = xaConnectionFactoryThreadLocal;
        this.xaConnectionFactory = (XAConnectionFactory)xaConnectionFactoryThreadLocal;
        xaConnectionFactoryThreadLocal = null;

    }

    @Override
    public Connection createConnection() throws JMSException
    {
        return connectionFactory.createConnection();
    }

    @Override
    public Connection createConnection(String s, String s2) throws JMSException
    {
        return connectionFactory.createConnection(s,s2);
    }

    @Override
    public XAConnection createXAConnection() throws JMSException
    {
        return xaConnectionFactory.createXAConnection();
    }

    @Override
    public XAConnection createXAConnection(String s, String s2) throws JMSException
    {
        return xaConnectionFactory.createXAConnection(s,s2);
    }
}
