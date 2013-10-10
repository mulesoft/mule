/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.XaTransaction;
import org.mule.transaction.XaTransactionFactory;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validate certain expectations when working with JTA API. It is called to catch discrepancies in TM implementations
 * and alert early. Subclasses are supposed to plug in specific transaction managers for tests.
 */
public abstract class AbstractTxThreadAssociationTestCase extends AbstractMuleContextTestCase
{
    /* To allow access from the dead TX threads we spawn. */
    private TransactionManager tm;
    protected static final int TRANSACTION_TIMEOUT_SECONDS = 3;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        TransactionManagerFactory factory = getTransactionManagerFactory();
        tm = factory.create(muleContext.getConfiguration());
        assertNotNull("Transaction Manager should be available.", tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());
    }

    @Test
    public void testTxHandleCommitKeepsThreadAssociation() throws Exception
    {
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);
        tm.begin();

        Transaction tx = tm.getTransaction();
        assertNotNull("Transaction should have started.", tx);
        assertEquals("TX should have been active", Status.STATUS_ACTIVE, tx.getStatus());

        tx.commit();

        tx = tm.getTransaction();
        assertNotNull("Committing via TX handle should NOT disassociated TX from the current thread.",
                      tx);
        assertEquals("TX status should have been COMMITTED.", Status.STATUS_COMMITTED, tx.getStatus());

        // Remove the TX-thread association. The only public API to achieve it is suspend(),
        // technically we never resume the same transaction (TX forget).
        Transaction suspended = tm.suspend();
        assertTrue("Wrong TX suspended?.", suspended.equals(tx));
        assertNull("TX should've been disassociated from the thread.", tm.getTransaction());

        // should be no-op and never fail
        tm.resume(null);

        // ensure we don't have any TX-Thread association lurking around a main thread
        assertNull(tm.getTransaction());
    }

    @Test
    public void testTxManagerCommitDissassociatesThread() throws Exception
    {
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);
        tm.begin();

        Transaction tx = tm.getTransaction();
        assertNotNull("Transaction should have started.", tx);
        assertEquals("TX should have been active", Status.STATUS_ACTIVE, tx.getStatus());

        tm.commit();

        assertNull("Committing via TX Manager should have disassociated TX from the current thread.",
                   tm.getTransaction());
    }

    @Test
    public void testTxManagerRollbackDissassociatesThread() throws Exception
    {
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);
        tm.begin();

        Transaction tx = tm.getTransaction();
        assertNotNull("Transaction should have started.", tx);
        assertEquals("TX should have been active", Status.STATUS_ACTIVE, tx.getStatus());

        tm.rollback();

        assertNull("Committing via TX Manager should have disassociated TX from the current thread.",
                   tm.getTransaction());
    }

    /**
     * AlwaysBegin action suspends current transaction and begins a new one.
     *
     * @throws Exception if any error
     */
    @Test
    public void testAlwaysBeginXaTransactionSuspendResume() throws Exception
    {
        muleContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());

        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        // this is one component with a TX always begin
        TransactionConfig config = new MuleTransactionConfig();
        config.setFactory(new XaTransactionFactory());
        config.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate template = new TransactionTemplate(config, muleContext);

        // and the callee component which should begin new transaction, current must be suspended
        final TransactionConfig nestedConfig = new MuleTransactionConfig();
        nestedConfig.setFactory(new XaTransactionFactory());
        nestedConfig.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);

        // start the call chain
        template.execute(new TransactionCallback<Void>()
        {
            public Void doInTransaction() throws Exception
            {
                // the callee executes within its own TX template, but uses the same global XA transaction,
                // bound to the current thread of execution via a ThreadLocal
                TransactionTemplate<Void> nestedTemplate =
                        new TransactionTemplate<Void>(nestedConfig, muleContext);
                final Transaction firstTx = tm.getTransaction();
                assertNotNull(firstTx);
                assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                return nestedTemplate.execute(new TransactionCallback<Void>()
                {
                    public Void doInTransaction() throws Exception
                    {
                        Transaction secondTx = tm.getTransaction();
                        assertNotNull(secondTx);
                        assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                        assertEquals(secondTx.getStatus(), Status.STATUS_ACTIVE);
                        try
                        {
                            tm.resume(firstTx);
                            fail("Second transaction must be active");
                        }
                        catch (java.lang.IllegalStateException e)
                        {
                            // expected

                            //Thrown if the thread is already associated with another transaction.
                            //Second tx is associated with the current thread
                        }
                        try
                        {
                            Transaction currentTx = tm.suspend();
                            assertTrue(currentTx.equals(secondTx));
                            tm.resume(firstTx);
                            assertEquals(firstTx, tm.getTransaction());
                            assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                            assertEquals(secondTx.getStatus(), Status.STATUS_ACTIVE);
                            Transaction a = tm.suspend();
                            assertTrue(a.equals(firstTx));
                            tm.resume(secondTx);
                        }
                        catch (Exception e)
                        {
                            fail("Error: " + e);
                        }

                        // do not care about the return really
                        return null;
                    }
                });
            }
        });
        assertNull("Committing via TX Manager should have disassociated TX from the current thread.",
                   tm.getTransaction());
    }

    /**
     * NONE action suspends current transaction and begins a new one.
     *
     * @throws Exception if any error
     */
    @Test
    public void testNoneXaTransactionSuspendResume() throws Exception
    {
        muleContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());

        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        // this is one component with a TX always begin
        TransactionConfig config = new MuleTransactionConfig();
        config.setFactory(new XaTransactionFactory());
        config.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate<Void> template = new TransactionTemplate<Void>(config, muleContext);

        // and the callee component which should begin new transaction, current must be suspended
        final TransactionConfig nestedConfig = new MuleTransactionConfig();
        nestedConfig.setFactory(new XaTransactionFactory());
        nestedConfig.setAction(TransactionConfig.ACTION_NONE);

        // start the call chain
        template.execute(new TransactionCallback<Void>()
        {
            public Void doInTransaction() throws Exception
            {
                // the callee executes within its own TX template, but uses the same global XA transaction,
                // bound to the current thread of execution via a ThreadLocal
                TransactionTemplate<Void> nestedTemplate =
                        new TransactionTemplate<Void>(nestedConfig, muleContext);
                final Transaction firstTx = tm.getTransaction();
                assertNotNull(firstTx);
                assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                return nestedTemplate.execute(new TransactionCallback<Void>()
                {
                    public Void doInTransaction() throws Exception
                    {
                        Transaction secondTx = tm.getTransaction();
                        assertNull(secondTx);
                        assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                        try
                        {
                            tm.resume(firstTx);
                            assertEquals(firstTx, tm.getTransaction());
                            assertEquals(firstTx.getStatus(), Status.STATUS_ACTIVE);
                            Transaction a = tm.suspend();
                            assertTrue(a.equals(firstTx));
                        }
                        catch (Exception e)
                        {
                            fail("Error: " + e);
                        }

                        // do not care about the return really
                        return null;
                    }
                });
            }
        });
        assertNull("Committing via TX Manager should have disassociated TX from the current thread.",
                   tm.getTransaction());
    }
    
    /**
     * This is a former XaTransactionTestCase.
     *
     * @throws Exception in case of any error
     */
    @Test
    public void testXaTransactionTermination() throws Exception
    {
        muleContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());

        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        XaTransaction muleTx = new XaTransaction(muleContext);
        assertFalse(muleTx.isBegun());
        assertEquals(Status.STATUS_NO_TRANSACTION, muleTx.getStatus());
        muleTx.begin();

        assertTrue(muleTx.isBegun());

        muleTx.commit();

        Transaction jtaTx = tm.getTransaction();
        assertNull("Committing via TX Manager should have disassociated TX from the current thread.", jtaTx);
        assertEquals(Status.STATUS_NO_TRANSACTION, muleTx.getStatus());
    }

    /**
     * This is a former TransactionTemplateTestCase.
     * http://mule.mulesoft.org/jira/browse/MULE-1494
     *
     * @throws Exception in case of any error
     */
    @Test
    public void testNoNestedTxStarted() throws Exception
    {
        muleContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());

        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        // this is one service with a TX always begin
        TransactionConfig config = new MuleTransactionConfig();
        config.setFactory(new XaTransactionFactory());
        config.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate template = new TransactionTemplate(config, muleContext);

        // and the callee service which should join the current XA transaction, not begin a nested one
        final TransactionConfig nestedConfig = new MuleTransactionConfig();
        nestedConfig.setFactory(new XaTransactionFactory());
        nestedConfig.setAction(TransactionConfig.ACTION_BEGIN_OR_JOIN);

        // start the call chain
        template.execute(new TransactionCallback<Void>()
        {
            public Void doInTransaction() throws Exception
            {
                // the callee executes within its own TX template, but uses the same global XA transaction,
                // bound to the current thread of execution via a ThreadLocal
                TransactionTemplate<Void> nestedTemplate =
                        new TransactionTemplate<Void>(nestedConfig, muleContext);
                return nestedTemplate.execute(new TransactionCallback<Void>()
                {
                    public Void doInTransaction() throws Exception
                    {
                        // do not care about the return really
                        return null;
                    }
                });
            }
        });
    }


    protected TransactionManager getTransactionManager()
    {
        return tm;
    }

    protected abstract TransactionManagerFactory getTransactionManagerFactory();

}
