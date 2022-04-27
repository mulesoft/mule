/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal.connection;

import static org.mule.test.customos.internal.MyOSConnector.STORAGE;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.sdk.api.store.ObjectStore;
import org.mule.sdk.api.store.ObjectStoreException;
import org.mule.sdk.api.store.ObjectStoreManager;
import org.mule.sdk.api.store.ObjectStoreSettings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom in-memory implementations of Object Store Manager and Object Store using SDK API
 */
public class CustomSdkObjectStoreManager implements ObjectStoreManager {

  private ObjectStore<TypedValue<String>> objectStore = new CustomSdkObjectStoreManager.MapObjectStore();

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name) {
    return (T) objectStore;
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name, ObjectStoreSettings settings) {
    return (T) objectStore;
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getOrCreateObjectStore(String name, ObjectStoreSettings settings) {
    return (T) objectStore;
  }

  @Override
  public void disposeStore(String name) throws ObjectStoreException {}

  private class MapObjectStore implements ObjectStore<TypedValue<String>> {

    @Override
    public boolean contains(String key) throws ObjectStoreException {
      return STORAGE.containsKey(key);
    }

    @Override
    public void store(String key, TypedValue<String> value) throws ObjectStoreException {
      STORAGE.put(key, value);
    }

    @Override
    public TypedValue<String> retrieve(String key) throws ObjectStoreException {
      return STORAGE.get(key);
    }

    @Override
    public TypedValue<String> remove(String key) throws ObjectStoreException {
      return STORAGE.remove(key);
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void clear() throws ObjectStoreException {
      STORAGE.clear();
    }

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return new ArrayList<>(STORAGE.keySet());
    }

    @Override
    public Map<String, TypedValue<String>> retrieveAll() throws ObjectStoreException {
      return STORAGE;
    }
  }

}
