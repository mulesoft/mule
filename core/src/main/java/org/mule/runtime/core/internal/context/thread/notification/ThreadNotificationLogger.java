/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import static org.mule.runtime.core.api.context.thread.notification.ThreadNotificationService.THREAD_LOGGING;

import org.mule.runtime.core.api.context.thread.notification.ThreadNotificationService;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadNotificationLogger {

  public static final String THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY = "mule.nb.ThreadNotificationLogger";

  private Map<String, DefaultThreadNotificationElement.Builder> threadNotificationBuilders = new ConcurrentHashMap<>();
  private ThreadNotificationService threadNotificationService;
  private ThreadLocal<Boolean> sameThread = new ThreadLocal<>();

  public ThreadNotificationLogger(ThreadNotificationService threadNotificationService) {
    this.threadNotificationService = threadNotificationService;
    sameThread.set(false);
  }

  private boolean isEnabled() {
    return THREAD_LOGGING;
  }

  public void setStartingThread(String eventId) {
    setStartingThread(eventId, false);
  }

  public void setStartingThread(String eventId, boolean avoidIfSet) {
    if (!isEnabled()) {
      return;
    }
    if (avoidIfSet && threadNotificationBuilders.containsKey(eventId)) {
      return;
    }
    sameThread.set(true);
    DefaultThreadNotificationElement.Builder builder = new DefaultThreadNotificationElement.Builder();
    builder.fromThread(Thread.currentThread());
    threadNotificationBuilders.put(eventId, builder);
  }

  public void setFinishThread(String eventId) {
    if (!isEnabled()) {
      return;
    }
    if (sameThread.get() != null && sameThread.get()) {
      sameThread.set(false);
      return;
    }
    DefaultThreadNotificationElement.Builder builder = threadNotificationBuilders.remove(eventId);
    builder.toThread(Thread.currentThread());
    threadNotificationService.addThreadNotificationElement(builder.build());
  }
}
