/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;

public class Event2 extends AbstractServerNotification {

  public Event2() {
    super("", NO_ACTION_ID);
  }

  public Event2(String id) {
    super("", NO_ACTION_ID, id);
  }

  @Override
  public String getEventName() {
    return "Event2";
  }
}
