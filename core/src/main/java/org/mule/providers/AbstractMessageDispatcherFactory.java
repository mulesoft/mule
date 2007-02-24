/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.util.ClassUtils;

/**
 * <code>AbstractMessageDispatcherFactory</code> is a base implementation of the
 * <code>UMOMessageDispatcherFactory</code> interface for managing the lifecycle of
 * message dispatchers.
 * 
 * @see UMOMessageDispatcherFactory
 */
public abstract class AbstractMessageDispatcherFactory implements UMOMessageDispatcherFactory
{

    public AbstractMessageDispatcherFactory()
    {
        super();
    }

    /**
     * This default implementation of
     * {@link UMOMessageDispatcherFactory#isCreateDispatcherPerRequest()} returns
     * <code>false</code>, which means that dispatchers are pooled according to
     * their lifecycle as described in {@link UMOMessageDispatcher}.
     * 
     * @return <code>false</code> by default, unless overwritten by a subclass.
     */
    public boolean isCreateDispatcherPerRequest()
    {
        return false;
    }

    public abstract UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException;

    public void activate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher) throws UMOException
    {
        dispatcher.activate();
    }

    public void destroy(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        dispatcher.dispose();
    }

    public void passivate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        dispatcher.passivate();
    }

    public boolean validate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        // Unless dispatchers are to be disposed of after every request, we check if
        // the dispatcher is still valid or has e.g. disposed itself after an
        // exception.
        return (this.isCreateDispatcherPerRequest() ? false : dispatcher.validate());
    }

    // @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer(60);
        sb.append(ClassUtils.getShortClassName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", createDispatcherPerRequest=").append(this.isCreateDispatcherPerRequest());
        sb.append('}');
        return sb.toString();
    }

}
