/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

/**
 * Allows the Mule container, extensions and applications to fire {@link Notification notifications}.
 * <p>
 * A {@link #dispatch(Notification) fired} {@link Notification notification} is dispatched by the implementation to any registered
 * listener for the concrete type of the {@link Notification}.
 * 
 * @since 4.0
 */
public interface NotificationDispatcher {

  /**
   * Send the {@code notification} to all the registered listeners.
   * 
   * @param notification the notification to fire.
   */
  void dispatch(Notification notification);
}
