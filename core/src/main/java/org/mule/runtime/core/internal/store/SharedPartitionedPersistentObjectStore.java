/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.store;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.util.store.PersistentObjectStorePartition;
import org.mule.runtime.core.internal.util.store.PersistentObjectStorePartitionData;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * Extends {@link PartitionedPersistentObjectStore} in order to allow using a shared path where OS data will be persisted.
 * This means also that if this is used by different MuleContext they will share the OS data. It should not be used
 * in the context of deployable artifacts, only Tooling uses this implementation.
 *
 * @param <T> the serializable entity to be persisted by OS
 *
 * @since 4.2.0, 4.1.4
 */
public class SharedPartitionedPersistentObjectStore<T extends Serializable> extends PartitionedPersistentObjectStore<T> {

  public static final String SHARED_PERSISTENT_OBJECT_STORE_KEY = "_defaultSharedPersistentObjectStore";

  /**
   * Uses an static field to control access from different instances of this partitioned persistent object store
   * between different deploymennts, registries.
   */
  private static Map<String, PersistentObjectStorePartition> partitionsByName =
      new ConcurrentHashMap<String, PersistentObjectStorePartition>() {

        @Override
        public PersistentObjectStorePartition put(String key, PersistentObjectStorePartition value) {
          // Creates an instance of the information to avoid referencing to the muleContext as the same persistentObjectStorePartition
          // is used by different muleContexts
          return super.put(key, new PersistentObjectStorePartitionData(value.getPartitionName(), value.getPartitionDirectory()));
        }

        @Override
        public PersistentObjectStorePartition putIfAbsent(String key, PersistentObjectStorePartition value) {
          // Creates an instance of the information to avoid referencing to the muleContext as the same persistentObjectStorePartition
          // is used by different muleContexts
          return super.putIfAbsent(key, new PersistentObjectStorePartitionData(value.getPartitionName(),
                                                                               value.getPartitionDirectory()));
        }
      };

  private File workingDirectory;
  private Lock lock;

  /**
   * Creates a shared partitioned persistent object store.
   *
   * @param workingDirectory {@link File} where to store this OS data. Not null.
   * @param lockFactory {@link LockFactory} an external lock factory to synchronize the access to this partitioned persistent object store.
   */
  public SharedPartitionedPersistentObjectStore(File workingDirectory, LockFactory lockFactory) {
    super(partitionsByName);
    checkArgument(workingDirectory != null, "workingDirectory cannot be null");
    this.workingDirectory = workingDirectory;
    this.lock = lockFactory.createLock(getWorkingDirectory());
  }

  @Override
  protected PersistentObjectStorePartition<T> getPartitionObjectStore(String partitionName) throws ObjectStoreException {
    PersistentObjectStorePartition<T> partitionObjectStore = super.getPartitionObjectStore(partitionName);
    // Create a new PersistentObjectStorePartition that references to the current muleContext to deserialize an entry that was added
    // by another muleContext (serialization)
    return new PersistentObjectStorePartition<>(muleContext, partitionName, partitionObjectStore.getPartitionDirectory());
  }

  @Override
  public void open() throws ObjectStoreException {
    lock.lock();
    try {
      super.open();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void open(String partitionName) throws ObjectStoreException {
    lock.lock();
    try {
      super.open(partitionName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected T doRemove(String key, String partitionName) throws ObjectStoreException {
    lock.lock();
    try {
      return super.doRemove(key, partitionName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void expire(long entryTTL, int maxEntries) throws ObjectStoreException {
    lock.lock();
    try {
      super.expire(entryTTL, maxEntries);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void expire(long entryTTL, int maxEntries, String partitionName) throws ObjectStoreException {
    lock.lock();
    try {
      super.expire(entryTTL, maxEntries, partitionName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected String getWorkingDirectory() {
    return workingDirectory.getAbsolutePath();
  }

}
