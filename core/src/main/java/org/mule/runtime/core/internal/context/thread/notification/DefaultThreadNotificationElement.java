/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationService.ThreadNotificationElement;

/**
 * Implementation of {@link ThreadNotificationElement}
 *
 * @since 4.0
 */
public final class DefaultThreadNotificationElement implements ThreadNotificationElement {

  private String fromThreadType;
  private String toThreadType;
  private long measuredLatency;

  private DefaultThreadNotificationElement(String from, String to, long time) {
    this.fromThreadType = from;
    this.toThreadType = to;
    this.measuredLatency = time;
  }

  @Override
  public long getLatencyTime() {
    return measuredLatency;
  }

  @Override
  public String getFromThreadType() {
    return fromThreadType;
  }

  @Override
  public String getToThreadType() {
    return toThreadType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String fromType, toType;
    private long startTime, stopTime;

    public ThreadNotificationService.ThreadNotificationElement build() {
      return new DefaultThreadNotificationElement(fromType, toType, stopTime - startTime);
    }

    public Builder fromThread(Thread thread) {
      this.fromType = thread.getThreadGroup().getName();
      this.startTime = System.nanoTime();
      return this;
    }

    public Builder toThread(Thread thread) {
      this.toType = thread.getThreadGroup().getName();
      this.stopTime = System.nanoTime();
      return this;
    }
  }

}
