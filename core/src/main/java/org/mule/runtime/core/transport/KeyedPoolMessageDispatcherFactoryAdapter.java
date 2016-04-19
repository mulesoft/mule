/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.api.transport.MessageDispatcherFactory;
import org.mule.runtime.core.config.i18n.CoreMessages;

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
        OutboundEndpoint endpoint = (OutboundEndpoint)key;
        //Ensure dispatcher has the same lifecycle as the connector
        applyLifecycle((MessageDispatcher)obj);

        factory.activate((OutboundEndpoint) key, (MessageDispatcher) obj);
    }

    public void destroyObject(Object key, Object obj) throws Exception
    {
        factory.destroy((OutboundEndpoint) key, (MessageDispatcher) obj);
    }

    public Object makeObject(Object key) throws Exception
    {
        OutboundEndpoint endpoint = (OutboundEndpoint) key;
        MessageDispatcher dispatcher = factory.create(endpoint);
        applyLifecycle(dispatcher);
        return dispatcher;
    }

    public void passivateObject(Object key, Object obj) throws Exception
    {
        factory.passivate((OutboundEndpoint) key, (MessageDispatcher) obj);
    }

    public boolean validateObject(Object key, Object obj)
    {
        return factory.validate((OutboundEndpoint) key, (MessageDispatcher) obj);
    }

    public boolean isCreateDispatcherPerRequest()
    {
        return factory.isCreateDispatcherPerRequest();
    }

    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return factory.create(endpoint);
    }

    public void activate(OutboundEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException
    {
        //Ensure dispatcher has the same lifecycle as the connector
        applyLifecycle(dispatcher);
        factory.activate(endpoint, dispatcher);
    }

    public void destroy(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        factory.destroy(endpoint, dispatcher);
    }

    public void passivate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        factory.passivate(endpoint, dispatcher);
    }

    public boolean validate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        return factory.validate(endpoint, dispatcher);
    }

    protected void applyLifecycle(MessageDispatcher dispatcher) throws MuleException
    {
        MessageDispatcherUtils.applyLifecycle(dispatcher);
    }

}
