/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

public class MyCustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor {

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    return processNext(MuleEvent.builder(event)
        .message(MuleMessage.builder(event.getMessage()).payload((String) event.getMessage().getPayload() + "!").build())
        .build());
  }

}


