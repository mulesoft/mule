/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.client;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Maintains test connector configuration
 */
public class TestConnectorConfig {

  public static final String DEFAULT_CONFIG_ID = "_defaultTestConnectorConfig";

  private final Map<String, BlockingQueue<CoreEvent>> queues = new HashMap<>();

  /**
   * Reads an event from a given queue waiting up to the specified wait time if necessary for an element to become available.
   *
   * @param queueName name of the queue which the event is read from. Non empty
   * @param timeout maximum number of milliseconds to wait for an available event. Non negative
   * @return a non null event if available before the timeout expires, null otherwise.
   */
  public CoreEvent poll(String queueName, long timeout) {
    checkArgument(!StringUtils.isEmpty(queueName), "Queue name cannot be empty");
    checkArgument(timeout >= 0L, "Timeout cannot be negative");

    final BlockingQueue<CoreEvent> queue = getQueue(queueName);
    try {
      return queue.poll(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.interrupted();
      return null;
    }
  }

  /**
   * Reads an event from a given queue waiting if necessary until an element becomes available.
   *
   * @param queueName name of the queue which the event is read from. Non empty
   * @return a non null event
   */
  public CoreEvent take(String queueName) {
    checkArgument(!StringUtils.isEmpty(queueName), "Queue name cannot be empty");

    final BlockingQueue<CoreEvent> queue = getQueue(queueName);
    try {
      return queue.take();
    } catch (InterruptedException e) {
      Thread.interrupted();
      return null;
    }
  }

  /**
   * Writes a even to to a given queue waiting if necessary for space to become available
   *
   * @param queueName name of the queue which the event is write to. Non empty
   * @param event event to be stored. Non null
   */
  public void write(String queueName, CoreEvent event) {
    checkArgument(!StringUtils.isEmpty(queueName), "Queue name cannot be empty");
    checkArgument(event != null, "Event cannot be null");
    final BlockingQueue<CoreEvent> queue = getQueue(queueName);
    try {
      queue.put(event);
    } catch (InterruptedException e) {
      Thread.interrupted();
      throw new IllegalStateException(e);
    }
  }

  private BlockingQueue<CoreEvent> getQueue(String queueName) {
    BlockingQueue<CoreEvent> queue = queues.get(queueName);
    if (queue == null) {
      synchronized (queues) {
        queue = queues.get(queueName);
        if (queue == null) {
          queue = new LinkedBlockingDeque(100);
          queues.put(queueName, queue);
        }
      }
    }

    return queue;
  }
}
