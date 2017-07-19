/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;

/**
 * Simple implementation of {@link ObjectStoreToMapAdapter} which operates on a
 * {@link ObjectStore} received in the constructor
 *
 * @param <T> the generic type of the instances contained in the {@link ObjectStore}
 * @since 4.0
 */
public class SimpleObjectStoreToMapAdapter<T extends Serializable> extends ObjectStoreToMapAdapter<T> {

  private final ObjectStore<T> objectStore;

  /**
   * Creates a new instance
   *
   * @param objectStore the {@link ObjectStore} to be bridged
   */
  public SimpleObjectStoreToMapAdapter(ObjectStore<T> objectStore) {
    this.objectStore = objectStore;
  }

  @Override
  public ObjectStore<T> getObjectStore() {
    return objectStore;
  }
}
