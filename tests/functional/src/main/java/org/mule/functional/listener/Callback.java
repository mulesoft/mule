/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.listener;

import org.mule.runtime.api.notification.Notification;

/**
 * Callback represents an operation to be executed upon notification receive by a test listener such as
 * {@link org.mule.functional.listener.ExceptionListener} or {@link org.mule.functional.listener.FlowExecutionListener}
 *
 * @param <T> the type of the source object provided by the listened notification.
 */
public interface Callback<T> {

  /**
   * @param source is the source value of the {@link Notification} received by the notification listener that executes this
   *               callback
   */
  public void execute(final T source);
}
