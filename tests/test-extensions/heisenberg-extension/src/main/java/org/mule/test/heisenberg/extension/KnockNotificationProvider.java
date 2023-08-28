/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.KNOCKED_DOOR;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.KNOCKING_DOOR;
import org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class KnockNotificationProvider implements NotificationActionProvider {

  @Override
  public Set<NotificationActionDefinition> getNotificationActions() {
    return ImmutableSet.<NotificationActionDefinition>builder()
        .add(KNOCKING_DOOR)
        .add(KNOCKED_DOOR)
        .build();
  }

}
