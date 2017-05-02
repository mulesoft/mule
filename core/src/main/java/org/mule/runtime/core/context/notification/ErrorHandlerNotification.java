/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo;
import org.mule.runtime.core.api.context.notification.EnrichedServerNotification;

public class ErrorHandlerNotification extends EnrichedServerNotification {

  // Fired when processing of exception strategy starts
  public static final int PROCESS_START = EXCEPTION_STRATEGY_MESSAGE_EVENT_ACTION_START_RANGE + 1;
  // Fired when processing of exception strategy ends
  public static final int PROCESS_END = EXCEPTION_STRATEGY_MESSAGE_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("exception strategy process start", PROCESS_START);
    registerAction("exception strategy process end", PROCESS_END);
  }

  public ErrorHandlerNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct, int action) {
    super(notificationInfo, action, flowConstruct);
  }
}
