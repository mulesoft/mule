/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.util.queue.QueueStoreDelegate;

import java.io.Serializable;

/**
 * A QueueStoreDelegate that can take objects directly from its store
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public interface TakingQueueStoreDelegate extends QueueStoreDelegate {

  /**
   * Poll the queue for its first member, and, if there is one, remove and return the corresponding object from the object store
   */
  Serializable takeFromObjectStore(long timeout) throws InterruptedException;

  void writeToObjectStore(Serializable data) throws InterruptedException, ObjectStoreException;
}
