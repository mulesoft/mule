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
