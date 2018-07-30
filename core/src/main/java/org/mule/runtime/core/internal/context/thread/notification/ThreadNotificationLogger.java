/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationService.THREAD_LOGGING;

public class ThreadNotificationLogger {

  private Map<String, DefaultThreadNotificationElement.Builder> threadNotificationBuilders = new ConcurrentHashMap<>();

  private Optional<ThreadNotificationService> threadNotificationService;

  public ThreadNotificationLogger(Optional<ThreadNotificationService> threadNotificationService) {
    this.threadNotificationService = threadNotificationService;
  }

  private boolean isEnabled() {
    return THREAD_LOGGING && threadNotificationService.isPresent();
  }

  public void setStartingThread(String id) {
    if (!isEnabled()) {
      return;
    }
    DefaultThreadNotificationElement.Builder builder = new DefaultThreadNotificationElement.Builder();
    builder.fromThread(Thread.currentThread());
    threadNotificationBuilders.put(id, builder);
  }

  public void setFinishThread(String id) {
    if (!isEnabled()) {
      return;
    }
    DefaultThreadNotificationElement.Builder builder = threadNotificationBuilders.remove(id);
    builder.toThread(Thread.currentThread());
    threadNotificationService.get().addThreadNotificationElement(builder.build());
  }
}
