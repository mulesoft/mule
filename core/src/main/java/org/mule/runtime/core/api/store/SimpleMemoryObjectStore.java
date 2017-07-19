/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMemoryObjectStore<T extends Serializable> extends TemplateObjectStore<T> implements ObjectStore<T> {

  private Map<String, T> map = new ConcurrentHashMap<>();

  @Override
  public boolean isPersistent() {
    return false;
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    return map.containsKey(key);
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    if (value == null) {
      throw new ObjectStoreException(objectIsNull("value"));
    }

    map.put(key, value);
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    return map.get(key);
  }

  @Override
  public void clear() throws ObjectStoreException {
    this.map.clear();
  }

  @Override
  protected T doRemove(String key) {
    return map.remove(key);
  }

  @Override
  public void open() throws ObjectStoreException {
    // this is a no-op
  }

  @Override
  public void close() throws ObjectStoreException {
    // this is a no-op
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return new ArrayList<>(map.keySet());
  }
}
