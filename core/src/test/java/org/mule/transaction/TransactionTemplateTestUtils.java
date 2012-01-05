/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.process.ProcessingCallback;

public class TransactionTemplateTestUtils
{
    public static ProcessingCallback getEmptyTransactionCallback(final MuleEvent returnObject)
    {
        return new ProcessingCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                return returnObject;
            }
        };
    }

    public static ProcessingCallback<MuleEvent> getRollbackTransactionCallback(final MuleEvent returnObject)
    {
        return new ProcessingCallback() {
            @Override
            public MuleEvent process() throws Exception
            {
                TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
                return returnObject;
            }
        };
    }

    public static ProcessingCallback<MuleEvent> getFailureTransactionCallback() throws Exception
    {
        return new ProcessingCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                throw Mockito.mock(MessagingException.class, Answers.RETURNS_MOCKS.get());
            }
        };
    }

    public static ProcessingCallback<MuleEvent> getFailureTransactionCallback(final MessagingException mockMessagingException) throws Exception
    {
        return new ProcessingCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception
            {
                throw mockMessagingException;
            }
        };
    }

    public static ProcessingCallback<MuleEvent> getFailureTransactionCallbackStartsTransaction(final MessagingException mockMessagingException, final Transaction mockTransaction)
    {
        return new ProcessingCallback<MuleEvent>() {

            @Override
            public MuleEvent process() throws Exception
            {
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                throw mockMessagingException;
            }
        };
    }

}
