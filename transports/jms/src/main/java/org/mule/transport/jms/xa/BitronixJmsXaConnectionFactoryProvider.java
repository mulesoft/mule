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
 * This class it's used to be able to create a bitronix jms connection pool from an already created
 * ConnectionFactory since BTM does not support it yet.
 *
 * This class workaround this issue by setting a ConnectionFactory in xaConnectionFactoryProvided and then
 * when BTM instanciate BitronixJmsXaConnectionFactoryProvider through reflection it uses that ConnectionFactory
 * as delegate for the operations.
 */
public class BitronixJmsXaConnectionFactoryProvider implements ConnectionFactory, XAConnectionFactory
{

    private final XAConnectionFactory xaConnectionFactory;
    private final ConnectionFactory connectionFactory;
    public static ConnectionFactory xaConnectionFactoryProvided;

    public BitronixJmsXaConnectionFactoryProvider()
    {
        this.connectionFactory = xaConnectionFactoryProvided;
        this.xaConnectionFactory = (XAConnectionFactory) xaConnectionFactoryProvided;
        xaConnectionFactoryProvided = null;
    }

    @Override
    public Connection createConnection() throws JMSException
    {
        return connectionFactory.createConnection();
    }

    @Override
    public Connection createConnection(String username, String password) throws JMSException
    {
        return connectionFactory.createConnection(username, password);
    }

    @Override
    public XAConnection createXAConnection() throws JMSException
    {
        return xaConnectionFactory.createXAConnection();
    }

    @Override
    public XAConnection createXAConnection(String username, String password) throws JMSException
    {
        return xaConnectionFactory.createXAConnection(username, password);
    }
}
