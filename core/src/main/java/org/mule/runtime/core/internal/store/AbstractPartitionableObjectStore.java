/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.store;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.store.AbstractObjectStoreSupport;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class AbstractPartitionableObjectStore<T extends Serializable> extends AbstractObjectStoreSupport<T>
    implements PartitionableObjectStore<T> {

  @Override
  public void open() throws ObjectStoreException {
    open(DEFAULT_PARTITION_NAME);
  }

  @Override
  public void close() throws ObjectStoreException {
    close(DEFAULT_PARTITION_NAME);
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return allKeys(DEFAULT_PARTITION_NAME);
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return retrieveAll(DEFAULT_PARTITION_NAME);
  }

  @Override
  public boolean contains(String key) throws ObjectStoreException {
    return contains(key, DEFAULT_PARTITION_NAME);
  }

  @Override
  public void store(String key, T value) throws ObjectStoreException {
    store(key, value, DEFAULT_PARTITION_NAME);
  }

  @Override
  public T retrieve(String key) throws ObjectStoreException {
    return retrieve(key, DEFAULT_PARTITION_NAME);
  }

  @Override
  public T remove(String key) throws ObjectStoreException {
    return remove(key, DEFAULT_PARTITION_NAME);
  }

  @Override
  public void clear() throws ObjectStoreException {
    clear(DEFAULT_PARTITION_NAME);
  }

  @Override
  public boolean contains(String key, String partitionName) throws ObjectStoreException {
    validateKeyAndPartitionName(key, partitionName);
    return doContains(key, partitionName);
  }

  protected abstract boolean doContains(String key, String partitionName) throws ObjectStoreException;

  @Override
  public void store(String key, T value, String partitionName) throws ObjectStoreException {
    validateKeyAndPartitionName(key, partitionName);
    doStore(key, value, partitionName);
  }

  protected abstract void doStore(String key, T value, String partitionName) throws ObjectStoreException;

  @Override
  public T retrieve(String key, String partitionName) throws ObjectStoreException {
    validatePresentKeyInPartition(key, partitionName);
    return doRetrieve(key, partitionName);
  }

  protected abstract T doRetrieve(String key, String partitionName) throws ObjectStoreException;

  @Override
  public T remove(String key, String partitionName) throws ObjectStoreException {
    validatePresentKeyInPartition(key, partitionName);
    return doRemove(key, partitionName);
  }

  protected abstract T doRemove(String key, String partitionName) throws ObjectStoreException;

  protected void validateKeyAndPartitionName(String key, String partitionName) throws ObjectStoreException {
    validateKey(key);
    if (partitionName == null || partitionName.trim().length() == 0) {
      throw new ObjectStoreException(createStaticMessage("partition name cannot be null or blank"));
    }
  }

  protected void validatePresentKeyInPartition(String key, String partitionName) throws ObjectStoreException {
    validateKeyAndPartitionName(key, partitionName);

    if (!contains(key, partitionName)) {
      throw new ObjectDoesNotExistException(createStaticMessage(
                                                                format("Key '%s' does not exist in partition '%s'", key,
                                                                       partitionName)));
    }
  }
}
