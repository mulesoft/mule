/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.Map.Entry;

/**
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private MuleContext muleContext;

  @Override
  public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException {
    try {
      return messageProcessor.process(event);
    } catch (MessagingException messagingException) {
      if (messagingException.getFailingMessageProcessor() == null) {
        throw putContext(messagingException, messageProcessor, event);
      } else {
        throw putContext(messagingException, messagingException.getFailingMessageProcessor(), event);
      }
    } catch (Throwable ex) {
      throw putContext(new MessagingException(event, ex, messageProcessor), messageProcessor, event);
    }
  }

  private MessagingException putContext(MessagingException messagingException, MessageProcessor failingMessageProcessor,
                                        MuleEvent event) {
    for (ExceptionContextProvider exceptionContextProvider : muleContext.getExceptionContextProviders()) {
      for (Entry<String, Object> contextInfoEntry : exceptionContextProvider.getContextInfo(event, failingMessageProcessor)
          .entrySet()) {
        if (!messagingException.getInfo().containsKey(contextInfoEntry.getKey())) {
          messagingException.getInfo().put(contextInfoEntry.getKey(), contextInfoEntry.getValue());
        }
      }
    }
    return messagingException;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
