/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

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
  protected void nullifyExceptionPayloadIfRequired(MuleEvent event) {
    event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(null).build());
  }

  @Override
  protected MuleEvent afterRouting(Exception exception, MuleEvent event) {
    return event;
  }

  @Override
  protected MuleEvent beforeRouting(Exception exception, MuleEvent event) {
    return event;
  }

}
