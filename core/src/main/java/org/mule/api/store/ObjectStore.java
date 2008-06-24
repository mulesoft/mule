/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

/**
 * TODO
 */

public interface ObjectStore
{
    /**
     * Check whether the given Object is already registered with this store.
     *
     * @param id the ID to check
     * @return <code>true</code> if the ID is stored or <code>false</code> if it could
     *         not be found
     * @throws IllegalArgumentException if the given ID is <code>null</code>
     * @throws Exception if any implementation-specific error occured, e.g. when the store
     *             is not available
     */
    boolean containsObject(String id) throws Exception;

    /**
     * Store the given Object.
     *
     * @param id the ID to store
     * @param item the Object to store with the id
     * @return <code>true</code> if the ID was stored properly, or <code>false</code>
     *         if it already existed
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *             <code>null</code>
     * @throws Exception if the store is not available or any other
     *             implementation-specific error occured
     */
    boolean storeObject(String id, Object item) throws Exception;

    /**
     * Retrieve the given Object.
     *
     * @param id the ID to store
     * @return the object instance associated with this id or null if there was no entry for the supplied id.
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *             <code>null</code>
     * @throws Exception if the store is not available or any other
     *             implementation-specific error occured
     */
    Object retrieveObject(String id) throws Exception;

    boolean removeObject(String id) throws Exception;

}
