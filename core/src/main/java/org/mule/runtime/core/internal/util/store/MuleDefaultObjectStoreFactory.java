/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
