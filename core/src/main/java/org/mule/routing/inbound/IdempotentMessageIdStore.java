/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

/**
 * <code>IdempotentMessageIdStore</code> is the main interface used by
 * {@link IdempotentReceiver} for storing received message IDs.
 * 
 * @see {@link IdempotentInMemoryMessageIdStore}
 */
public interface IdempotentMessageIdStore
{
    /**
     * Check whether the given ID is already registered with this store.
     * 
     * @param id the ID to check
     * @return <code>true</code> if the ID is stored or <code>false</code> if it could
     *         not be found
     * @throws IllegalArgumentException if the given ID is <code>null</code>
     * @throws Exception if any implementation-specific error occured, e.g. when the store
     *             is not available
     */
    boolean containsId(Object id) throws IllegalArgumentException, Exception;

    /**
     * Store the given ID.
     * 
     * @param id the ID to store
     * @return <code>true</code> if the ID was stored properly, or <code>false</code>
     *         if it already existed
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *             <code>null</code>
     * @throws Exception if the store is not available or any other
     *             implementation-specific error occured
     */
    boolean storeId(Object id) throws IllegalArgumentException, Exception;
}
