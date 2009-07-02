/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.DefaultMuleException;
import org.mule.api.transaction.Transaction;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;
import org.mule.service.DefaultServiceExceptionStrategy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import java.io.FileNotFoundException;

public class ExceptionRollbackTestCase extends AbstractMuleTestCase
{

    private DefaultServiceExceptionStrategy strategy;
    private Transaction tx;

    @Override
    protected void doSetUp() throws Exception
    {
        strategy = new DefaultServiceExceptionStrategy();
        strategy.setCommitTxFilter(new WildcardFilter("java.io.*"));
        strategy.setRollbackTxFilter(new WildcardFilter("org.mule.*, javax.*"));

        initialiseObject(strategy);
        tx = new TestTransaction(muleContext);
        TransactionCoordination.getInstance().bindTransaction(tx);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction(tx);
    }

    public void testCommit() throws Exception
    {
        strategy.exceptionThrown(new FileNotFoundException());
        assertFalse(tx.isRollbackOnly());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        //assertTrue(tx.isCommitted());
    }

    public void testRollback() throws Exception
    {
        strategy.exceptionThrown(new DefaultMuleException(CoreMessages.agentsRunning()));
        assertTrue(tx.isRollbackOnly());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        assertFalse(tx.isCommitted());
    }

    public void testRollbackByDefault() throws Exception
    {
        strategy.exceptionThrown(new IllegalAccessException());
        assertTrue(tx.isRollbackOnly());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        assertFalse(tx.isCommitted());
    }
}
