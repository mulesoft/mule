/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.internal.exception.MessagingException;

public class TransactionTemplateTestUtils {

  public static ExecutionCallback<CoreEvent> getEmptyTransactionCallback(final CoreEvent returnObject) {
    return () -> returnObject;
  }

  public static ExecutionCallback<CoreEvent> getRollbackTransactionCallback(final CoreEvent returnObject) {
    return () -> {
      TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
      return returnObject;
    };
  }

  public static ExecutionCallback<CoreEvent> getCommitTransactionCallback(final CoreEvent returnObject) {
    return () -> {
      TransactionCoordination.getInstance().getTransaction().commit();
      return returnObject;
    };
  }

  public static ExecutionCallback<CoreEvent> getFailureTransactionCallback() throws Exception {
    return () -> {
      throw mock(MessagingException.class, RETURNS_MOCKS.get());
    };
  }

  public static ExecutionCallback<CoreEvent> getFailureTransactionCallback(final MessagingException mockMessagingException)
      throws Exception {
    return () -> {
      throw mockMessagingException;
    };
  }

  public static ExecutionCallback<CoreEvent> getFailureTransactionCallbackStartsTransaction(final MessagingException mockMessagingException,
                                                                                            final Transaction mockTransaction) {
    return () -> {
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
      throw mockMessagingException;
    };
  }

}
