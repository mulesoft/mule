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

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageRequester;
import org.mule.umo.provider.UMOMessageRequesterFactory;
import org.mule.util.ClassUtils;

/**
 * A base implementation of the {@link org.mule.umo.provider.UMOMessageRequesterFactory} interface for managing the
 * lifecycle of message requesters.
 *
 * @see org.mule.umo.provider.UMOMessageDispatcherFactory
 */
public abstract class AbstractMessageRequesterFactory implements UMOMessageRequesterFactory
{

    public AbstractMessageRequesterFactory()
    {
        super();
    }

    /**
     * This default implementation of
     * {@link org.mule.umo.provider.UMOMessageDispatcherFactory#isCreateDispatcherPerRequest()} returns
     * <code>false</code>, which means that dispatchers are pooled according to
     * their lifecycle as described in {@link org.mule.umo.provider.UMOMessageRequester}.
     *
     * @return <code>false</code> by default, unless overwritten by a subclass.
     */
    public boolean isCreateRequesterPerRequest()
    {
        return false;
    }

    public abstract UMOMessageRequester create(UMOImmutableEndpoint endpoint) throws UMOException;

    public void activate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester) throws UMOException
    {
        requester.activate();
    }

    public void destroy(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
    {
        requester.dispose();
    }

    public void passivate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
    {
        requester.passivate();
    }

    public boolean validate(UMOImmutableEndpoint endpoint, UMOMessageRequester requester)
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