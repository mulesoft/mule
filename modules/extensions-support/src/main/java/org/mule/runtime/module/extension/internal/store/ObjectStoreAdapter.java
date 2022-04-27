/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Adapts the SDK API for object store {@link org.mule.sdk.api.store.ObjectStore} into the Mule api {@link ObjectStore}
 *
 * @since 4.5.0
 */
public class ObjectStoreAdapter<T extends Serializable> implements ObjectStore<T> {

  private final org.mule.sdk.api.store.ObjectStore<T> delegate;

  ObjectStoreAdapter(org.mule.sdk.api.store.ObjectStore<T> delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns an adapter which wrappers the SDK api {@code org.mule.sdk.api.store.ObjectStore} into the Mule api
   * {@code ObjectStore}. The adapter is only created if the value was not yet adapted nor it is a native {@code ObjectStore},
   * otherwise the same instance is returned.
   *
   * @param value the instance to be adapted
   * @param <T>   the object store value's generic type
   * @return a {@code ObjectStore} adapter, if needed
   */
  public static <T extends Serializable> ObjectStore<T> from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");
    if (value instanceof ObjectStore) {
      return (ObjectStore<T>) value;
    } else if (value instanceof org.mule.sdk.api.store.ObjectStore) {
      return new ObjectStoreAdapter((org.mule.sdk.api.store.ObjectStore) value);
    } else {
      throw new IllegalArgumentException(format("Value of class '%s' is neither a '%s' nor a '%s'",
                                                value.getClass().getName(),
                                                ObjectStore.class.getName(),
                                                org.mule.sdk.api.store.ObjectStore.class.getName()));
    }
  }

  @Override
  public boolean contains(String key) throws ObjectStoreException {
    try {
      return delegate.contains(key);
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public void store(String key, T value) throws ObjectStoreException {
    try {
      delegate.store(key, value);
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public T retrieve(String key) throws ObjectStoreException {
    try {
      return delegate.retrieve(key);
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public T remove(String key) throws ObjectStoreException {
    try {
      return delegate.remove(key);
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public boolean isPersistent() {
    return delegate.isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    try {
      delegate.clear();
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public void open() throws ObjectStoreException {
    try {
      delegate.open();
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public void close() throws ObjectStoreException {
    try {
      delegate.close();
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    try {
      return delegate.allKeys();
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    try {
      return delegate.retrieveAll();
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  private ObjectStoreException createObjectStoreException(org.mule.sdk.api.store.ObjectStoreException exception) {
    // TODO create Mule object store exception from SDK api OS exception
    return new ObjectStoreException(exception);
  }
}
