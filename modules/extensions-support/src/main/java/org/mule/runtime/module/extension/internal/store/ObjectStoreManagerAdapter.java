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
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;

import java.io.Serializable;

/**
 * Adapts the SDK API for object store manager {@link org.mule.sdk.api.store.ObjectStoreManager} into the Mule api
 * {@link ObjectStoreManager}
 *
 * @since 4.5.0
 */
public class ObjectStoreManagerAdapter implements ObjectStoreManager {

  private final org.mule.sdk.api.store.ObjectStoreManager delegate;

  ObjectStoreManagerAdapter(org.mule.sdk.api.store.ObjectStoreManager delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns an adapter which wrappers the SDK api {@code org.mule.sdk.api.store.ObjectStoreManager} into the Mule api
   * {@code ObjectStoreManager}. The adapter is only created if the value was not yet adapted nor it is a native
   * {@code ObjectStoreManager}, otherwise the same instance is returned.
   *
   * @param value the instance to be adapted
   * @return a {@code ObjectStore} adapter, if needed
   */
  public static ObjectStoreManager from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");
    if (value instanceof ObjectStoreManager) {
      return (ObjectStoreManager) value;
    } else if (value instanceof org.mule.sdk.api.store.ObjectStoreManager) {
      return new ObjectStoreManagerAdapter((org.mule.sdk.api.store.ObjectStoreManager) value);
    } else {
      throw new IllegalArgumentException(format("Value of class '%s' is neither a '%s' nor a '%s'",
                                                value.getClass().getName(),
                                                ObjectStoreManager.class.getName(),
                                                org.mule.sdk.api.store.ObjectStoreManager.class.getName()));
    }
  }

  public org.mule.sdk.api.store.ObjectStoreManager getDelegate() {
    return delegate;
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name) {
    return (T) ObjectStoreAdapter.from(delegate.getObjectStore(name));
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name,
                                                                             ObjectStoreSettings objectStoreSettings) {
    return (T) ObjectStoreAdapter.from(delegate.createObjectStore(name, createSDKObjectStoreSettings(objectStoreSettings)));
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getOrCreateObjectStore(String name,
                                                                                  ObjectStoreSettings objectStoreSettings) {
    return (T) ObjectStoreAdapter.from(delegate.getOrCreateObjectStore(name, createSDKObjectStoreSettings(objectStoreSettings)));
  }

  @Override
  public void disposeStore(String name) throws ObjectStoreException {
    try {
      delegate.disposeStore(name);
    } catch (org.mule.sdk.api.store.ObjectStoreException e) {
      throw createObjectStoreException(e);
    }
  }

  private ObjectStoreException createObjectStoreException(org.mule.sdk.api.store.ObjectStoreException exception) {
    // TODO create Mule object store exception from SDK api OS exception
    return new ObjectStoreException(exception);
  }

  private org.mule.sdk.api.store.ObjectStoreSettings createSDKObjectStoreSettings(ObjectStoreSettings objectStoreSettings) {
    // TODO create SDK object store settings from mule api OS settings
    return org.mule.sdk.api.store.ObjectStoreSettings.builder().build();
  }
}
