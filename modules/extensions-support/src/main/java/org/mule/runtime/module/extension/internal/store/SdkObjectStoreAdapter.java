/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * Adapts the Mule api for object store {@link ObjectStore} into the SDK api {@link org.mule.sdk.api.store.ObjectStore}
 *
 * @since 4.5.0
 */
public class SdkObjectStoreAdapter<T extends Serializable> implements org.mule.sdk.api.store.ObjectStore<T> {

  private final ObjectStore delegate;

  SdkObjectStoreAdapter(ObjectStore<T> delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns an adapter which wrappers the Mule api {@code ObjectStore} into the SDK api
   * {@code org.mule.sdk.api.store.ObjectStore}. The adapter is only created if the value was not yet adapted nor it is a native
   * {@code org.mule.sdk.api.store.ObjectStore}, otherwise the same instance is returned.
   *
   * @param value the instance to be adapted
   * @param <T>   the object store value's generic type
   * @return a {@code org.mule.sdk.api.store.ObjectStore} adapter, if needed
   */
  public static <T extends Serializable> org.mule.sdk.api.store.ObjectStore<T> from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");
    if (value instanceof org.mule.sdk.api.store.ObjectStore) {
      return (org.mule.sdk.api.store.ObjectStore<T>) value;
    } else if (value instanceof ObjectStore) {
      return new SdkObjectStoreAdapter<>((ObjectStore) value);
    } else {
      throw new IllegalArgumentException(format("Value of class '%s' is neither a '%s' nor a '%s'",
                                                value.getClass().getName(),
                                                org.mule.sdk.api.store.ObjectStore.class.getName(),
                                                org.mule.runtime.api.store.ObjectStore.class.getName()));
    }
  }

  @Override
  public boolean contains(String key) throws ObjectStoreException {
    return delegate.contains(key);
  }

  @Override
  public void store(String key, T value) throws ObjectStoreException {
    delegate.store(key, value);
  }

  @Override
  public T retrieve(String key) throws ObjectStoreException {
    return (T) delegate.retrieve(key);
  }

  @Override
  public T remove(String key) throws ObjectStoreException {
    return (T) delegate.remove(key);
  }

  @Override
  public boolean isPersistent() {
    return delegate.isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    delegate.clear();
  }

  @Override
  public void open() throws ObjectStoreException {
    delegate.open();
  }

  @Override
  public void close() throws ObjectStoreException {
    delegate.close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return delegate.allKeys();
  }

  @Override
  public Map retrieveAll() throws ObjectStoreException {
    return delegate.retrieveAll();
  }
}
