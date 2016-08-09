/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPartitionedObjectStore<T extends Serializable> implements PartitionableObjectStore<T> {

  protected final static String DEFAULT_PARTITION = "DEFAULT_PARTITION";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void open() throws ObjectStoreException {
    open(DEFAULT_PARTITION);
  }

  @Override
  public void close() throws ObjectStoreException {
    close(DEFAULT_PARTITION);
  }

  @Override
  public List<Serializable> allKeys() throws ObjectStoreException {
    return allKeys(DEFAULT_PARTITION);
  }

  @Override
  public boolean contains(Serializable key) throws ObjectStoreException {
    return contains(key, DEFAULT_PARTITION);
  }

  @Override
  public void store(Serializable key, T value) throws ObjectStoreException {
    store(key, value, DEFAULT_PARTITION);
  }

  @Override
  public T retrieve(Serializable key) throws ObjectStoreException {
    return retrieve(key, DEFAULT_PARTITION);
  }

  @Override
  public T remove(Serializable key) throws ObjectStoreException {
    return remove(key, DEFAULT_PARTITION);
  }

  @Override
  public void clear() throws ObjectStoreException {
    this.clear(DEFAULT_PARTITION);
  }

}
