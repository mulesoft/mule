/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry.map;

import org.mule.runtime.api.lifecycle.Disposable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;

/**
 * This class encapsulates the {@link HashMap} that's used for storing the objects in the transient registry and also shields
 * client code from having to deal with locking the {@link ReadWriteLock} for the exposed Map operations.
 */
public class RegistryMap {

  private final Map<String, Object> registry = new HashMap<>();
  private final ReadWriteLock registryLock = new ReentrantReadWriteLock();
  private final Set<Object> lostObjects = new TreeSet<>(new Comparator<Object>() {

    @Override
    public int compare(Object o1, Object o2) {
      return o1 == o2 ? 0 : nvl(o1) - nvl(o2);
    }

    private int nvl(Object o) {
      return o != null ? o.hashCode() : 0;
    }
  });

  private Logger logger;

  public RegistryMap(Logger log) {
    super();
    logger = log;
  }

  public Collection<?> select(Predicate predicate) {
    Lock readLock = registryLock.readLock();
    try {
      readLock.lock();
      return org.apache.commons.collections.CollectionUtils.select(registry.values(), predicate);
    } finally {
      readLock.unlock();
    }
  }

  public void clear() {
    Lock writeLock = registryLock.writeLock();
    try {
      writeLock.lock();
      registry.clear();
      lostObjects.clear();
    } finally {
      writeLock.unlock();
    }
  }

  public void putAndLogWarningIfDuplicate(String key, Object object) {
    Lock writeLock = registryLock.writeLock();
    try {
      writeLock.lock();

      final Object previousObject = registry.put(key, object);
      if (previousObject != null && previousObject != object) {
        if (previousObject instanceof Disposable) {
          lostObjects.add(previousObject);
        }
        // registry.put(key, value) would overwrite a previous entity with the same name. Is this really what we want?
        // Not sure whether to throw an exception or log a warning here.
        // throw new RegistrationException("TransientRegistry already contains an object named '" + key + "'. The previous
        // object would be overwritten.");
        logger.warn("TransientRegistry already contains an object named '" + key
            + "'.  The previous object will be overwritten.");
      }
    } finally {
      writeLock.unlock();
    }
  }

  public void putAll(Map<String, Object> map) {
    Lock writeLock = registryLock.writeLock();
    try {
      writeLock.lock();
      registry.putAll(map);
    } finally {
      writeLock.unlock();
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    Lock readLock = registryLock.readLock();
    try {
      readLock.lock();
      return (T) registry.get(key);
    } finally {
      readLock.unlock();
    }
  }

  public Object remove(String key) {
    Lock writeLock = registryLock.writeLock();
    try {
      writeLock.lock();
      return registry.remove(key);
    } finally {
      writeLock.unlock();
    }
  }

  public Set<Map.Entry<String, Object>> entrySet() {
    return registry.entrySet();
  }

  public Set<Object> getLostObjects() {
    return lostObjects;
  }

  public void lockForReading() {
    registryLock.readLock().lock();
  }

  public void unlockForReading() {
    registryLock.readLock().unlock();
  }
}
