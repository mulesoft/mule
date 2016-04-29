/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.transport.jms.xa.XAConnectionFactoryWrapper;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

/**
 * Base class for every {@link ConnectionFactoryDecorator} to apply consistent behavior.
 */
public abstract class AbstractConnectionFactoryDecorator implements ConnectionFactoryDecorator
{

    @Override
    public ConnectionFactory decorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext muleContext)
    {
        Preconditions.checkState(appliesTo(connectionFactory, muleContext), "DefaultConnectionFactoryDecorator invoked but it shouldn't be called since it does not applies to the ConnectionFactory");
        return doDecorate(connectionFactory, jmsConnector, muleContext);
    }

    protected abstract ConnectionFactory doDecorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext muleContext);

    protected boolean isXaConnectionFactory(ConnectionFactory connectionFactory)
    {
        return connectionFactory instanceof XAConnectionFactory;
    }

    protected boolean isConnectionFactoryWrapper(ConnectionFactory connectionFactory)
    {
        return (connectionFactory instanceof XAConnectionFactoryWrapper);
    }
}
