/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;

/**
 * Implements {@link DbTransactionManager} using {@link TransactionCoordination}
 */
public class TransactionCoordinationDbTransactionManager implements DbTransactionManager
{

    @Override
    public Transaction getTransaction()
    {
        return TransactionCoordination.getInstance().getTransaction();
    }
}
