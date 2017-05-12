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
import org.mule.runtime.core.api.context.notification.SynchronousServerEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to notify that a message was received or sent through a Connector
 */
public class ConnectorMessageNotification extends EnrichedServerNotification implements SynchronousServerEvent {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -5118299601117624094L;

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(ConnectorMessageNotification.class);

  public static final int MESSAGE_RECEIVED = MESSAGE_EVENT_ACTION_START_RANGE + 1;
  public static final int MESSAGE_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 5;
  public static final int MESSAGE_ERROR_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 6;

  public static final int MESSAGE_REQUEST_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 4;
  public static final int MESSAGE_REQUEST_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 3;

  static {
    registerAction("receive", MESSAGE_RECEIVED);
    registerAction("response", MESSAGE_RESPONSE);
    registerAction("error response", MESSAGE_ERROR_RESPONSE);

    registerAction("begin request", MESSAGE_REQUEST_BEGIN);
    registerAction("end request", MESSAGE_REQUEST_END);
  }

  public ConnectorMessageNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct,
                                      int action) {
    super(notificationInfo, action, flowConstruct);
    this.flowConstruct = flowConstruct;
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{action=" + getActionName(action) + ", endpoint: " + getLocationUri() + ", resourceId="
        + resourceIdentifier
        + ", timestamp=" + timestamp + ", serverId=" + serverId + ", message: " + source + "}";
  }

  @Override
  public String getType() {
    return TYPE_TRACE;
  }
}
