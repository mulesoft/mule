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

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * <code>UMOMessageDispatcherFactory</code> is a factory interface for managing the
 * lifecycles of a transport's message dispatchers. The methods basically implement
 * the {@link KeyedPoolableObjectFactory} lifecycle, with a
 * {@link UMOImmutableEndpoint} as the key and the dispatcher as pooled object.
 */
public interface UMOMessageDispatcherFactory
{
    /**
     * Creates a new message dispatcher instance, initialised with the passed
     * endpoint. The returned instance should be immediately useable.
     * 
     * @param endpoint the endoint for which this dispatcher should be created
     * @return a properly created <code>UMOMessageDispatcher</code> for this
     *         transport
     * @throws UMOException if the dispatcher cannot be created
     */
    UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException;

    /**
     * Invoked <strong>before</strong> the given dispatcher is handed out to a
     * client, but <strong>not</strong> after {@link #create(UMOImmutableEndpoint)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be activated
     * @throws UMOException if the dispatcher cannot be activated
     */
    void activate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher) throws UMOException;

    /**
     * Invoked <strong>after</strong> the dispatcher is returned from a client but
     * <strong>before</strong> it is prepared for return to its pool via
     * {@link #passivate(UMOImmutableEndpoint, UMOMessageDispatcher)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be validated
     * @return <code>true</code> if the dispatcher is valid for reuse,
     *         <code>false</code> otherwise.
     */
    boolean validate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);

    /**
     * Invoked immediately <strong>before</strong> the given dispatcher is returned
     * to its pool.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be passivated
     */
    void passivate(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);

    /**
     * Invoked when a dispatcher returned <code>false</code> for
     * {@link #validate(UMOImmutableEndpoint, UMOMessageDispatcher)}.
     * 
     * @param endpoint the endpoint of the dispatcher
     * @param dispatcher the dispatcher to be validated
     */
    void destroy(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher);
}
