/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.store;

import static java.lang.Thread.sleep;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Migration implementation from old {@link ObjectStore} mechanism to newer one. For retrievals, it will continue using the old
 * version until the first step of migration is over. During that step, there will be a thread that will check if there is any
 * difference between the old OS and the newer one. If there is, it is because an old node modified the OS, and we have to
 * consider that value.
 *
 * After the first step of migration is over, and we start with the second step, in which newer nodes only use the new version of
 * OS, then we will stop considering the old OS.
 */
public class TwoImplementationsObjectStore<T extends Serializable> implements ObjectStore<T>, Startable, Stoppable {

  private final ObjectStore<T> oldOS;
  private final ObjectStore<T> newOs;
  private final AtomicBoolean considerOldOs = new AtomicBoolean(true);
  private Thread updater = null;
  private Object locker = new Object();

  public TwoImplementationsObjectStore(ObjectStore<T> oldos, ObjectStore<T> newos) {
    this.oldOS = oldos;
    this.newOs = newos;
  }


  @Override
  public boolean contains(String key) throws ObjectStoreException {
    return considerOldOs.get() ? oldOS.contains(key) : newOs.contains(key);
  }

  @Override
  public void store(String key, T value) throws ObjectStoreException {
    if (considerOldOs.get()) {
      synchronized (locker) {
        this.newOs.store(key, value);
        this.oldOS.store(key, value);
      }
    } else {
      this.newOs.store(key, value);
    }
  }

  @Override
  public T retrieve(String key) throws ObjectStoreException {
    return considerOldOs.get() ? this.oldOS.retrieve(key) : this.newOs.retrieve(key);
  }

  @Override
  public T remove(String key) throws ObjectStoreException {
    if (considerOldOs.get()) {
      synchronized (locker) {
        this.newOs.remove(key);
        return this.oldOS.remove(key);
      }
    } else {
      return this.newOs.remove(key);
    }
  }

  @Override
  public boolean isPersistent() {
    return this.newOs.isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    if (considerOldOs.get()) {
      synchronized (locker) {
        this.oldOS.clear();
        this.newOs.clear();
      }
    } else {
      this.newOs.clear();
    }
  }

  @Override
  public void open() throws ObjectStoreException {
    if (considerOldOs.get()) {
      this.oldOS.open();
    }
    this.newOs.open();
  }

  @Override
  public void close() throws ObjectStoreException {
    if (considerOldOs.get()) {
      this.oldOS.close();
    }
    this.newOs.close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return considerOldOs.get() ? oldOS.allKeys() : newOs.allKeys();
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    return considerOldOs.get() ? oldOS.retrieveAll() : newOs.retrieveAll();
  }

  private void updateNewIfNecessary() throws ObjectStoreException {
    for (String key : oldOS.allKeys()) {
      if (!newOs.contains(key) || !newOs.retrieve(key).equals(oldOS.retrieve(key))) {
        newOs.store(key, oldOS.retrieve(key));
      }
    }
    for (String key : newOs.allKeys()) {
      if (!oldOS.contains(key)) {
        newOs.remove(key);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    updater = new Thread(() -> {
      try {
        while (considerOldOs.get()) {
          try {
            synchronized (locker) {
              if (true) { // condition?
                updateNewIfNecessary();
              } else {
                considerOldOs.set(false);
              }
            }
          } catch (ObjectStoreException e) {
            // do nothing
          }
          sleep(100);
        }
      } catch (InterruptedException e) {
        // just finish
      }
    });
    updater.start();
  }

  @Override
  public void stop() throws MuleException {
    if (updater != null) {
      updater.interrupt();
      updater = null;
    }
  }
}
