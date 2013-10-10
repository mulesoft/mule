/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
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

    public abstract MessageRequester create(InboundEndpoint endpoint) throws MuleException;

    public void activate(InboundEndpoint endpoint, MessageRequester requester) throws MuleException
    {
        requester.activate();
    }

    public void destroy(InboundEndpoint endpoint, MessageRequester requester)
    {
        requester.dispose();
    }

    public void passivate(InboundEndpoint endpoint, MessageRequester requester)
    {
        requester.passivate();
    }

    public boolean validate(InboundEndpoint endpoint, MessageRequester requester)
    {
        // Unless requesters are to be disposed of after every request, we check if
        // the requester is still valid or has e.g. disposed itself after an
        // exception.
        return (!this.isCreateRequesterPerRequest() && requester.validate());
    }

    @Override
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
