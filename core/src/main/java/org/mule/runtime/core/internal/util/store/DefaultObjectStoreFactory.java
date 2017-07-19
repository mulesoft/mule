/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;

/**
 * Create the default object stores
 */
public interface DefaultObjectStoreFactory {

  /**
   * Creates an in memory object store for mule components
   *
   * @return in memory object store
   */
  ObjectStore<Serializable> createDefaultInMemoryObjectStore();

  /**
   * Creates a persistent object store for mule components
   *
   * @return persistent object store
   */
  ObjectStore<Serializable> createDefaultPersistentObjectStore();
}
