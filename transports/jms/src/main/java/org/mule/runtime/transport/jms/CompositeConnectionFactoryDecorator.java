/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleContext;

import java.util.Collection;
import java.util.LinkedList;

import javax.jms.ConnectionFactory;

public class CompositeConnectionFactoryDecorator implements ConnectionFactoryDecorator
{

    private LinkedList<ConnectionFactoryDecorator> decorators = new LinkedList<ConnectionFactoryDecorator>();

    public CompositeConnectionFactoryDecorator()
    {
        decorators.add(new DefaultConnectionFactoryDecorator());
        decorators.add(new CachingConnectionFactoryDecorator());
    }

    @Override
    public ConnectionFactory decorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext mulecontext)
    {
        for (ConnectionFactoryDecorator decorator : decorators)
        {
            if (decorator.appliesTo(connectionFactory, mulecontext))
            {
                return decorator.decorate(connectionFactory, jmsConnector, mulecontext);
            }
        }
        return connectionFactory;
    }

    @Override
    public boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext)
    {
        return true;
    }

    public void init(MuleContext muleContext)
    {
        Collection<ConnectionFactoryDecorator> connectionFactoryDecorators = muleContext.getRegistry().lookupObjects(ConnectionFactoryDecorator.class);
        for (ConnectionFactoryDecorator connectionFactoryDecorator : connectionFactoryDecorators)
        {
            decorators.addFirst(connectionFactoryDecorator);
        }
    }

}
