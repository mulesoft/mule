/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalMessage;

/**
 * Handler that will consume errors and finally commit transactions. Replaces the catch-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorContinueHandler extends TemplateOnErrorHandler {

  public OnErrorContinueHandler() {
    setHandleException(true);
  }

  @Override
  protected Event nullifyExceptionPayloadIfRequired(Event event) {
    return Event.builder(event).error(null).message(InternalMessage.builder(event.getMessage()).exceptionPayload(null).build())
        .build();
  }

  @Override
  protected Event afterRouting(MessagingException exception, Event event) {
    return event;
  }

  @Override
  protected Event beforeRouting(MessagingException exception, Event event) {
    return event;
  }

}
