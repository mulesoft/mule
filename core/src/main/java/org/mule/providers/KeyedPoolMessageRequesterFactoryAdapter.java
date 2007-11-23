/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageRequester;
import org.mule.umo.provider.UMOMessageRequesterFactory;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>KeyedPoolMessageRequesterFactoryAdapter</code> adapts a
 * <code>UMOMessageRequesterFactory</code> with methods from commons-pool
 * <code>KeyedPoolableObjectFactory</code>. It is only required for requester
 * factories that do not inherit from <code>AbstractMessageRequesterFactory</code>.
 *
 * @see org.mule.providers.AbstractMessageRequesterFactory
 */
public class KeyedPoolMessageRequesterFactoryAdapter
    implements UMOMessageRequesterFactory, KeyedPoolableObjectFactory
{
    private final UMOMessageRequesterFactory factory;

    public KeyedPoolMessageRequesterFactoryAdapter(UMOMessageRequesterFactory factory)
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
        factory.activate((UMOImmutableEndpoint) key, (UMOMessageRequester) obj);
    }

    public void destroyObject(Object key, Object obj) throws Exception
    {
        factory.destroy((UMOImmutableEndpoint) key, (UMOMessageRequester) obj);
    }

    public Object makeObject(Object key) throws Exception
    {
        return factory.create((UMOImmutableEndpoint) key);
    }

    public void passivateObject(Object key, Object obj) throws Exception
    {
        factory.passivate((UMOImmutableEndpoint) key, (UMOMessageRequester) obj);
    }

    public boolean validateObject(Object key, Object obj)
    {
        return factory.validate((UMOImmutableEndpoint) key, (UMOMessageRequester) obj);
    }

    public boolean isCreateRequesterPerRequest()
    {
        return factory.isCreateRequesterPerRequest();
    }

    public UMOMessageRequester create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return factory.create(endpoint);
    }

    public void activate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester) throws UMOException
    {
        factory.activate(endpoint, requester);
    }

    public void destroy(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
    {
        factory.destroy(endpoint, requester);
    }

    public void passivate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
    {
        factory.passivate(endpoint, requester);
    }

    public boolean validate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
    {
        return factory.validate(endpoint, requester);
    }

}