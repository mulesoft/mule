/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;

/**
 * <code>QueueProfile</code> determines how an internal queue for a service will behave
 */
public class QueueProfile extends AbstractComponent {

  private int maxOutstandingMessages = 0;
  private boolean persistent;

  public static QueueProfile newInstancePersistingToDefaultMemoryQueueStore() {
    return new QueueProfile(false);
  }

  public static QueueProfile newInstanceWithPersistentQueueStore() {
    return new QueueProfile(true);
  }

  public QueueProfile(boolean persistent) {
    this.persistent = persistent;
  }

  public QueueProfile(int maxOutstandingMessages, boolean persistent) {
    this.maxOutstandingMessages = maxOutstandingMessages;
    this.persistent = persistent;
  }

  /**
   * This specifies the number of messages that can be queued before it starts blocking.
   *
   * @return the max number of messages that will be queued
   */
  public int getMaxOutstandingMessages() {
    return maxOutstandingMessages;
  }

  /**
   * This specifies the number of messages that can be queued before it starts blocking.
   *
   * @param maxOutstandingMessages the max number of messages that will be queued
   */
  public void setMaxOutstandingMessages(int maxOutstandingMessages) {
    this.maxOutstandingMessages = maxOutstandingMessages;
  }

  public QueueConfiguration configureQueue(String component, QueueManager queueManager)
      throws InitialisationException {
    QueueConfiguration qc = new DefaultQueueConfiguration(maxOutstandingMessages, persistent);
    queueManager.setQueueConfiguration(component, qc);
    return qc;
  }

  @Override
  public String toString() {
    return "QueueProfile{maxOutstandingMessage=" + maxOutstandingMessages + ", persistent=" + persistent + "}";
  }
}
