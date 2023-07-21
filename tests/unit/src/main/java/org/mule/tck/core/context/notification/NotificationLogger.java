/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.core.context.notification;

import org.mule.runtime.api.notification.Notification;

import java.util.List;

/**
 * Provides access to the notifications generated during a test run.
 * 
 * @param <T> the type of notifications to store.
 * @since 4.0
 */
public interface NotificationLogger<T extends Notification> {

  /**
   * @return the notifications generated during a test run.
   */
  public List<T> getNotifications();

}
