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

public abstract class AbstractMessageDispatcherFactory implements UMOMessageDispatcherFactory
{

    public AbstractMessageDispatcherFactory()
    {
        super();
    }

    public abstract UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException;

    public void activate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher) throws UMOException
    {
        // template method
    }

    public void destroy(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        // by default we simply dispose the dispatcher.
        dispatcher.dispose();
    }

    public void passivate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        // template method
    }

    public boolean validate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        return true;
    }

}
