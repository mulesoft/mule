/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

public class MyCustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor {

  @Override
  public Processor getNext() {
    return next;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processNext(Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).payload((String) event.getMessage().getPayload().getValue() + "!")
            .build())
        .build());
  }

}


