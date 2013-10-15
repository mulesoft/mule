/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    private TransactionCoordination tc;

    @Before
    public void setUpTransaction() throws Exception
    {
        tc = TransactionCoordination.getInstance();
    }

    @After
    public void unbindTransaction() throws Exception
    {
        tc.unbindTransaction(tc.getTransaction());
    }

    @Test
    public void testBindTransaction() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());
        tc.unbindTransaction(tx);
    }

    @Test
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

    @Test
    public void testUnbindTransactionWithoutBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.unbindTransaction(tx);
    }

    @Test
    public void testSetInstanceWithBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Transaction tx = Mockito.mock(Transaction.class);

        tc.bindTransaction(tx);

        tc.unbindTransaction(tx);
    }
}
