/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.util.Pair;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

public class PartitionedObjectStoreWrapper<T extends Serializable> extends TemplateObjectStore<T> implements Disposable {

  private static final Logger LOGGER = getLogger(PartitionedObjectStoreWrapper.class);

  private String partitionName;
  private ObjectStore<T> baseStore;

  public PartitionedObjectStoreWrapper(String name, ObjectStore<T> store) {
    partitionName = name;
    baseStore = store;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(baseStore, LOGGER);
  }

  private String partitioned(String key) {
    return partitionName + '|' + key;
  }

  private Pair<String, String> splitKey(String key) {
    int split = key.indexOf("|");
    if (split < 1) {
      throw new IllegalStateException("Invalid partitioned key " + key);
    }

    return new Pair<>(key.substring(0, split), key.substring(split + 1));
  }

  @Override
  protected void validateKey(String key) throws ObjectStoreException {
    super.validateKey(partitioned(key));
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    return getStore().contains(partitioned(key));
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    getStore().store(partitioned(key), value);
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    return getStore().retrieve(partitioned(key));
  }

  @Override
  public void clear() throws ObjectStoreException {
    for (String key : this.allKeys()) {
      remove(key);
    }
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
    return getStore().remove(partitioned(key));
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
  public List<String> allKeys() throws ObjectStoreException {
    return getStore().allKeys().stream()
        .map(this::splitKey)
        .filter(key -> key.getFirst().equals(partitionName))
        .map(Pair::getSecond)
        .collect(toList());
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    Map<String, T> all = getStore().retrieveAll();
    Map<String, T> result = new LinkedHashMap<>();

    all.forEach((k, v) -> {
      Pair<String, String> key = splitKey(k);
      if (partitionName.equals(key.getFirst())) {
        result.put(key.getSecond(), v);
      }
    });

    return result;
  }

  private ObjectStore<T> getStore() {
    return baseStore;
  }

  public ObjectStore<T> getBaseStore() {
    return getStore();
  }

}
