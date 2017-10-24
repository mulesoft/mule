/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.store;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.core.internal.util.store.MuleObjectStoreManager.UNBOUNDED;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.PartitionableExpirableObjectStore;
import org.mule.runtime.core.api.component.InternalComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartitionedInMemoryObjectStore<T extends Serializable> extends AbstractPartitionableObjectStore<T>
    implements PartitionableExpirableObjectStore<T>, InternalComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(PartitionedInMemoryObjectStore.class);

  private ConcurrentMap<String, ConcurrentMap<String, T>> partitions = new ConcurrentHashMap<>();
  private ConcurrentMap<String, ConcurrentLinkedQueue<ExpiryEntry>> expiryInfoPartition = new ConcurrentHashMap<>();

  @Override
  public boolean isPersistent() {
    return false;
  }

  @Override
  protected boolean doContains(String key, String partitionName) throws ObjectStoreException {
    if (partitions.containsKey(partitionName)) {
      return partitions.get(partitionName).containsKey(key);
    } else {
      return false;
    }
  }

  @Override
  protected void doStore(String key, T value, String partitionName) throws ObjectStoreException {
    T oldValue = getPartition(partitionName).putIfAbsent(key, value);
    if (oldValue != null) {
      throw new ObjectAlreadyExistsException();
    }
    getExpiryInfoPartition(partitionName).add(new ExpiryEntry(getCurrentNanoTime(), key));
  }

  @Override
  protected T doRetrieve(String key, String partitionName) throws ObjectStoreException {
    T value = getPartition(partitionName).get(key);
    if (value == null) {
      throw new ObjectDoesNotExistException();
    }
    return value;
  }

  @Override
  protected T doRemove(String key, String partitionName) throws ObjectStoreException {
    T removedValue = getPartition(partitionName).remove(key);
    if (removedValue == null) {
      throw new ObjectDoesNotExistException();
    }

    Iterator<ExpiryEntry> iterator = getExpiryInfoPartition(partitionName).iterator();
    while (iterator.hasNext()) {
      ExpiryEntry entry = iterator.next();
      if (key.equals(entry.getKey())) {
        iterator.remove();
        break;
      }
    }

    return removedValue;
  }

  @Override
  public List<String> allKeys(String partitionName) throws ObjectStoreException {
    return new ArrayList<>(getPartition(partitionName).keySet());
  }

  @Override
  public Map<String, T> retrieveAll(String partitionName) throws ObjectStoreException {
    return new LinkedHashMap<>(getPartition(partitionName));
  }

  @Override
  public void clear(String partitionName) throws ObjectStoreException {
    getPartition(partitionName).clear();
  }

  @Override
  public List<String> allPartitions() throws ObjectStoreException {
    return new ArrayList<>(partitions.keySet());
  }

  private ConcurrentMap<String, T> getPartition(String partitionName) {
    ConcurrentMap<String, T> partition = partitions.get(partitionName);
    if (partition == null) {
      partition = new ConcurrentHashMap<>();
      ConcurrentMap<String, T> previous = partitions.putIfAbsent(partitionName, partition);
      if (previous != null) {
        partition = previous;
      }
    }
    return partition;
  }

  private ConcurrentLinkedQueue<ExpiryEntry> getExpiryInfoPartition(String partitionName) {
    ConcurrentLinkedQueue<ExpiryEntry> partition = expiryInfoPartition.get(partitionName);
    if (partition == null) {
      partition = new ConcurrentLinkedQueue<>();
      ConcurrentLinkedQueue<ExpiryEntry> previous = expiryInfoPartition.putIfAbsent(partitionName, partition);
      if (previous != null) {
        partition = previous;
      }
    }
    return partition;
  }

  @Override
  public void open(String partitionName) throws ObjectStoreException {
    // Nothing to do
  }

  @Override
  public void close(String partitionName) throws ObjectStoreException {
    // Nothing to do
  }

  @Override
  public void expire(long entryTTL, int maxEntries) throws ObjectStoreException {
    expire(entryTTL, maxEntries, DEFAULT_PARTITION_NAME);
  }

  @Override
  public void expire(long entryTTL, int maxEntries, String partitionName) throws ObjectStoreException {
    final long now = getCurrentNanoTime();
    int expiredEntries = 0;
    ExpiryEntry oldestEntry;
    ConcurrentLinkedQueue<ExpiryEntry> store = getExpiryInfoPartition(partitionName);
    ConcurrentMap<String, T> partition = getPartition(partitionName);

    trimToMaxSize(store, maxEntries, partition);

    if (entryTTL == UNBOUNDED) {
      return;
    }

    while ((oldestEntry = store.peek()) != null) {
      if (NANOSECONDS.toMillis(now - oldestEntry.getTime()) >= entryTTL) {
        oldestEntry = store.remove();
        partition.remove(oldestEntry.getKey());
        expiredEntries++;
      } else {
        break;
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Expired " + expiredEntries + " old entries");
    }
  }

  private void trimToMaxSize(ConcurrentLinkedQueue<ExpiryEntry> store, int maxEntries, ConcurrentMap<String, T> partition) {
    if (maxEntries == UNBOUNDED) {
      return;
    }

    int currentSize = store.size();
    int excess = (currentSize - maxEntries);
    if (excess > 0) {
      while (currentSize > maxEntries) {
        ExpiryEntry toRemove = store.remove();
        partition.remove(toRemove.getKey());
        currentSize--;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Expired " + excess + " excess entries");
      }
    }
  }

  @Override
  public void disposePartition(String partitionName) throws ObjectStoreException {
    Map partition = partitions.remove(partitionName);
    if (partition != null) {
      partition.clear();
    }

    ConcurrentLinkedQueue<ExpiryEntry> entries = expiryInfoPartition.remove(partitionName);
    if (entries != null) {
      entries.clear();
    }
  }

  protected long getCurrentNanoTime() {
    return System.nanoTime();
  }

  private static class ExpiryEntry {

    private final long time;
    private final Serializable key;

    public ExpiryEntry(long time, Serializable key) {
      this.time = time;
      this.key = key;
    }

    public long getTime() {
      return time;
    }

    public Serializable getKey() {
      return key;
    }
  }
}
