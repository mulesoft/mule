/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import java.io.Serializable;
import java.util.List;

/**
 * An {@link ObjectStore} which has native support for partitioning. All the methods inherited from the
 * {@link ObjectStore} interface should be executed against the default partition. Plus, this interface
 * adds methods to obtain the same behaviour against specific partitions.
 * <p>
 * The Mule Runtime will automatically decide when to invoke each version of each method and the partition name
 * to use.
 *
 * @param <T> the generic type of the store's items.
 * @since 1.0
 */
public interface PartitionableObjectStore<T extends Serializable> extends ListableObjectStore<T>
{
  /**
   * Check whether the given {@code partitionName} already contains a value for the given {@code key}
   *
   * @param key the identifier of the object to check
   * @return <code>true</code> if the key is stored or <code>false</code> no value was stored for the key.
   * @throws ObjectStoreException             if the given key is <code>null</code>.
   * @throws ObjectStoreNotAvaliableException if any implementation-specific error occured, e.g. when the store is not available
   * @code partitionName the partition in which the key is to be tested
   */
    boolean contains(Serializable key, String partitionName) throws ObjectStoreException;

  /**
   * Stores the given Object in the given {@code partitionName}
   *
   * @param key   the identifier for <code>value</code>
   * @param value the Object to store with <code>key</code>
   * @throws ObjectStoreException             if the given key cannot be stored or is <code>null</code>.
   * @throws ObjectStoreNotAvaliableException if the store is not available or any other implementation-specific error occured.
   * @throws ObjectAlreadyExistsException     if an attempt is made to store an object for a key that already has an object
   *                                          associated.
   * @code partitionName the partition in which the value is to be stored
   */
    void store(Serializable key, T value, String partitionName) throws ObjectStoreException;

  /**
   * Retrieve the object stored under the given {@code key} in the given {@code partitionName}
   *
   * @param key the identifier of the object to retrieve.
   * @return the object associated with the given key. If no object for the given key was found this method throws an
   * {@link ObjectDoesNotExistException}.
   * @throws ObjectStoreException             if the given key is <code>null</code>.
   * @throws ObjectStoreNotAvaliableException if the store is not available or any other implementation-specific error occured.
   * @throws ObjectDoesNotExistException      if no value for the given key was previously stored.
   * @code partitionName the partition from which the value is to be retrieved
   */
    T retrieve(Serializable key, String partitionName) throws ObjectStoreException;

  /**
   * Remove the object with the given {@code key} in the given {@code partitionName}
   *
   * @param key the identifier of the object to remove.
   * @return the object that was previously stored for the given key
   * @throws ObjectStoreException        if the given key is <code>null</code> or if the store is not available or any other
   *                                     implementation-specific error occured
   * @throws ObjectDoesNotExistException if no value for the given key was previously stored.
   * @code partitionName the partition from which the value is to be removed
   */
    T remove(Serializable key, String partitionName) throws ObjectStoreException;

  /**
   * @return list containing all keys that the {@code partitionName} currently holds values for.
   * @throws ObjectStoreException if an exception occurred while collecting the list of all keys.
   */
    List<Serializable> allKeys(String partitionName) throws ObjectStoreException;

  /**
   * @return list containing the names of all the available partitions
   * @throws ObjectStoreException if an exception occurred while collecting the partition names
   */
    List<String> allPartitions() throws ObjectStoreException;

  /**
   * Open the partition of the given {@code partitionName}
   *
   * @param partitionName the name of the partition to open
   * @throws ObjectStoreException if an exception occurred while opening the partition
   */
    void open(String partitionName) throws ObjectStoreException;

  /**
   * Closes the partition of the give {@code partitionName}
   * @param partitionName the name of the partition to close
   * @throws ObjectStoreException if an exception occurred while closing the partition
   */
    void close(String partitionName) throws ObjectStoreException;

  /**
   * Disposes the given partition, releasing all associated resources and storage. Contents of the partition
   * will be lost as a result of executing this method.
   *
   * @param partitionName the name of the partition to dispose
   * @throws ObjectStoreException if an exception occurred while disposing the partition
   */
    void disposePartition(String partitionName) throws ObjectStoreException;

  /**
   * Clears the contents of the partition, but the partition itself remains functional
   * @param partitionName the name of the partition
   * @throws ObjectStoreException if an exception occurred while clearing the partition
   */
  void clear(String partitionName) throws ObjectStoreException;
}
