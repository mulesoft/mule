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

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOTransaction;

import com.mockobjects.dynamic.Mock;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class XaTransactionTestCase extends AbstractMuleTestCase
{
    protected Mock mockTm = new Mock(TransactionManager.class);

    protected void doSetUp() throws Exception
    {
        TransactionManager tm = (TransactionManager)mockTm.proxy();
        managementContext.setTransactionManager(tm);
    }

    public void testBeginCommit() throws Exception
    {
        Mock mockTx = new Mock(Transaction.class);
        mockTm.expect("begin");
        mockTm.expectAndReturn("getTransaction", mockTx.proxy());

        XaTransaction tx = new XaTransaction(managementContext.getTransactionManager());
        assertFalse(tx.isBegun());
        assertEquals(UMOTransaction.STATUS_NO_TRANSACTION, tx.getStatus());
        tx.begin();

        mockTx.expectAndReturn("getStatus", UMOTransaction.STATUS_ACTIVE);
        assertTrue(tx.isBegun());

        mockTx.expectAndReturn("getStatus", UMOTransaction.STATUS_ACTIVE);
        mockTx.expect("commit");
        tx.commit();

        mockTx.expectAndReturn("getStatus", UMOTransaction.STATUS_COMMITTED);
        assertTrue(tx.isCommitted());
    }

}
