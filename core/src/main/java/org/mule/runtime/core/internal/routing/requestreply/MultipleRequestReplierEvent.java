/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.requestreply;

import org.mule.runtime.core.api.event.BaseEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MultipleRequestReplierEvent implements Serializable {

  private final List<BaseEvent> muleEvents = new ArrayList<>();

  protected synchronized void addEvent(BaseEvent event) {
    muleEvents.add(event);
  }

  protected synchronized void removeEvent() {
    muleEvents.remove(0);
  }

  protected synchronized BaseEvent getEvent() {
    return muleEvents.get(0);
  }
}
