/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
