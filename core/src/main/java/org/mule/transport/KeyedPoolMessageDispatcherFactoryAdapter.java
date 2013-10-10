/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
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
        String phase = ((AbstractConnector)dispatcher.getConnector()).getLifecycleManager().getCurrentPhase();
        if(phase.equals(Startable.PHASE_NAME) && !dispatcher.getLifecycleState().isStarted())
        {
            if(!dispatcher.getLifecycleState().isInitialised())
            {
                dispatcher.initialise();
            }
            dispatcher.start();
        }
        else if(phase.equals(Stoppable.PHASE_NAME) && dispatcher.getLifecycleState().isStarted())
        {
            dispatcher.stop();
        }
        else if(Disposable.PHASE_NAME.equals(phase))
        {
            dispatcher.dispose();
        }
    }

}
