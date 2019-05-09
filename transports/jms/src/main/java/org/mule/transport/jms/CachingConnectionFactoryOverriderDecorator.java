/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.MuleContext;

import javax.jms.ConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Decorator that overrides a Spring-bean defined {@link CachingConnectionFactory}, putting instead a {@link CustomCachingConnectionFactory},
 * which we have control over.
 */
public class CachingConnectionFactoryOverriderDecorator implements ConnectionFactoryDecorator
{

    CustomCachingConnectionFactory decoratedFactory;

    @Override
    public ConnectionFactory decorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext mulecontext)
    {
        ConnectionFactory targetConnectionFactory = ((CachingConnectionFactory) connectionFactory).getTargetConnectionFactory();
        decoratedFactory = new CustomCachingConnectionFactory(targetConnectionFactory, jmsConnector.getUsername(), jmsConnector.getPassword(), jmsConnector);
        // TODO: Extract somehow from the Spring-bean-defined CachingConnectionFactory its properties, and configure them in
        // the CustomCachingConnectionFactory.

        return decoratedFactory;
    }

    @Override
    public boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        return connectionFactory instanceof CachingConnectionFactory;
    }
}
