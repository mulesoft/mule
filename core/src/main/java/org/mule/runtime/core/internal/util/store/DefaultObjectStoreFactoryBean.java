/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;

/**
 * Manage the creation of the default Mule object stores.
 */
public class DefaultObjectStoreFactoryBean {

  private static DefaultObjectStoreFactory delegate = new MuleDefaultObjectStoreFactory();

  /**
   * Do not instantiate
   */
  private DefaultObjectStoreFactoryBean() {}

  public static ObjectStore<Serializable> createDefaultInMemoryObjectStore() {
    return delegate.createDefaultInMemoryObjectStore();
  }

  public static ObjectStore<Serializable> createDefaultPersistentObjectStore() {
    return delegate.createDefaultPersistentObjectStore();
  }
}
