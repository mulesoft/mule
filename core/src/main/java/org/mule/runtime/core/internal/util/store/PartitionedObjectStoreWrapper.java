/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartitionedObjectStoreWrapper<T extends Serializable> implements ListableObjectStore<T> {

  private String partitionName;
  private MuleContext context;
  private ListableObjectStore<T> baseStore;

  public PartitionedObjectStoreWrapper(String name, MuleContext context, ListableObjectStore<T> store) {
    partitionName = name;
    this.context = context;
    baseStore = store;
  }

  @Override
  public boolean contains(Serializable key) throws ObjectStoreException {
    return getStore().contains(new Pair<>(partitionName, key));
  }

  @Override
  public void store(Serializable key, T value) throws ObjectStoreException {
    // This required because QueuePersistenceObject store will NOT complain in
    // cases where object already exists!
    Pair<String, Serializable> qKey = new Pair<>(partitionName, key);
    synchronized (this) {
      if (getStore().contains(qKey)) {
        throw new ObjectAlreadyExistsException();
      }
      getStore().store(qKey, value);
    }
  }

  @Override
  public T retrieve(Serializable key) throws ObjectStoreException {
    return getStore().retrieve(new Pair<>(partitionName, key));
  }

  @Override
  public void clear() throws ObjectStoreException {
    for (Serializable key : this.allKeys()) {
      this.remove(key);
    }
  }

  @Override
  public T remove(Serializable key) throws ObjectStoreException {
    return getStore().remove(new Pair<>(partitionName, key));
  }

  @Override
  public boolean isPersistent() {
    return getStore().isPersistent();
  }

  @Override
  public void open() throws ObjectStoreException {
    getStore().open();
  }

  @Override
  public void close() throws ObjectStoreException {
    getStore().close();
  }

  @Override
  public List<Serializable> allKeys() throws ObjectStoreException {
    // TODO this is NOT efficient!
    List<Serializable> results = new ArrayList<>();
    List<Serializable> keys = getStore().allKeys();
    for (Serializable key : keys) {
      Pair<String, Serializable> qKey = (Pair<String, Serializable>) key;
      if (qKey.getFirst().equals(partitionName)) {
        results.add(qKey.getSecond());
      }
    }
    return results;
  }

  private ListableObjectStore<T> getStore() {
    return baseStore;
  }

  public ListableObjectStore<T> getBaseStore() {
    return getStore();
  }

}
