/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.object.iterator;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.streaming.iterator.Producer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Producer} to stream the contents of a {@link ObjectStore}
 * 
 * @since 3.5.0
 */
public class ObjectStoreProducer<T extends Serializable> implements Producer<T> {

  private static final Logger logger = LoggerFactory.getLogger(ObjectStoreProducer.class);

  private ObjectStore<T> objectStore;
  private Iterator<String> keys;
  private int size;

  public ObjectStoreProducer(ObjectStore<T> objectStore) {
    if (objectStore == null) {
      throw new IllegalArgumentException("Cannot construct a producer with a null object store");
    }

    this.objectStore = objectStore;
    try {
      List<String> allKeys = new ArrayList<>(objectStore.allKeys());
      this.keys = allKeys.iterator();
      this.size = allKeys.size();
    } catch (ObjectStoreException e) {
      throw new RuntimeException("Could not construct producer because exception was found retrieving keys", e);
    }
  }

  @Override
  public T produce() {
    if (this.objectStore == null || !this.keys.hasNext()) {
      return null;
    }

    String key = this.keys.next();
    try {
      return this.objectStore.retrieve(key);
    } catch (ObjectDoesNotExistException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format(
                                   "key %s no longer available in objectstore. This is likely due to a concurrency issue. Will continue with next key if available",
                                   key));
      }

      return this.produce();
    } catch (ObjectStoreException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getSize() {
    return this.size;
  }

  @Override
  public void close() throws IOException {
    this.objectStore = null;
    this.keys = null;
  }

}
