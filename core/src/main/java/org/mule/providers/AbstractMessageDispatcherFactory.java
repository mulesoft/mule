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
        // should dispatchers be disposed after every request?
        // TODO HH: remove evil cast, move method into interface
        if (((AbstractConnector)endpoint.getConnector()).isCreateDispatcherPerRequest())
        {
            return false;
        }

        // is the dispatcher still valid or has it e.g. disposed itself after an
        // exception?
        return dispatcher.validate();
    }

}
