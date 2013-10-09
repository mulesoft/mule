/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.DefaultMuleException;
import org.mule.api.transaction.Transaction;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import java.io.FileNotFoundException;

import org.junit.Test;

public class ExceptionRollbackTestCase extends AbstractMuleContextTestCase
{
    private DefaultSystemExceptionStrategy strategy;
    private Transaction tx;

    @Override
    protected void doSetUp() throws Exception
    {
        strategy = new DefaultSystemExceptionStrategy(muleContext);
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

    @Test
    public void testCommit() throws Exception
    {
        strategy.handleException(new FileNotFoundException());
        assertFalse(tx.isRolledBack());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        //assertTrue(tx.isCommitted());
    }

    @Test
    public void testRollback() throws Exception
    {
        strategy.handleException(new DefaultMuleException(CoreMessages.agentsRunning()));
        assertTrue(tx.isRolledBack());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        assertFalse(tx.isCommitted());
    }

    @Test
    public void testRollbackByDefault() throws Exception
    {
        strategy.handleException(new IllegalAccessException());
        assertTrue(tx.isRolledBack());
        //There is nothing to actually commit the transaction since we are not running in a real tx
        assertFalse(tx.isCommitted());
    }
}
