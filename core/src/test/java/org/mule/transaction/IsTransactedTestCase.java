/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.mule.tck.testmodels.mule.TestTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IsTransactedTestCase extends AbstractMuleTestCase
{
    @Test
    public void testIsTransacted() throws Exception
    {
        MuleTransactionConfig cfg = new MuleTransactionConfig();
        TestTransaction testTx = new TestTransaction(null);

        cfg.setAction(TransactionConfig.ACTION_NEVER);
        assertFalse(cfg.isTransacted());

        cfg.setAction(TransactionConfig.ACTION_NEVER);
        assertFalse(cfg.isTransacted());

        cfg.setFactory(new TransactedFactory());
        cfg.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        assertTrue(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_ALWAYS_JOIN);
        assertTrue(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        assertTrue(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().bindTransaction(testTx);
        assertTrue(cfg.isTransacted());
        TransactionCoordination.getInstance().unbindTransaction(testTx);
        cfg.setAction(TransactionConfig.ACTION_INDIFFERENT);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().bindTransaction(testTx);
        assertTrue(cfg.isTransacted());
        TransactionCoordination.getInstance().unbindTransaction(testTx);

        cfg.setFactory(new NonTransactedFactory());
        cfg.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        assertFalse(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_ALWAYS_JOIN);
        assertFalse(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        assertFalse(cfg.isTransacted());
        cfg.setAction(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().bindTransaction(testTx);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().unbindTransaction(testTx);
        cfg.setAction(TransactionConfig.ACTION_INDIFFERENT);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().bindTransaction(testTx);
        assertFalse(cfg.isTransacted());
        TransactionCoordination.getInstance().unbindTransaction(testTx);
    }

    @Test(expected = MuleRuntimeException.class)
    public void testExpectException1()
    {
        MuleTransactionConfig cfg = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        cfg.isTransacted();
    }

    @Test(expected = MuleRuntimeException.class)
    public void testExpectException2()
    {
        MuleTransactionConfig cfg = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        cfg.isTransacted();
    }

    public static class TransactedFactory implements TransactionFactory
    {
        @Override
        public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
        {
            return null;
        }

        @Override
        public boolean isTransacted()
        {
            return true;
        }
    }

    public static class NonTransactedFactory implements TransactionFactory
    {
        @Override
        public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
        {
            return null;
        }

        @Override
        public boolean isTransacted()
        {
            return false;
        }
    }

}
