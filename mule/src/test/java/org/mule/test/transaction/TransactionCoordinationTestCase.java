/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transaction;

import com.mockobjects.dynamic.Mock;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOTransaction;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    TransactionCoordination tc;

    protected void doSetUp() throws Exception
    {
        tc = TransactionCoordination.getInstance();
    }

    protected void doTearDown() throws Exception
    {
        tc.unbindTransaction(tc.getTransaction());
        TransactionCoordination.setInstance(null);
    }

    public void testBindTransaction() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction) mockTx.proxy();

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());
        tc.unbindTransaction(tx);
    }

    public void testBindTransactionWithAlreadyBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction) mockTx.proxy();

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());

        try {
            UMOTransaction tx2 = (UMOTransaction) new Mock(UMOTransaction.class, "trans").proxy();
            tc.bindTransaction(tx2);
            fail();
        } catch (IllegalTransactionStateException e) {
            // expected
        }

        tc.unbindTransaction(tx);
    }

    public void testUnbindTransactionWithoutBound() throws Exception
    {
        assertNull(tc.getTransaction());
        //try {
            Mock mockTx = new Mock(UMOTransaction.class, "trans");
            UMOTransaction tx = (UMOTransaction) mockTx.proxy();
            tc.unbindTransaction(tx);
//            fail();
//        } catch (IllegalTransactionStateException e) {
//            // expected
//        }
    }

    public void testSetInstanceWithBound() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock mockTx = new Mock(UMOTransaction.class, "trans");
        UMOTransaction tx = (UMOTransaction) mockTx.proxy();

        tc.bindTransaction(tx);
        try {
            TransactionCoordination.setInstance(null);
            fail();
        } catch (IllegalStateException e) {
            // expected
        }

        tc.unbindTransaction(tx);
        TransactionCoordination.setInstance(null);
    }

}
