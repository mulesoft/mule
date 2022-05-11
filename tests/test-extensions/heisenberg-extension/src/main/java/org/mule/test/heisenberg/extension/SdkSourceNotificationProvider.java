/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_DELIVERED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_DELIVERY_FAILED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_FAILED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_TERMINATED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.NEW_BATCH;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.NEXT_BATCH;

import org.mule.sdk.api.annotation.notification.NotificationActionProvider;
import org.mule.sdk.api.notification.NotificationActionDefinition;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class SdkSourceNotificationProvider implements NotificationActionProvider {

  @Override
  public Set<NotificationActionDefinition> getNotificationActions() {
    return ImmutableSet.<NotificationActionDefinition>builder()
        .add(NEW_BATCH)
        .add(NEXT_BATCH)
        .add(BATCH_DELIVERED)
        .add(BATCH_DELIVERY_FAILED)
        .add(BATCH_FAILED)
        .add(BATCH_TERMINATED)
        .build();
  }
}
