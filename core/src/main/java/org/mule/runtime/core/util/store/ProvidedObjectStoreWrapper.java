/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;

import java.io.Serializable;

import org.apache.commons.collections.Factory;

/**
 * Will wrap a provided object store or a newly created one with the provided factory, with the provided having precedence if
 * present.
 * <p/>
 * In the case the factory is used and a fresh object store is created, its lifecycle management will be delegated by this
 * wrapper.
 */
public class ProvidedObjectStoreWrapper<T extends Serializable> implements ObjectStore<T>, Disposable {

  private ObjectStore<T> wrapped;
  private final boolean provided;

  /**
   * Wraps the {@code providedObjectStore} if given, or uses the {@code objectStoreFactory} to create one.
   * 
   * @param providedObjectStore the objectStroe provided through config to use. May be null.
   * @param objectStoreFactory the factory to use to build an object store if {@code providedObjectStore} is null.
   */
  public ProvidedObjectStoreWrapper(ObjectStore<T> providedObjectStore, Factory objectStoreFactory) {
    if (providedObjectStore == null) {
      provided = false;
      wrapped = (ObjectStore<T>) objectStoreFactory.create();
    } else {
      provided = true;
      wrapped = providedObjectStore;
    }
  }

  @Override
  public boolean contains(Serializable key) throws ObjectStoreException {
    return getWrapped().contains(key);
  }

  @Override
  public void store(Serializable key, T value) throws ObjectStoreException {
    getWrapped().store(key, value);
  }

  @Override
  public T retrieve(Serializable key) throws ObjectStoreException {
    return getWrapped().retrieve(key);
  }

  @Override
  public T remove(Serializable key) throws ObjectStoreException {
    return getWrapped().remove(key);
  }

  @Override
  public boolean isPersistent() {
    return getWrapped().isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    getWrapped().clear();
  }

  @Override
  public void dispose() {
    if (!provided && wrapped != null && wrapped instanceof Disposable) {
      ((Disposable) wrapped).dispose();
    }
    wrapped = null;
  }

  protected ObjectStore<T> getWrapped() {
    return wrapped;
  }
}
