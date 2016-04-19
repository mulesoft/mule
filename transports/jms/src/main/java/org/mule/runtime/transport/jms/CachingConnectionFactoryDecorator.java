/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;

import javax.jms.ConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Decorates the JMS {@link javax.jms.ConnectionFactory} with a {@link org.mule.runtime.transport.jms.CustomCachingConnectionFactory}
 * in order to ensure JMS session instances are reused if {@link JmsConnector#isCacheJmsSessions()} is 'true' and the
 * supplied connection factory isn't already an instance of {@link org.mule.runtime.transport.jms.CustomCachingConnectionFactory}
 * NOTE: Currently only Non-XA JMS 1.1 {@link javax.jms.ConnectionFactory}'s will be decorated to provide caching.
 */
public class CachingConnectionFactoryDecorator extends AbstractConnectionFactoryDecorator
{

    protected CustomCachingConnectionFactory cachingConnectionFactory;

    @Override
    protected ConnectionFactory doDecorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext muleContext)
    {
        if (resolveReuseSessionsEnabled(jmsConnector))
        {
            cachingConnectionFactory = new CustomCachingConnectionFactory(connectionFactory, jmsConnector.getUsername(),
                                                                          jmsConnector.getPassword());
            cachingConnectionFactory.setCacheConsumers(false);
            cachingConnectionFactory.setCacheProducers(true);
            cachingConnectionFactory.setSessionCacheSize(Integer.MAX_VALUE);
            cachingConnectionFactory.setClientId(jmsConnector.getClientId());
            cachingConnectionFactory.setExceptionListener(jmsConnector);
            cachingConnectionFactory.setReconnectOnException(false);
            return cachingConnectionFactory;
        }
        else
        {
            return connectionFactory;
        }
    }

    private boolean resolveReuseSessionsEnabled(JmsConnector jmsConnector)
    {
        // CachingConnectionFactory only supports JMS 1.1 connection factories currently.
        return (jmsConnector.getJmsSupport() instanceof Jms11Support)
               && !(jmsConnector.getJmsSupport() instanceof Jms102bSupport)
               && jmsConnector.isCacheJmsSessions();
    }

    @Override
    public boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        // We only wrap connection factories that i) aren't instances of XAConnectionFactory ii) haven't already been
        // wrapped.
        // Note: we need to explicitly check for instances or CachingConnectionFactory here JMSConnection currently
        // allows the ConnectionFactory to be decorated on each reconnection.
        return !isXaConnectionFactory(connectionFactory)
               && !isConnectionFactoryWrapper(connectionFactory)
               && !(connectionFactory instanceof CachingConnectionFactory);
    }

}
