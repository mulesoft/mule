/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

public class ObjectStorePartition<T extends Serializable> extends TemplateObjectStore<T> implements Disposable {

  private static final Logger LOGGER = getLogger(ObjectStorePartition.class);

  private final String partitionName;
  private final PartitionableObjectStore<T> partitionedObjectStore;

  public ObjectStorePartition(String partitionName, PartitionableObjectStore<T> partitionedObjectStore) {
    this.partitionName = partitionName;
    this.partitionedObjectStore = partitionedObjectStore;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(partitionedObjectStore, LOGGER);
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    return partitionedObjectStore.contains(key, partitionName);
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    partitionedObjectStore.store(key, value, partitionName);
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    return partitionedObjectStore.retrieve(key, partitionName);
  }

  @Override
  public void clear() throws ObjectStoreException {
    this.partitionedObjectStore.clear(this.partitionName);
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
    return partitionedObjectStore.remove(key, partitionName);
  }

  @Override
  public boolean isPersistent() {
    return partitionedObjectStore.isPersistent();
  }

  @Override
  public void open() throws ObjectStoreException {
    partitionedObjectStore.open(partitionName);
  }

  @Override
  public void close() throws ObjectStoreException {
    partitionedObjectStore.close(partitionName);
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return partitionedObjectStore.allKeys(partitionName);
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return partitionedObjectStore.retrieveAll(partitionName);
  }

  public PartitionableObjectStore<T> getBaseStore() {
    return partitionedObjectStore;
  }

  public String getPartitionName() {
    return partitionName;
  }

}
