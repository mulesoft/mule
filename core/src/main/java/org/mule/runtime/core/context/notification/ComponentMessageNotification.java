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

/**
 * These notifications are fired when before and after a service component is invoked.
 */
public class ComponentMessageNotification extends EnrichedServerNotification {

  private static final long serialVersionUID = -6369685122731797646L;

  public static final int COMPONENT_PRE_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 1;
  public static final int COMPONENT_POST_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("component pre invoke", COMPONENT_PRE_INVOKE);
    registerAction("component post invoke", COMPONENT_POST_INVOKE);
  }

  public ComponentMessageNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct,
                                      int action) {
    super(notificationInfo, action, flowConstruct);
    this.flowConstruct = flowConstruct;
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{action=" + getActionName(action) + ", message: " + source + ", resourceId=" + resourceIdentifier
        + ", timestamp=" + timestamp + ", serverId=" + serverId + ", component: " + "}";
  }

  @Override
  public String getType() {
    return TYPE_TRACE;
  }
}
