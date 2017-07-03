/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import java.io.Serializable;

/**
 * Simple implementation of {@link ObjectStoreToMapAdapter} which operates on a
 * {@link ListableObjectStore} received in the constructor
 *
 * @param <T> the generic type of the instances contained in the {@link ListableObjectStore}
 * @since 4.0
 */
public class SimpleObjectStoreToMapAdapter<T extends Serializable> extends ObjectStoreToMapAdapter<T> {

  private final ListableObjectStore<T> objectStore;

  /**
   * Creates a new instance
   *
   * @param objectStore the {@link ListableObjectStore} to be bridged
   */
  public SimpleObjectStoreToMapAdapter(ListableObjectStore<T> objectStore) {
    this.objectStore = objectStore;
  }

  @Override
  public ListableObjectStore<T> getObjectStore() {
    return objectStore;
  }
}
