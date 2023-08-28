/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
