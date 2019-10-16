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

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;

/**
 * Extends {@link PartitionedPersistentObjectStore} in order to allow using a shared path where OS data will be persisted.
 * This means also that if this is used by different MuleContext they will share the OS data.
 *
 * @param <T> the serializable entity to be persisted by OS
 *
 * @since 4.2.0, 4.1.4
 */
public class SharedPartitionedPersistentObjectStore<T extends Serializable> extends PartitionedPersistentObjectStore<T> {

  public static final String SHARED_PERSISTENT_OBJECT_STORE_KEY = "_defaultSharedPersistentObjectStore";

  private File workingDirectory;
  private Lock lock;

  /**
   * Creates a shared partitioned persistent object store.
   *
   * @param workingDirectory {@link File} where to store this OS data. Not null.
   * @param lockFactory {@link LockFactory} an external lock factory to synchronize the access to this partitioned persistent object store.
   */
  public SharedPartitionedPersistentObjectStore(File workingDirectory, LockFactory lockFactory) {
    checkArgument(workingDirectory != null, "workingDirectory cannot be null");
    this.workingDirectory = workingDirectory;
    this.lock = lockFactory.createLock(getWorkingDirectory());
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
  protected String getWorkingDirectory() {
    return workingDirectory.getAbsolutePath();
  }

}
