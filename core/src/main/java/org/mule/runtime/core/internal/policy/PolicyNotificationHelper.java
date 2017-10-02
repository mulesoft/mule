/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.notification.PolicyNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.function.Consumer;

/**
 * Helper to fire policy notifications from a specific policy
 * 
 * @since 4.0
 */
public class PolicyNotificationHelper {

  private ServerNotificationHandler notificationHandler;

  /**
   * Id of the policy firing the notification
   */
  private String policyId;

  /**
   * Component firing the notification
   */
  private Component component;

  public PolicyNotificationHelper(ServerNotificationHandler notificationHandler, String policyId, Component component) {
    this.notificationHandler = notificationHandler;
    this.policyId = policyId;
    this.component = component;
  }

  /**
   * Creates a event consumer that fires a notification using the specified action
   * 
   * @param action the action the notification is created with
   * @return the created consumer
   */
  public Consumer<? super CoreEvent> notification(int action) {
    return (Consumer<CoreEvent>) event -> fireNotification(event, null, action);
  }

  /**
   * Creates an exception consumer that fires a notification using the specified action
   *
   * @param action the action the notification is created with
   * @return the created consumer
   */
  public Consumer<? super MessagingException> errorNotification(int action) {
    return (Consumer<MessagingException>) e -> fireNotification(e.getEvent(), e, action);
  }

  public void fireNotification(CoreEvent event, Exception e, int action) {
    if (notificationHandler != null && event != null && notificationHandler.isNotificationEnabled(PolicyNotification.class)) {
      notificationHandler.fireNotification(createNotification(event, e, action));
    }
  }

  private PolicyNotification createNotification(CoreEvent event, Exception e, int action) {
    EnrichedNotificationInfo info = EnrichedNotificationInfo.createInfo(event, e, component);
    return new PolicyNotification(policyId, info, action, component.getLocation());
  }

}
