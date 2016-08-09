/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.Collection;

/**
 * A DefaultQueueStore delegates the actual work of processing its queue to one of these.
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public interface QueueInfoDelegate {

  /**
   * append a new member to the end of the queue
   */
  void putNow(Serializable o);

  /**
   * Offer to append a new member to the end of the queue
   */
  boolean offer(Serializable o, int room, long timeout) throws InterruptedException, ObjectStoreException;

  /**
   * Poll the queue for its first member, and, if there is one, remove and return it
   */
  Serializable poll(long timeout) throws InterruptedException;

  /**
   * return, but do not remove, the first member of the queue
   */
  Serializable peek() throws InterruptedException;

  /**
   * Restore a previously removed member to the front of the queue
   */
  void untake(Serializable item) throws InterruptedException, ObjectStoreException;

  /**
   * Return the size of the queue
   */
  int getSize();

  /**
   * Discards all the elements in the queue
   */
  void clear() throws InterruptedException;

  /**
   * Appends all of the elements in the specified collection to the queue (optional operation).
   * 
   * @param items to be added to the queue
   * @return <tt>true</tt> if this queue changed as a result of the call
   */
  boolean addAll(Collection<? extends Serializable> items);
}
