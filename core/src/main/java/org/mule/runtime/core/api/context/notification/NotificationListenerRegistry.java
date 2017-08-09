/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import java.util.function.Predicate;

/**
 * Allows to register/unregister {@link NotificationListener}s for {@link Notification}s fired by the Mule container, extensions
 * and applications.
 * 
 * @since 4.0
 */
public interface NotificationListenerRegistry {

  /**
   * Registers a {@link NotificationListener}. The listener will be notified when a particular event happens within the server.
   * Typically this is not an event in the same sense as a Mule Event (although there is nothing stopping the implementation of
   * this class triggering listeners when a Mule Event is received).
   *
   * @param listener the listener to register
   * @param <N> the concrete type of the notification to be handled by the {@code listener}
   */
  <N extends Notification> void registerListener(NotificationListener<N> listener);

  /**
   * Registers a {@link NotificationListener}. The listener will be notified when a particular event happens within the server.
   * Typically this is not an event in the same sense as a Mule Event (although there is nothing stopping the implementation of
   * this class triggering listeners when a Mule Event is received).
   *
   * @param listener the listener to register
   * @param selector a filter to apply on a fired {@link Notification} before calling the {@code listener} with it. Non-null.
   * @param <N> the concrete type of the notification to be handled by the {@code listener}
   */
  <N extends Notification> void registerListener(NotificationListener<N> listener, Predicate<N> selector);

  /**
   * Unregisters a previously registered {@link NotificationListener}. If the listener has not already been registered, this
   * method should return without exception
   *
   * @param listener the listener to unregister
   * @param <N> the concrete type of the notification handled by the {@code listener}
   */
  <N extends Notification> void unregisterListener(NotificationListener<N> listener);

}
