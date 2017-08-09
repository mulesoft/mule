/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.execution.ExecutionCallback;

import org.mockito.Answers;
import org.mockito.Mockito;

public class TransactionTemplateTestUtils {

  public static ExecutionCallback getEmptyTransactionCallback(final InternalEvent returnObject) {
    return new ExecutionCallback<InternalEvent>() {

      @Override
      public InternalEvent process() throws Exception {
        return returnObject;
      }
    };
  }

  public static ExecutionCallback<InternalEvent> getRollbackTransactionCallback(final InternalEvent returnObject) {
    return new ExecutionCallback() {

      @Override
      public InternalEvent process() throws Exception {
        TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
        return returnObject;
      }
    };
  }

  public static ExecutionCallback<InternalEvent> getFailureTransactionCallback() throws Exception {
    return new ExecutionCallback<InternalEvent>() {

      @Override
      public InternalEvent process() throws Exception {
        throw Mockito.mock(MessagingException.class, Answers.RETURNS_MOCKS.get());
      }
    };
  }

  public static ExecutionCallback<InternalEvent> getFailureTransactionCallback(final MessagingException mockMessagingException)
      throws Exception {
    return new ExecutionCallback<InternalEvent>() {

      @Override
      public InternalEvent process() throws Exception {
        throw mockMessagingException;
      }
    };
  }

  public static ExecutionCallback<InternalEvent> getFailureTransactionCallbackStartsTransaction(final MessagingException mockMessagingException,
                                                                                                final Transaction mockTransaction) {
    return new ExecutionCallback<InternalEvent>() {

      @Override
      public InternalEvent process() throws Exception {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        throw mockMessagingException;
      }
    };
  }

}
