/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;

import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.runtime.api.metadata.DataType;
import org.mule.sdk.api.notification.NotificationActionDefinition;

public enum PollItemNotificationAction implements NotificationActionDefinition<PollItemNotificationAction> {

  ACCEPTED_ITEM(fromType(String.class));

  private final DataType dataType;

  PollItemNotificationAction(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }
}
