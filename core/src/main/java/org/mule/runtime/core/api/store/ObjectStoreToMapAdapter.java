/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts the object store interface to a map interface so the client doesn't have to deal with all the ObjectStoreExceptions
 * thrown by ObjectStore.
 * <p>
 * This class provides limited functionality from the Map interface. It does not support some methods (see methods javadoc) that
 * can have a big impact in performance due the underlying object store being used.
 * <p>
 * The object store provided will be access for completing the map operations but the whole lifecycle of the provided object store
 * must be handled by the user.
 * <p>
 * Operations of this map are not thread safe so the user must synchronize access to this map properly.
 *
 * @param <T> the generic type of the instances contained in the {@link ListableObjectStore}
 */
public abstract class ObjectStoreToMapAdapter<T extends Serializable> implements Map<Serializable, T> {

  protected abstract ListableObjectStore<T> getObjectStore();

  @Override
  public int size() {
    try {
      return getObjectStore().allKeys().size();
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean isEmpty() {
    try {
      return getObjectStore().allKeys().isEmpty();
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    try {
      return getObjectStore().contains((Serializable) key);
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException("Map adapter for object store does not support contains value");
  }

  @Override
  public T get(Object key) {
    try {
      if (!getObjectStore().contains((Serializable) key)) {
        return null;
      }
      return getObjectStore().retrieve((Serializable) key);
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public T put(Serializable key, T value) {
    T previousValue = null;
    try {
      if (getObjectStore().contains(key)) {
        previousValue = getObjectStore().retrieve(key);
        getObjectStore().remove(key);
      }
      if (value != null) {
        getObjectStore().store(key, value);
      }
      return previousValue;
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public T remove(Object key) {
    try {
      if (getObjectStore().contains((Serializable) key)) {
        return getObjectStore().remove((Serializable) key);
      }
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends Serializable, ? extends T> mapToAdd) {
    for (Serializable key : mapToAdd.keySet()) {
      put(key, mapToAdd.get(key));
    }
  }

  @Override
  public void clear() {
    try {
      getObjectStore().clear();
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Set<Serializable> keySet() {
    try {
      final List<Serializable> allKeys = getObjectStore().allKeys();
      return new HashSet(allKeys);
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * This method is not supported for performance reasons
   */
  @Override
  public Collection<T> values() {
    throw new UnsupportedOperationException("ObjectStoreToMapAdapter does not support values() method");
  }

  /**
   * This method is not supported for performance reasons
   */
  @Override
  public Set<Entry<Serializable, T>> entrySet() {
    throw new UnsupportedOperationException("ObjectStoreToMapAdapter does not support entrySet() method");
  }
}
