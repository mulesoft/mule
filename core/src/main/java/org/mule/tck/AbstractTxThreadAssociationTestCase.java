/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleTransactionConfig;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.XaTransaction;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Validate certain expectations when working with JTA API. It is called to catch discrepancies in TM implementations
 * and alert early. Subclasses are supposed to plug in specific transaction managers for tests.
 */
public abstract class AbstractTxThreadAssociationTestCase extends AbstractMuleTestCase
{
    /* To allow access from the dead TX threads we spawn. */
    private TransactionManager tm;
    protected static final int TRANSACTION_TIMEOUT_SECONDS = 3;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        UMOTransactionManagerFactory factory = getTransactionManagerFactory();
        tm = factory.create();
        assertNotNull("Transaction Manager should be available.", tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());
    }

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
        assertNotNull("Committing via TX handle should NOT disassociate TX from the current thread.",
                      tx);
        assertEquals("TX status should have been COMMITTED.", Status.STATUS_COMMITTED, tx.getStatus());

        // Remove the TX-thread association. The only public API to achieve it is suspend(),
        // technically we never resume the same transaction (TX forget).
        Transaction suspended = tm.suspend();
        assertTrue("Wron TX suspended?.", suspended.equals(tx));
        assertNull("TX should've been disassociated from the thread.", tm.getTransaction());

        // should be no-op and never fail
        tm.resume(null);

        // ensure we don't have any TX-Thread association lurking around a main thread
        assertNull(tm.getTransaction());
    }

    public void testTxManagerCommitDissassociatesThread() throws Exception
    {
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);
        tm.begin();

        Transaction tx = tm.getTransaction();
        assertNotNull("Transaction should have started.", tx);
        assertEquals("TX should have been active", Status.STATUS_ACTIVE, tx.getStatus());

        tm.commit();

        assertNull("Committing via TX Manager should have disassociate TX from the current thread.",
                   tm.getTransaction());
    }

    public void testTxManagerRollbackDissassociatesThread() throws Exception
    {
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);
        tm.begin();

        Transaction tx = tm.getTransaction();
        assertNotNull("Transaction should have started.", tx);
        assertEquals("TX should have been active", Status.STATUS_ACTIVE, tx.getStatus());

        tm.rollback();

        assertNull("Committing via TX Manager should have disassociate TX from the current thread.",
                   tm.getTransaction());
    }

    // TODO add tests for suspend/resume

    /**
     * This is a former XaTransactionTestCase.
     * @throws Exception in case of any error
     */
    public void testXaTransactionTermination() throws Exception
    {
        managementContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());
        
        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        XaTransaction muleTx = new XaTransaction(tm);
        assertFalse(muleTx.isBegun());
        assertEquals(Status.STATUS_NO_TRANSACTION, muleTx.getStatus());
        muleTx.begin();

        assertTrue(muleTx.isBegun());

        muleTx.commit();

        Transaction jtaTx = tm.getTransaction();
        assertNull("Committing via TX Manager should have disassociate TX from the current thread.", jtaTx);
        assertEquals(Status.STATUS_NO_TRANSACTION, muleTx.getStatus());
    }

    /**
     * This is a former TransactionTemplateTestCase.
     * http://mule.mulesource.org/jira/browse/MULE-1494
     * @throws Exception in case of any error
     */
    public void testNoNestedTxStarted() throws Exception
    {
        managementContext.setTransactionManager(tm);
        assertNull("There sould be no current transaction associated.", tm.getTransaction());

        // don't wait for ages, has to be set before TX is begun
        tm.setTransactionTimeout(TRANSACTION_TIMEOUT_SECONDS);

        // this is one component with a TX always begin
        UMOTransactionConfig config = new MuleTransactionConfig();
        config.setFactory(new XaTransactionFactory());
        config.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate template = new TransactionTemplate(config, new DefaultExceptionStrategy(), managementContext);

        // and the callee component which should join the current XA transaction, not begin a nested one
        final UMOTransactionConfig nestedConfig = new MuleTransactionConfig();
        nestedConfig.setFactory(new XaTransactionFactory());
        nestedConfig.setAction(UMOTransactionConfig.ACTION_BEGIN_OR_JOIN);

        // start the call chain
        template.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                // the callee executes within its own TX template, but uses the same global XA transaction,
                // bound to the current thread of execution via a ThreadLocal
                TransactionTemplate nestedTemplate =
                        new TransactionTemplate(nestedConfig, new DefaultExceptionStrategy(), managementContext);
                return nestedTemplate.execute(new TransactionCallback()
                {
                    public Object doInTransaction() throws Exception
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

    protected abstract UMOTransactionManagerFactory getTransactionManagerFactory();

}
