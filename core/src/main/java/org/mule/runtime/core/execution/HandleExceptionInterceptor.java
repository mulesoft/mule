/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionCallback;

class HandleExceptionInterceptor implements ExecutionInterceptor<MuleEvent> {

  private final ExecutionInterceptor<MuleEvent> next;
  private final MessagingExceptionHandler messagingExceptionHandler;
  private final FlowConstruct flow;

  public HandleExceptionInterceptor(ExecutionInterceptor<MuleEvent> next, MessagingExceptionHandler messagingExceptionHandler,
                                    FlowConstruct flow) {
    this.next = next;
    this.messagingExceptionHandler = messagingExceptionHandler;
    this.flow = flow;
  }

  @Override
  public MuleEvent execute(ExecutionCallback<MuleEvent> callback, ExecutionContext executionContext) throws Exception {
    try {
      return next.execute(callback, executionContext);
    } catch (MessagingException e) {
      MuleEvent result;
      if (messagingExceptionHandler != null) {
        result = messagingExceptionHandler.handleException(e, e.getEvent());
      } else {
        result = flow.getExceptionListener().handleException(e, e.getEvent());
      }
      e.setProcessedEvent(result);
      throw e;
    } catch (Exception e) {
      throw e;
    }
  }
}
