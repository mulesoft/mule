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
import org.mule.api.transaction.TransactionCallback;

public class TransactionTemplateTestUtils
{
    public static TransactionCallback getEmptyTransactionCallback(final Object returnObject)
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                return returnObject;
            }
        };
    }

    public static TransactionCallback getRollbackTransactionCallback(final Object returnObject)
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
                return returnObject;
            }
        };
    }

    public static TransactionCallback getFailureTransactionCallback() throws Exception
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                throw Mockito.mock(MessagingException.class, Answers.RETURNS_MOCKS.get());
            }
        };
    }

    public static TransactionCallback getFailureTransactionCallback(final MessagingException mockMessagingException) throws Exception
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                throw mockMessagingException;
            }
        };

    }
}
