/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.routing.filter.Filter;

public class ExceptionThrowingFilter implements Filter {

  @Override
  public boolean accept(Message message, Event.Builder builder) {
    throw new RuntimeException();
  }

}
