/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>UMOMessageDispatcherFactory</code> is a factory interface for managing the
 * lifecycle of a message dispatcher for the underlying transport.
 */
public interface UMOMessageDispatcherFactory
{
    UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException;

    void activate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher) throws UMOException;

    boolean validate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);

    void passivate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);

    void destroy(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);
}
