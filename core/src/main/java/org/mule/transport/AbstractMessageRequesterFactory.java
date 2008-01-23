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
import org.mule.util.ClassUtils;

/**
 * A base implementation of the {@link org.mule.api.transport.MessageRequesterFactory} interface for managing the
 * lifecycle of message requesters.
 *
 * @see org.mule.api.transport.MessageDispatcherFactory
 */
public abstract class AbstractMessageRequesterFactory implements MessageRequesterFactory
{

    public AbstractMessageRequesterFactory()
    {
        super();
    }

    /**
     * This default implementation of
     * {@link org.mule.api.transport.MessageDispatcherFactory#isCreateDispatcherPerRequest()} returns
     * <code>false</code>, which means that dispatchers are pooled according to
     * their lifecycle as described in {@link org.mule.api.transport.MessageRequester}.
     *
     * @return <code>false</code> by default, unless overwritten by a subclass.
     */
    public boolean isCreateRequesterPerRequest()
    {
        return false;
    }

    public abstract MessageRequester create(ImmutableEndpoint endpoint) throws MuleException;

    public void activate(ImmutableEndpoint endpoint, MessageRequester requester) throws MuleException
    {
        requester.activate();
    }

    public void destroy(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        requester.dispose();
    }

    public void passivate(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        requester.passivate();
    }

    public boolean validate(ImmutableEndpoint endpoint, MessageRequester requester)
    {
        // Unless requesters are to be disposed of after every request, we check if
        // the requester is still valid or has e.g. disposed itself after an
        // exception.
        return (!this.isCreateRequesterPerRequest() && requester.validate());
    }

    // @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer(60);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", createRequesterPerRequest=").append(this.isCreateRequesterPerRequest());
        sb.append('}');
        return sb.toString();
    }

}