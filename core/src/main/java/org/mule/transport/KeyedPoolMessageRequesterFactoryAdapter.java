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
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.config.i18n.CoreMessages;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>KeyedPoolMessageRequesterFactoryAdapter</code> adapts a
 * <code>MessageRequesterFactory</code> with methods from commons-pool
 * <code>KeyedPoolableObjectFactory</code>. It is only required for requester
 * factories that do not inherit from <code>AbstractMessageRequesterFactory</code>.
 *
 * @see org.mule.transport.AbstractMessageRequesterFactory
 */
public class KeyedPoolMessageRequesterFactoryAdapter
    implements MessageRequesterFactory, KeyedPoolableObjectFactory
{
    private final MessageRequesterFactory factory;

    public KeyedPoolMessageRequesterFactoryAdapter(MessageRequesterFactory factory)
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
        factory.activate((ImmutableEndpoint) key, (MessageRequester) obj);
    }

    public void destroyObject(Object key, Object obj) throws Exception
    {
        factory.destroy((ImmutableEndpoint) key, (MessageRequester) obj);
    }

    public Object makeObject(Object key) throws Exception
    {
        return factory.create((ImmutableEndpoint) key);
    }

    public void passivateObject(Object key, Object obj) throws Exception
    {
        factory.passivate((ImmutableEndpoint) key, (MessageRequester) obj);
    }

    public boolean validateObject(Object key, Object obj)
    {
        return factory.validate((ImmutableEndpoint) key, (MessageRequester) obj);
    }

    public boolean isCreateRequesterPerRequest()
    {
        return factory.isCreateRequesterPerRequest();
    }

    public MessageRequester create(ImmutableEndpoint endpoint) throws MuleException
    {
        return factory.create(endpoint);
    }

    public void activate(ImmutableEndpoint endpoint, MessageRequester requester) throws MuleException
    {
        factory.activate(endpoint, requester);
    }

    public void destroy(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        factory.destroy(endpoint, requester);
    }

    public void passivate(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        factory.passivate(endpoint, requester);
    }

    public boolean validate(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        return factory.validate(endpoint, requester);
    }

}