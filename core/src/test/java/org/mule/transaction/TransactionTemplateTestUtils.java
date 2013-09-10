/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.Transaction;

public class TransactionTemplateTestUtils
{
    public static ExecutionCallback getEmptyTransactionCallback(final MuleEvent returnObject)
    {
        return new ExecutionCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                return returnObject;
            }
        };
    }

    public static ExecutionCallback<MuleEvent> getRollbackTransactionCallback(final MuleEvent returnObject)
    {
        return new ExecutionCallback() {
            @Override
            public MuleEvent process() throws Exception
            {
                TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
                return returnObject;
            }
        };
    }

    public static ExecutionCallback<MuleEvent> getFailureTransactionCallback() throws Exception
    {
        return new ExecutionCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                throw Mockito.mock(MessagingException.class, Answers.RETURNS_MOCKS.get());
            }
        };
    }

    public static ExecutionCallback<MuleEvent> getFailureTransactionCallback(final MessagingException mockMessagingException) throws Exception
    {
        return new ExecutionCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                throw mockMessagingException;
            }
        };
    }

    public static ExecutionCallback<MuleEvent> getFailureTransactionCallbackStartsTransaction(final MessagingException mockMessagingException, final Transaction mockTransaction)
    {
        return new ExecutionCallback<MuleEvent>() {

            @Override
            public MuleEvent process() throws Exception
            {
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                throw mockMessagingException;
            }
        };
    }

}
