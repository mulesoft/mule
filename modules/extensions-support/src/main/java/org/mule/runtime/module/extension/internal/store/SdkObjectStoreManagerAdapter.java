/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.sdk.api.store.ObjectStore;
import org.mule.sdk.api.store.ObjectStoreSettings;

import java.io.Serializable;

import jakarta.inject.Inject;

/**
 * Adapts the Mule api for object store manager {@link ObjectStoreManager} into the SDK api
 * {@link org.mule.sdk.api.store.ObjectStoreManager}
 *
 * @since 4.5.0
 */
public class SdkObjectStoreManagerAdapter implements org.mule.sdk.api.store.ObjectStoreManager {

  private ObjectStoreManager delegate;

  @Inject
  public void setDelegate(ObjectStoreManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name) {
    return (T) SdkObjectStoreAdapter.from(delegate.getObjectStore(name));
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name,
                                                                             ObjectStoreSettings objectStoreSettings) {
    return (T) SdkObjectStoreAdapter.from(
                                          delegate.createObjectStore(name,
                                                                     SdkObjectStoreUtils
                                                                         .convertToMuleObjectStoreSettings(objectStoreSettings)));
  }

  @Override
  public <T extends ObjectStore<? extends Serializable>> T getOrCreateObjectStore(String name,
                                                                                  ObjectStoreSettings objectStoreSettings) {
    return (T) SdkObjectStoreAdapter.from(
                                          delegate
                                              .getOrCreateObjectStore(name,
                                                                      SdkObjectStoreUtils
                                                                          .convertToMuleObjectStoreSettings(objectStoreSettings)));
  }

  @Override
  public void disposeStore(String name) throws ObjectStoreException {
    delegate.disposeStore(name);
  }
}

