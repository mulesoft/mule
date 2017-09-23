/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;

public class Event1 extends AbstractServerNotification {

  public Event1() {
    super("", 0);
  }

  public Event1(String id) {
    super("", 0, id);
  }

}
