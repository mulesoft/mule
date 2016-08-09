/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections.Factory;

/**
 * Will wrap a provided object store or a newly created one with the provided factory, with the provided having precedence if
 * present.
 * <p/>
 * In the case the factory is used and a fresh object store is created, its lifecycle management will be delegated by this
 * wrapper.
 */
public class ProvidedPartitionableObjectStoreWrapper<T extends Serializable> extends ProvidedObjectStoreWrapper<T>
    implements PartitionableObjectStore<T> {

  /**
   * Wraps the {@code providedObjectStore} if given, or uses the {@code objectStoreFactory} to create one.
   * 
   * @param providedObjectStore the objectStroe provided through config to use. May be null.
   * @param objectStoreFactory the factory to use to build an object store if {@code providedObjectStore} is null.
   */
  public ProvidedPartitionableObjectStoreWrapper(PartitionableObjectStore<T> providedObjectStore, Factory objectStoreFactory) {
    super(providedObjectStore, objectStoreFactory);
  }

  @Override
  public void open() throws ObjectStoreException {
    getWrapped().open();
  }

  @Override
  public void close() throws ObjectStoreException {
    getWrapped().close();
  }

  @Override
  public List<Serializable> allKeys() throws ObjectStoreException {
    return getWrapped().allKeys();
  }

  @Override
  public boolean contains(Serializable key, String partitionName) throws ObjectStoreException {
    return getWrapped().contains(key, partitionName);
  }

  @Override
  public void store(Serializable key, T value, String partitionName) throws ObjectStoreException {
    getWrapped().store(key, value, partitionName);
  }

  @Override
  public T retrieve(Serializable key, String partitionName) throws ObjectStoreException {
    return getWrapped().retrieve(key, partitionName);
  }

  @Override
  public T remove(Serializable key, String partitionName) throws ObjectStoreException {
    return getWrapped().remove(key, partitionName);
  }

  @Override
  public List<Serializable> allKeys(String partitionName) throws ObjectStoreException {
    return getWrapped().allKeys(partitionName);
  }

  @Override
  public List<String> allPartitions() throws ObjectStoreException {
    return getWrapped().allPartitions();
  }

  @Override
  public void open(String partitionName) throws ObjectStoreException {
    getWrapped().open(partitionName);
  }

  @Override
  public void close(String partitionName) throws ObjectStoreException {
    getWrapped().close(partitionName);
  }

  @Override
  public void disposePartition(String partitionName) throws ObjectStoreException {
    getWrapped().disposePartition(partitionName);
  }

  @Override
  public void clear(String partitionName) throws ObjectStoreException {
    getWrapped().clear(partitionName);
  }

  @Override
  protected PartitionableObjectStore<T> getWrapped() {
    return (PartitionableObjectStore<T>) super.getWrapped();
  }
}
