/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.internal.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;

import java.io.Serializable;

public class MuleDefaultObjectStoreFactory implements DefaultObjectStoreFactory {

  @Override
  public ObjectStore<Serializable> createDefaultInMemoryObjectStore() {
    return new PartitionedInMemoryObjectStore<>();
  }

  @Override
  public ObjectStore<Serializable> createDefaultPersistentObjectStore() {
    return new PartitionedPersistentObjectStore<>();
  }
}
