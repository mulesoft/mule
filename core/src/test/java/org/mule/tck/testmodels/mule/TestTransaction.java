/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.AbstractSingleResourceTransaction;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

/**
 * A test transaction that does nothing on commit or rollback. The transaction does retain a status so that
 * developers can determine if the the transaction was rolled back or committed.
 */
public class TestTransaction extends AbstractSingleResourceTransaction
{
    private AtomicBoolean committed = new AtomicBoolean(false);
    private AtomicBoolean rolledBack = new AtomicBoolean(false);

    public TestTransaction(MuleContext muleContext)
    {
        super(muleContext);
    }

    /**
     * Really begin the transaction. Note that resources are enlisted yet.
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doBegin() throws TransactionException
    {
        //do nothing
    }

    /**
     * Commit the transaction on the underlying resource
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doCommit() throws TransactionException
    {
        committed.set(true);
    }

    /**
     * Rollback the transaction on the underlying resource
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doRollback() throws TransactionException
    {
        rolledBack.set(true);
    }

//    public boolean isCommitted()
//    {
//        return committed.get();
//    }
//
//    public boolean isRolledBack()
//    {
//        return rolledBack.get();
//    }
}
