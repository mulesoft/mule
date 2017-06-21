/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.execution.ExecutionCallback;

public class RethrowExceptionInterceptor implements ExecutionInterceptor<Event> {

  private final ExecutionInterceptor<Event> next;

  public RethrowExceptionInterceptor(ExecutionInterceptor<Event> next) {
    this.next = next;
  }

  @Override
  public Event execute(ExecutionCallback<Event> processingCallback, ExecutionContext executionContext) throws Exception {
    try {
      return this.next.execute(processingCallback, executionContext);
    } catch (MessagingException e) {
      if (e.handled()) {
        return e.getEvent();
      }
      throw e;
    }
  }
}
