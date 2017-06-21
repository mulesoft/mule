/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.api.construct.FlowConstruct;

/**
 * This class is from Mule 2.2.5. It is modified so the ExceptionNotification has a resourceId of the exception type. This is only
 * here so we can avoid doing a hot fix of Mule to run MMC. This will be removed in future releases of MMC.
 */
public class ExceptionNotification extends EnrichedServerNotification {

  /**
   * Serial version.
   */
  private static final long serialVersionUID = -43091546451476239L;
  public static final int EXCEPTION_ACTION = EXCEPTION_EVENT_ACTION_START_RANGE + 1;

  static {
    registerAction("exception", EXCEPTION_ACTION);
  }

  public ExceptionNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct) {
    super(notificationInfo, EXCEPTION_ACTION, flowConstruct);
  }

  @Override
  public String getType() {
    return TYPE_ERROR;
  }
}
