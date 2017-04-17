/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingException;

public class TestExceptionStrategy extends AbstractMessagingExceptionStrategy {

  public TestExceptionStrategy() {
    super(null);
  }

  @Override
  public Event handleException(MessagingException exception, Event event) {
    return Event.builder(event)
        .message(InternalMessage.builder(super.handleException(exception, event).getMessage()).payload("Ka-boom!").build())
        .build();
  }

  public boolean isRedeliver() {
    return false;
  }
}
