/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.store;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.List;

public interface PartitionableObjectStore<T extends Serializable> extends ObjectStore<T> {

  String DEFAULT_PARTITION_NAME = "DEFAULT_PARTITION";

  boolean contains(String key, String partitionName) throws ObjectStoreException;

  void store(String key, T value, String partitionName) throws ObjectStoreException;

  T retrieve(String key, String partitionName) throws ObjectStoreException;

  T remove(String key, String partitionName) throws ObjectStoreException;

  List<String> allKeys(String partitionName) throws ObjectStoreException;

  List<String> allPartitions() throws ObjectStoreException;

  void open(String partitionName) throws ObjectStoreException;

  void close(String partitionName) throws ObjectStoreException;

  void disposePartition(String partitionName) throws ObjectStoreException;

  void clear(String partitionName) throws ObjectStoreException;
}
