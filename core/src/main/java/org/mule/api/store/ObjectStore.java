/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import java.io.Serializable;

public interface ObjectStore<T extends Serializable>
{
    /**
     * Check whether the given Object is already registered with this store.
     *
     * @param key the identifier of the object to check
     * @return <code>true</code> if the key is stored or <code>false</code> no value was stored for
     *          the key.
     * @throws ObjectStoreException if the given key is <code>null</code>.
     * @throws ObjectStoreNotAvaliableException if any implementation-specific error occured, e.g. 
     *          when the store is not available
     */
    boolean contains(Serializable key) throws ObjectStoreException;

    /**
     * Store the given Object.
     *
     * @param key the identifier for <code>value</code>
     * @param value the Object to store with <code>key</code>
     * @throws ObjectStoreException if the given key cannot be stored or is <code>null</code>.
     * @throws ObjectStoreNotAvaliableException if the store is not available or any other 
     *          implementation-specific error occured.
     * @throws ObjectAlreadyExistsException if an attempt is made to store an object for a key
     *          that already has an object associated.
     */
    void store(Serializable key, T value) throws ObjectStoreException;

    /**
     * Retrieve the given Object.
     *
     * @param key the identifier of the object to retrieve.
     * @return the object associated with the given key. If no object for the given key was found
     *          this method throws an {@link ObjectDoesNotExistException}.
     * @throws ObjectStoreException if the given key is <code>null</code>.
     * @throws ObjectStoreNotAvaliableException if the store is not  available or any other 
     *          implementation-specific error occured.
     * @throws ObjectDoesNotExistException if no value for the given key was previously stored.
     */
    T retrieve(Serializable key) throws ObjectStoreException;

    /**
     * Remove the object with key.
     * 
     * @param key the identifier of the object to remove.
     * @return the object that was previously stored for the given key
     * @throws ObjectStoreException if the given key is <code>null</code> or if the store is not 
     *          available or any other implementation-specific error occured
     * @throws ObjectDoesNotExistException if no value for the given key was previously stored.
     */
    T remove(Serializable key) throws ObjectStoreException;

    /**
     * Is this store persistent?
     *
     * @return true if this store is persistent
     */
    boolean isPersistent();
}
