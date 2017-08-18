/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

/**
 * Observer interface that objects can implement and register themselves with the Mule Server to receive notifications when a
 * {@link Notification} is {@link NotificationDispatcher#dispatch(Notification) fired}.
 * 
 * @param <T> the concrete type of {@link Notification} that an implementation can handle.
 * @since 4.0
 */
public interface NotificationListener<T extends Notification> {

  /**
   * Allows the notification handler to perform otimizations on the handling of the {@link Notification} when
   * {@link NotificationDispatcher#dispatch(Notification) fired}.
   * 
   * @return true if this listener is expected to perform blocking I/O operations, false otherwise.
   */
  default boolean isBlocking() {
    return true;
  }

  /**
   * Handles the {@link NotificationDispatcher#dispatch(Notification) fired} {@link Notification}.
   * 
   * @param notification the {@link Notification} to handle.
   */
  void onNotification(T notification);
}
