/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.metadata.DataType.fromType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.SimpleKnockeableDoor;

public enum HeisenbergNotificationAction implements NotificationActionDefinition<HeisenbergNotificationAction> {

  NEW_BATCH(fromType(Integer.class)),

  NEXT_BATCH(fromType(Long.class)),

  BATCH_DELIVERED(fromType(Long.class)),

  BATCH_DELIVERY_FAILED(fromType(PersonalInfo.class)),

  BATCH_FAILED(fromType(Integer.class)),

  BATCH_TERMINATED(fromType(Integer.class)),

  KNOCKING_DOOR(fromType(SimpleKnockeableDoor.class)),

  KNOCKED_DOOR(fromType(SimpleKnockeableDoor.class));

  private final DataType dataType;

  HeisenbergNotificationAction(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }

}
