/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

/**
 * Listener for queue operations affecting the queue store cache.
 */
public interface QueueStoreCacheListener {

  /**
   * @param queueStore queue to be disposed
   */
  void disposeQueueStore(QueueStore queueStore);

  /**
   * @param queueStore queue to be closed.
   */
  void closeQueueStore(QueueStore queueStore);

}
