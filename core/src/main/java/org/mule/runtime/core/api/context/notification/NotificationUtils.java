/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Utilitary methods for working with {@link org.mule.runtime.api.notification.Notification}s
 *
 * @since 4.3.0
 */
public class NotificationUtils {

  /**
   * Creates {@link ConnectorMessageNotification} to be fired.
   *
   * @param source
   * @param event {@link CoreEvent}
   * @param location the location of the component that generated the notification
   * @param action the action code for the notification
   * @return
   */
  public static ConnectorMessageNotification createConnectorMessageNotification(Component source, Event event,
                                                                                ComponentLocation location, int action) {
    return new ConnectorMessageNotification(createInfo(event, null, source), location, action);
  }

}
