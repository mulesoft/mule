/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.TemplateObjectStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Will wrap a provided object store or a newly created one with the provided factory, with the provided having precedence if
 * present.
 * <p/>
 * In the case the factory is used and a fresh object store is created, its lifecycle management will be delegated by this
 * wrapper.
 */
public class ProvidedObjectStoreWrapper<T extends Serializable> extends TemplateObjectStore<T> implements Disposable {

  private ObjectStore<T> wrapped;
  private final boolean provided;

  /**
   * Wraps the {@code providedObjectStore} if given, or uses the {@code objectStoreSupplier} to create one.
   * 
   * @param providedObjectStore the objectStroe provided through config to use. May be null.
   * @param objectStoreSupplier provides the object store to use if {@code providedObjectStore} is null.
   */
  public ProvidedObjectStoreWrapper(ObjectStore<T> providedObjectStore, Supplier<ObjectStore> objectStoreSupplier) {
    if (providedObjectStore == null) {
      provided = false;
      wrapped = objectStoreSupplier.get();
    } else {
      provided = true;
      wrapped = providedObjectStore;
    }
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    return getWrapped().contains(key);
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    getWrapped().store(key, value);
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    return getWrapped().retrieve(key);
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
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

  @Override
  public void open() throws ObjectStoreException {
    getWrapped().open();
  }

  @Override
  public void close() throws ObjectStoreException {
    getWrapped().close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return getWrapped().allKeys();
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return getWrapped().retrieveAll();
  }

  protected ObjectStore<T> getWrapped() {
    return wrapped;
  }
}
