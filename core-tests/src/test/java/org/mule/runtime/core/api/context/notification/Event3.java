/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;

public class Event3 extends AbstractServerNotification {

  public Event3() {
    super("", 0);
  }

  public Event3(String id) {
    super("", 0, id);
  }

  @Override
  public String getEventName() {
    return "Event3";
  }
}
