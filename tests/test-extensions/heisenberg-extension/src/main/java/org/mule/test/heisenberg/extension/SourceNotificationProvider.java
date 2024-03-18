/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class SourceNotificationProvider implements NotificationActionProvider {

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
