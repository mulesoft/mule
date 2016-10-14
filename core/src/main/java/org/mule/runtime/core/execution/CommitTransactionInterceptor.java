/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.api.Event.getCurrentEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.transaction.TransactionCoordination;

/**
 * Commits any pending transaction.
 * <p/>
 * This interceptor must be executed before the error handling interceptor so if there is any failure doing commit, the error
 * handler gets executed.
 */
class CommitTransactionInterceptor implements ExecutionInterceptor<Event> {

  private final ExecutionInterceptor<Event> nextInterceptor;

  public CommitTransactionInterceptor(ExecutionInterceptor<Event> nextInterceptor) {
    this.nextInterceptor = nextInterceptor;
  }

  @Override
  public Event execute(ExecutionCallback<Event> callback, ExecutionContext executionContext) throws Exception {
    Event result = nextInterceptor.execute(callback, executionContext);
    if (executionContext.needsTransactionResolution()) {
      try {
        TransactionCoordination.getInstance().resolveTransaction();
      } catch (Exception e) {
        // Null result only happens when there's a filter in the chain.
        // Unfortunately a filter causes the whole chain to return null
        // and there's no other way to retrieve the last event but using the RequestContext.
        // see https://www.mulesoft.org/jira/browse/MULE-8670
        if (result == null) {
          result = getCurrentEvent();
        }
        throw new MessagingException(result, e);
      }
    }
    return result;
  }
}
