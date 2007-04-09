/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOTransaction;

import com.mockobjects.dynamic.Mock;

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    volatile TransactionCoordination tc;

    protected void doSetUp() throws Exception
    {
        tc = TransactionCoordination.getInstance();
    }

    protected void doTearDown() throws Exception
    {
        tc.unbindTransaction(tc.getTransaction());
    }

    public void testBindTransaction() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction)mockTx.proxy();

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());
        tc.unbindTransaction(tx);
    }

    public void testBindTransactionWithAlreadyBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction)mockTx.proxy();

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());

        try
        {
            UMOTransaction tx2 = (UMOTransaction)new Mock(UMOTransaction.class, "trans").proxy();
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
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction)mockTx.proxy();
        tc.unbindTransaction(tx);
    }

    public void testSetInstanceWithBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction)mockTx.proxy();

        tc.bindTransaction(tx);

        tc.unbindTransaction(tx);
    }

}
