/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * An implementation that only references the partitionName and partitionDirectory so it can be shared between different
 * muleContext and uses by {@link org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore}.
 * Only Tooling should use this in order to share object store state between different deployments.
 *
 * @param <T>
 */
public class PersistentObjectStorePartitionData<T extends Serializable> extends PersistentObjectStorePartition {

  private File partitionDirectory;
  private String partitionName;

  public PersistentObjectStorePartitionData(String partitionName, File partitionDirectory) {
    this.partitionDirectory = partitionDirectory;
    this.partitionName = partitionName;
  }

  @Override
  public synchronized void open() throws ObjectStoreException {}

  @Override
  public void close() throws ObjectStoreException {}

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected void doStore(String key, Serializable value) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  public void clear() throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected Serializable doRetrieve(String key) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  public Map retrieveAll() throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected Serializable doRemove(String key) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  public boolean isPersistent() {
    return true;
  }

  @Override
  public void expire(long entryTTL, int maxEntries) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  public File getPartitionDirectory() {
    return partitionDirectory;
  }

  @Override
  protected void createDirectory(File directory) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected File createFileToStoreObject() throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected File createOrRetrievePartitionDescriptorFile() throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected void serialize(File outputFile, StoreValue storeValue) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected StoreValue deserialize(File file) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  protected void deleteStoreFile(File file) throws ObjectStoreException {
    throw new ObjectStoreNotAvailableException();
  }

  @Override
  public String getPartitionName() {
    return partitionName;
  }
}
