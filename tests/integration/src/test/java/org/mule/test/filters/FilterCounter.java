/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.filters;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.routing.filter.Filter;

import java.util.concurrent.atomic.AtomicInteger;

public class FilterCounter implements Filter {

  public static AtomicInteger counter = new AtomicInteger();

  /**
   * Increments the counter if it passes the filter
   */
  @Override
  public boolean accept(Message message, Event.Builder builder) {
    if ("true".equals(((InternalMessage) message).getInboundProperty("pass"))) {
      counter.incrementAndGet();
      return true;
    }
    return false;
  }

  public boolean test(int arg0) {
    // TODO Auto-generated method stub
    return false;
  }

}


