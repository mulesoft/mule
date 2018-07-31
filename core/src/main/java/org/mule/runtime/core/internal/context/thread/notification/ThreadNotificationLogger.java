/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationService.THREAD_LOGGING;

public class ThreadNotificationLogger {

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

  public void setStartingThread(CoreEvent event) {
    setStartingThread(event, false);
  }

  public void setStartingThread(CoreEvent event, boolean avoidIfSetted) {
    if (!isEnabled()) {
      return;
    }
    if (avoidIfSetted && threadNotificationBuilders.containsKey(event.getContext().getId())) {
      return;
    }
    sameThread.set(true);
    DefaultThreadNotificationElement.Builder builder = new DefaultThreadNotificationElement.Builder();
    builder.fromThread(Thread.currentThread());
    threadNotificationBuilders.put(event.getContext().getId(), builder);
  }

  public void setFinishThread(CoreEvent event) {
    if (!isEnabled()) {
      return;
    }
    if (sameThread.get() != null && sameThread.get()) {
      sameThread.set(false);
      return;
    }
    DefaultThreadNotificationElement.Builder builder = threadNotificationBuilders.remove(event.getContext().getId());
    builder.toThread(Thread.currentThread());
    threadNotificationService.addThreadNotificationElement(builder.build());
  }
}
