/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.tck.AbstractMuleTestCase;

import org.mockito.Mockito;

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    private TransactionCoordination tc;

    @Override
    protected void doSetUp() throws Exception
    {
        tc = TransactionCoordination.getInstance();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        tc.unbindTransaction(tc.getTransaction());
    }

    public void testBindTransaction() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());
        tc.unbindTransaction(tx);
    }

    public void testBindTransactionWithAlreadyBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());

        try
        {
            Transaction tx2 = Mockito.mock(Transaction.class);
            tc.bindTransaction(tx2);
            fail();
        }
        catch (IllegalTransactionStateException e)
        {
            // expected
        }

        tc.unbindTransaction(tx);
    }

    public void testUnbindTransactionWithoutBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.unbindTransaction(tx);
    }

    public void testSetInstanceWithBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.bindTransaction(tx);

        tc.unbindTransaction(tx);
    }
}
