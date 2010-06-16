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

public interface ObjectStore
{
    /**
     * Check whether the given Object is already registered with this store.
     *
     * @param id the identifier of the object to check
     * @return <code>true</code> if the ID is stored or <code>false</code> if it could not be found
     * @throws IllegalArgumentException if the given ID is <code>null</code>.
     * @throws Exception if any implementation-specific error occured, e.g. when the store is 
     *          not available
     */
    boolean contains(String id) throws Exception;

    /**
     * Store the given Object.
     *
     * @param id the identifier for <code>item</code>
     * @param item the Object to store with <code>id</code>
     * @return <code>true</code> if the object was stored properly, or <code>false</code> if an
     *          object with the given id already existed.
     * @throws IllegalArgumentException if the given ID cannot be stored or is <code>null</code>
     * @throws Exception if the store is not available or any other implementation-specific 
     *          error occured.
     */
    boolean store(String id, Object item) throws Exception;

    /**
     * Retrieve the given Object.
     *
     * @param id the identifier of the object to retrieve.
     * @return the object associated with the given ID or <code>null</code> if there was no entry 
     *          for the supplied ID.
     * @throws IllegalArgumentException if the given ID is <code>null</code>
     * @throws Exception if the store is not available or any other implementation-specific 
     *          error occured
     */
    Object retrieve(String id) throws Exception;

    /**
     * Remove the object with ID.
     * 
     * @param id the identifier of the object to remove.
     * @return <code>true</code> if the object was found and removed or <code>false</code> if no
     *          object with ID was stored.
     * @throws IllegalArgumentException if the given ID is <code>null</code>
     * @throws Exception if the store is not available or any other implementation-specific 
     *          error occured
     */
    boolean remove(String id) throws Exception;
}
