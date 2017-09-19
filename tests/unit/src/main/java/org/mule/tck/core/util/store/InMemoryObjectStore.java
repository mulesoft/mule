/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.util.store;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.util.store.AbstractMonitoredObjectStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <code>InMemoryObjectStore</code> implements an optionally bounded in-memory store for message IDs with periodic expiry of old
 * entries. The bounded size is a <i>soft</i> limit and only enforced periodically by the expiry process; this means that the
 * store may temporarily exceed its maximum size between expiry runs, but will eventually shrink to its configured size.
 */
public class InMemoryObjectStore<T extends Serializable> extends AbstractMonitoredObjectStore<T> {

  protected final ConcurrentSkipListMap<Long, StoredObject<T>> store;

  public InMemoryObjectStore() {
    this.store = new ConcurrentSkipListMap<>();
  }

  @Override
  public boolean isPersistent() {
    return false;
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    synchronized (store) {
      return store.values().contains(new StoredObject<T>(key, null));
    }
  }

  @Override
  protected void doStore(String id, T value) throws ObjectStoreException {
    if (id == null) {
      throw new ObjectStoreException(CoreMessages.objectIsNull("id"));
    }

    // this block is unfortunately necessary to counter a possible race condition
    // between multiple nonatomic calls to containsObject/storeObject
    StoredObject<T> obj = new StoredObject<>(id, value);
    synchronized (store) {
      if (store.values().contains(obj)) {
        throw new ObjectAlreadyExistsException();
      }

      boolean written = false;
      while (!written) {
        Long key = Long.valueOf(System.nanoTime());
        written = (store.put(key, obj) == null);
      }
    }
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    synchronized (store) {
      Map.Entry<?, ?> entry = findEntry(key);
      if (entry != null) {
        StoredObject<T> object = (StoredObject<T>) entry.getValue();
        return object.getItem();
      }
    }

    throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
  }

  @SuppressWarnings("unchecked")
  private Map.Entry<?, ?> findEntry(Serializable key) {
    for (Object o : store.entrySet()) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;

      StoredObject<T> object = (StoredObject<T>) entry.getValue();
      if (object.getId().equals(key)) {
        return entry;
      }
    }
    return null;
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
    synchronized (store) {
      Map.Entry<?, ?> entry = findEntry(key);
      if (entry != null) {
        StoredObject<T> removedObject = store.remove(entry.getKey());
        return removedObject.getItem();
      }
    }

    throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
  }

  @Override
  public void clear() throws ObjectStoreException {
    synchronized (store) {
      store.clear();
    }
  }

  @Override
  public void open() throws ObjectStoreException {

  }

  @Override
  public void close() throws ObjectStoreException {

  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return store.keySet().stream().map(String::valueOf).collect(toList());
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return store.values().stream().collect(toMap(StoredObject::getId, StoredObject::getItem));
  }

  private int expireAndCount() {
    // first we trim the store according to max size
    int expiredEntries = 0;

    final long now = System.nanoTime();

    Map.Entry<?, ?> oldestEntry;

    purge: while ((oldestEntry = store.firstEntry()) != null) {
      Long oldestKey = (Long) oldestEntry.getKey();
      long oldestKeyValue = oldestKey.longValue();

      if (NANOSECONDS.toMillis(now - oldestKeyValue) >= entryTTL) {
        store.remove(oldestKey);
        expiredEntries++;
      } else {
        break purge;
      }
    }

    return expiredEntries;
  }

  private boolean isTrimNeeded(int currentSize) {
    return currentSize > maxEntries;
  }

  private boolean isExpirationNeeded() {
    // this is not guaranteed to be precise, but we don't mind
    int currentSize = store.size();

    // should expire further if entry TTLs are enabled
    return entryTTL > 0 && currentSize != 0;
  }

  private int doTrimAndExpire() {
    int expiredEntries = 0;

    if (isTrimNeeded(store.size())) {
      expiredEntries += trimToMaxSize(store.size());
    }

    if (isExpirationNeeded()) {
      expiredEntries = trimToMaxSize(store.size());
      expiredEntries += this.expireAndCount();
    }
    return expiredEntries;
  }

  @Override
  public void expire() {
    int expiredEntries = doTrimAndExpire();

    if (logger.isDebugEnabled()) {
      logger.debug("Expired " + expiredEntries + " old entries");
    }
  }

  private int trimToMaxSize(int currentSize) {
    if (maxEntries < 0) {
      return currentSize;
    }

    int excess = (currentSize - maxEntries);
    if (excess > 0) {
      while (currentSize > maxEntries) {
        store.pollFirstEntry();
        currentSize--;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Expired " + excess + " excess entries");
      }
    }
    return excess;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + store;
  }

  /**
   * Represents the object stored in the store. This class holds the Object itslef and its ID.
   */
  protected static class StoredObject<T> {

    private String id;
    private T item;

    StoredObject(String id, T item) {
      this.id = id;
      this.item = item;
    }

    public String getId() {
      return id;
    }

    public T getItem() {
      return item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      StoredObject<T> that = (StoredObject<T>) o;

      return id.equals(that.id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("StoredObject");
      sb.append("{id='").append(id).append('\'');
      sb.append(", item=").append(item);
      sb.append('}');
      return sb.toString();
    }
  }
}
