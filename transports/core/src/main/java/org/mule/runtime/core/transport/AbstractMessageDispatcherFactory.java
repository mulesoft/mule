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
import org.mule.runtime.core.util.ClassUtils;

/**
 * <code>AbstractMessageDispatcherFactory</code> is a base implementation of the
 * <code>MessageDispatcherFactory</code> interface for managing the lifecycle of
 * message dispatchers.
 * 
 * @see MessageDispatcherFactory
 */
public abstract class AbstractMessageDispatcherFactory implements MessageDispatcherFactory
{

    public AbstractMessageDispatcherFactory()
    {
        super();
    }

    /**
     * This default implementation of
     * {@link MessageDispatcherFactory#isCreateDispatcherPerRequest()} returns
     * <code>false</code>, which means that dispatchers are pooled according to
     * their lifecycle as described in {@link MessageDispatcher}.
     * 
     * @return <code>false</code> by default, unless overwritten by a subclass.
     */
    @Override
    public boolean isCreateDispatcherPerRequest()
    {
        return false;
    }

    @Override
    public abstract MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException;

    @Override
    public void activate(OutboundEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException
    {
        dispatcher.activate();
    }

    @Override
    public void destroy(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        dispatcher.dispose();
    }

    @Override
    public void passivate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        dispatcher.passivate();
    }

    @Override
    public boolean validate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        // Unless dispatchers are to be disposed of after every request, we check if
        // the dispatcher is still valid or has e.g. disposed itself after an
        // exception.
        return (this.isCreateDispatcherPerRequest() ? false : dispatcher.validate());
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(60);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", createDispatcherPerRequest=").append(this.isCreateDispatcherPerRequest());
        sb.append('}');
        return sb.toString();
    }

}
