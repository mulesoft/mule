/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.transport.jms.xa.DefaultXAConnectionFactoryWrapper;

import javax.jms.ConnectionFactory;

/**
 * Decorates the jms ConnectionFactory with a {@link org.mule.runtime.transport.jms.xa.DefaultXAConnectionFactoryWrapper} in order
 * to avoid releasing jms resources before the XA transaction has ended.
 */
public class DefaultConnectionFactoryDecorator extends AbstractConnectionFactoryDecorator
{

    @Override
    protected ConnectionFactory doDecorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext muleContext)
    {
        return new DefaultXAConnectionFactoryWrapper(connectionFactory, jmsConnector.getSameRMOverrideValue());
    }

    @Override
    public boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        return !isConnectionFactoryWrapper(connectionFactory) && isConnectionFactoryXaAndThereIsATxManager(connectionFactory, muleContext);
    }

    private boolean isConnectionFactoryXaAndThereIsATxManager(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        return (isXaConnectionFactory(connectionFactory) && muleContext.getTransactionManager() != null);
    }

}
