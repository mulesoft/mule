/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.store.AbstractPartitionableObjectStore;
import org.mule.runtime.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Will wrap a provided object store or a newly created one with the provided factory, with the provided having precedence if
 * present.
 * <p/>
 * In the case the factory is used and a fresh object store is created, its lifecycle management will be delegated by this
 * wrapper.
 */
public class ProvidedPartitionableObjectStoreWrapper<T extends Serializable> extends AbstractPartitionableObjectStore<T> {

  private PartitionableObjectStore<T> wrapped;

  /**
   * Wraps the {@code providedObjectStore} if given, or uses the {@code objectStoreSupplier} to create one.
   *
   * @param providedObjectStore the objectStroe provided through config to use. May be null.
   * @param objectStoreSupplier provides the object store to use if {@code providedObjectStore} is null.
   */

  /**
   * Wraps the {@code providedObjectStore} if given, or uses the {@code objectStoreSupplier} to create one.
   *
   * @param providedObjectStore the objectStroe provided through config to use. May be null.
   * @param objectStoreSupplier provides the object store to use if {@code providedObjectStore} is null.
   */
  public ProvidedPartitionableObjectStoreWrapper(PartitionableObjectStore<T> providedObjectStore,
                                                 Supplier<PartitionableObjectStore> objectStoreSupplier) {
    if (providedObjectStore == null) {
      wrapped = objectStoreSupplier.get();
    } else {
      wrapped = providedObjectStore;
    }
  }

  @Override
  public void open() throws ObjectStoreException {
    wrapped.open();
  }

  @Override
  public void close() throws ObjectStoreException {
    wrapped.close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return wrapped.allKeys();
  }

  @Override
  protected boolean doContains(String key, String partitionName) throws ObjectStoreException {
    return wrapped.contains(key, partitionName);
  }

  @Override
  protected void doStore(String key, T value, String partitionName) throws ObjectStoreException {
    wrapped.store(key, value, partitionName);
  }

  @Override
  protected T doRetrieve(String key, String partitionName) throws ObjectStoreException {
    return wrapped.retrieve(key, partitionName);
  }

  @Override
  protected T doRemove(String key, String partitionName) throws ObjectStoreException {
    return wrapped.remove(key, partitionName);
  }

  @Override
  public List<String> allKeys(String partitionName) throws ObjectStoreException {
    return wrapped.allKeys(partitionName);
  }

  @Override
  public Map<String, T> retrieveAll(String partitionName) throws ObjectStoreException {
    return wrapped.retrieveAll(partitionName);
  }

  @Override
  public List<String> allPartitions() throws ObjectStoreException {
    return wrapped.allPartitions();
  }

  @Override
  public void open(String partitionName) throws ObjectStoreException {
    wrapped.open(partitionName);
  }

  @Override
  public void close(String partitionName) throws ObjectStoreException {
    wrapped.close(partitionName);
  }

  @Override
  public void disposePartition(String partitionName) throws ObjectStoreException {
    wrapped.disposePartition(partitionName);
  }

  @Override
  public void clear(String partitionName) throws ObjectStoreException {
    wrapped.clear(partitionName);
  }

  @Override
  public boolean isPersistent() {
    return wrapped.isPersistent();
  }
}
