/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.listener;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Listener for flow execution complete action.
 */
public class FlowExecutionListener {

  private final List<Callback<EnrichedNotificationInfo>> callbacks = new ArrayList<>();
  private CountDownLatch flowExecutedLatch = new Latch();
  private String flowName;
  private int timeout = 10000;

  /**
   * Constructor for releasing latch when any flow execution completes
   */
  public FlowExecutionListener(NotificationListenerRegistry notificationListenerRegistry) {
    createFlowExecutionListener(notificationListenerRegistry);
  }

  /**
   * Constructor for releasing latch when flow with name flowName completes
   */
  public FlowExecutionListener(String flowName, NotificationListenerRegistry notificationListenerRegistry) {
    this.flowName = flowName;
    createFlowExecutionListener(notificationListenerRegistry);
  }

  private void createFlowExecutionListener(NotificationListenerRegistry notificationListenerRegistry) {
    notificationListenerRegistry
        .registerListener((PipelineMessageNotificationListener<PipelineMessageNotification>) notification -> {
          if (flowName != null && !notification.getResourceIdentifier().equals(flowName)) {
            return;
          }
          if (new IntegerAction(PROCESS_COMPLETE).equals(notification.getAction())) {
            for (Callback<EnrichedNotificationInfo> callback : callbacks) {
              callback.execute(notification.getInfo());
            }
            flowExecutedLatch.countDown();
          }
        });
  }

  public void waitUntilFlowIsComplete() {
    try {
      if (!flowExecutedLatch.await(timeout, MILLISECONDS)) {
        fail(format("Flow %s never completed an execution", (flowName == null ? "ANY FLOW" : flowName)));
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param numberOfExecutionsRequired number of times that the listener must be notified before releasing the latch.
   */
  public FlowExecutionListener setNumberOfExecutionsRequired(int numberOfExecutionsRequired) {
    this.flowExecutedLatch = new CountDownLatch(numberOfExecutionsRequired);
    return this;
  }

  public FlowExecutionListener setTimeoutInMillis(int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * @param callback callback to be executed once a notification is received
   */
  public void addListener(Callback<EnrichedNotificationInfo> callback) {
    this.callbacks.add(callback);
  }
}
