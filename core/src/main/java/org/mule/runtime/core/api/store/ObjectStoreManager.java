/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import java.io.Serializable;

public interface ObjectStoreManager {

  int UNBOUNDED = 0;

  /**
   * Return the partition of the default in-memory store with the given name, creating it if necessary.
   *
   * @param name the name of the object store
   * @return an {@link org.mule.runtime.core.api.store.ObjectStore}
   */
  <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name);

  /**
   * Return the partition of the default in-memory or persistent store with the given name, creating it if necessary.
   *
   * @param name the name of the object store
   * @param isPersistent whether it should be in memory or persistent
   *
   * @return an {@link org.mule.runtime.core.api.store.ObjectStore}
   */
  <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent);

  /**
   * Return the monitored partition of the default in-memory or persistent store with the given name, creating it if necessary.
   *
   * @param name the name of the object store
   * @param isPersistent whether it should be in memory or persistent
   * @param maxEntries what's the max number of entries allowed. Exceeding entries will be removed when expiration thread runs
   * @param entryTTL entry timeout in milliseconds.
   * @param expirationInterval how frequently should the expiration thread run
   *
   * @return an {@link org.mule.runtime.core.api.store.ObjectStore}
   */
  <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent, int maxEntries,
                                                                   long entryTTL, long expirationInterval);

  /**
   * Delete all objects from the partition
   */
  void disposeStore(ObjectStore<? extends Serializable> store) throws ObjectStoreException;
}
