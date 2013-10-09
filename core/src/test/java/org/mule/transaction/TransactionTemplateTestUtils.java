/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
