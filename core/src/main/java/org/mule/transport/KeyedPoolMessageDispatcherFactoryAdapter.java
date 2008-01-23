/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.config.i18n.CoreMessages;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>KeyedPoolMessageDispatcherFactoryAdapter</code> adapts a
 * <code>MessageDispatcherFactory</code> with methods from commons-pool
 * <code>KeyedPoolableObjectFactory</code>. It is only required for dispatcher
 * factories that do not inherit from <code>AbstractMessageDispatcherFactory</code>.
 * 
 * @see AbstractMessageDispatcherFactory
 */
public class KeyedPoolMessageDispatcherFactoryAdapter
    implements MessageDispatcherFactory, KeyedPoolableObjectFactory
{
    private final MessageDispatcherFactory factory;

    public KeyedPoolMessageDispatcherFactoryAdapter(MessageDispatcherFactory factory)
    {
        super();

        if (factory == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("factory").toString());
        }

        this.factory = factory;
    }

    public void activateObject(Object key, Object obj) throws Exception
    {
        factory.activate((ImmutableEndpoint) key, (MessageDispatcher) obj);
    }

    public void destroyObject(Object key, Object obj) throws Exception
    {
        factory.destroy((ImmutableEndpoint) key, (MessageDispatcher) obj);
    }

    public Object makeObject(Object key) throws Exception
    {
        return factory.create((ImmutableEndpoint) key);
    }

    public void passivateObject(Object key, Object obj) throws Exception
    {
        factory.passivate((ImmutableEndpoint) key, (MessageDispatcher) obj);
    }

    public boolean validateObject(Object key, Object obj)
    {
        return factory.validate((ImmutableEndpoint) key, (MessageDispatcher) obj);
    }

    public boolean isCreateDispatcherPerRequest()
    {
        return factory.isCreateDispatcherPerRequest();
    }

    public MessageDispatcher create(ImmutableEndpoint endpoint) throws MuleException
    {
        return factory.create(endpoint);
    }

    public void activate(ImmutableEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException
    {
        factory.activate(endpoint, dispatcher);
    }

    public void destroy(ImmutableEndpoint endpoint, MessageDispatcher dispatcher)
    {
        factory.destroy(endpoint, dispatcher);
    }

    public void passivate(ImmutableEndpoint endpoint, MessageDispatcher dispatcher)
    {
        factory.passivate(endpoint, dispatcher);
    }

    public boolean validate(ImmutableEndpoint endpoint, MessageDispatcher dispatcher)
    {
        return factory.validate(endpoint, dispatcher);
    }

}
