/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageRequester;
import org.mule.compatibility.core.api.transport.MessageRequesterFactory;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>KeyedPoolMessageRequesterFactoryAdapter</code> adapts a
 * <code>MessageRequesterFactory</code> with methods from commons-pool
 * <code>KeyedPoolableObjectFactory</code>. It is only required for requester
 * factories that do not inherit from <code>AbstractMessageRequesterFactory</code>.
 *
 * @see org.mule.compatibility.core.transport.AbstractMessageRequesterFactory
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

    @Override
    public void activateObject(Object key, Object obj) throws Exception
    {
        //Ensure requester has the same lifecycle as the connector
        applyLifecycle((MessageRequester)obj, false);

        factory.activate((InboundEndpoint) key, (MessageRequester) obj);
    }

    @Override
    public void destroyObject(Object key, Object obj) throws Exception
    {
        factory.destroy((InboundEndpoint) key, (MessageRequester) obj);
    }

    @Override
    public Object makeObject(Object key) throws Exception
    {
        Object obj = factory.create((InboundEndpoint) key);
        applyLifecycle((MessageRequester)obj, true);
        return obj;
    }

    @Override
    public void passivateObject(Object key, Object obj) throws Exception
    {
        factory.passivate((InboundEndpoint) key, (MessageRequester) obj);
    }

    @Override
    public boolean validateObject(Object key, Object obj)
    {
        return factory.validate((InboundEndpoint) key, (MessageRequester) obj);
    }

    @Override
    public boolean isCreateRequesterPerRequest()
    {
        return factory.isCreateRequesterPerRequest();
    }

    @Override
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        return factory.create(endpoint);
    }

    @Override
    public void activate(InboundEndpoint endpoint, MessageRequester requester) throws MuleException
    {
        factory.activate(endpoint, requester);
    }

    @Override
    public void destroy(InboundEndpoint endpoint, MessageRequester requester)
    {
        factory.destroy(endpoint, requester);
    }

    @Override
    public void passivate(InboundEndpoint endpoint, MessageRequester requester)
    {
        factory.passivate(endpoint, requester);
    }

    @Override
    public boolean validate(InboundEndpoint endpoint, MessageRequester requester)
    {
        return factory.validate(endpoint, requester);
    }

    protected void applyLifecycle(MessageRequester requester, boolean created) throws MuleException
    {
        String phase = ((AbstractConnector)requester.getConnector()).getLifecycleManager().getCurrentPhase();
        if(phase.equals(Startable.PHASE_NAME) && !requester.getLifecycleState().isStarted())
        {
            if(!requester.getLifecycleState().isInitialised())
            {
                requester.initialise();
            }
            requester.start();
        }
        else if(phase.equals(Stoppable.PHASE_NAME) && requester.getLifecycleState().isStarted())
        {
            requester.stop();
        }
        else if(Disposable.PHASE_NAME.equals(phase))
        {
            requester.dispose();
        }
    }
}
