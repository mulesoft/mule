/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.scheduler.Scheduler;

/**
 * Contains information about an event that happenned in the Mule Server. The event may happen in any Mule artifact running on the
 * Mule Server (an extension, a plugin, or an inner component of the Runtime).
 * <p>
 * The nature of the event that occurred is identified by the concrete type of the {@link Notification} and its
 * {@link #getAction() action}.
 * <p>
 * The concrete type of the {@link Notification} is an indication of the kind of component that caused the event, and in may
 * onctain additional data describing the event.
 * 
 * @since 4.0
 */
public interface Notification {

  /**
   * Indicates the synchronicity of the processing of this notification.
   * 
   * @return Whether the execution of the listeners for this notification will happen on the same thread that called
   *         {@link NotificationDispatcher#dispatch(Notification)} or will be dispatched to a {@link Scheduler} for asynchronous
   *         processing.
   */
  default boolean isSynchronous() {
    return false;
  }

  /**
   * Informs the kind of event that caused this notification to be fired.
   * 
   * @return the kind of action that caused this notification to be fired.
   */
  Action getAction();

  /**
   * Indicates the kind of action that causes a {@link Notification} to be fired.
   */
  public interface Action {

  }
}
