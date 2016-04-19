/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional.transactions;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transaction.XaTransactionFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractExternalTransactionTestCase extends FunctionalTestCase
{

    protected static final Log logger = LogFactory.getLog(AbstractExternalTransactionTestCase.class);

    protected MuleContext context;
    protected TransactionManager tm;

    @Override
    protected void doSetUp() throws Exception
    {
        cleanupTransactionState();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        cleanupTransactionState();
    }

    private void cleanupTransactionState()
    {
        try
        {
            if (tm != null && tm.getTransaction() != null)
            {
                tm.rollback();
            }
        }
        catch (Exception ex)
        {
            logger.debug(ex);
        }
        try
        {
            org.mule.runtime.core.api.transaction.Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (tx != null)
            {
                TransactionCoordination.getInstance().unbindTransaction(tx);
            }
        }
        catch (TransactionException ex)
        {
            logger.debug(ex);
        }
    }

    protected void init() throws Exception
    {
        context = createMuleContext();
        tm = context.getTransactionManager();
    }

    protected <T> ExecutionTemplate<T> createExecutionTemplate(byte action, boolean considerExternal)
    {
        TransactionConfig tc = new MuleTransactionConfig(action);
        tc.setFactory(new XaTransactionFactory());
        tc.setInteractWithExternal(considerExternal);
        ExecutionTemplate<T> executionTemplate = TransactionalExecutionTemplate.createTransactionalExecutionTemplate(context, tc);
        return executionTemplate;
    }

    /** An XA resource that allows setting, committing, and rolling back the value of one resource */
    public static class TestResource implements XAResource
    {
        private Map<Transaction, Integer> transientValue = new HashMap<Transaction, Integer>();
        private int persistentValue;
        private TransactionManager tm;

        public TestResource(TransactionManager tm)
        {
            this.tm = tm;
        }

        public void setValue(int val)
        {
            Transaction tx = getCurrentTransaction();
            transientValue.put(tx, val);
        }

        private Transaction getCurrentTransaction()
        {
            Transaction tx = null;
            Exception ex = null;
            try
            {
                tx = tm.getTransaction();
            }
            catch (SystemException e)
            {
                tx = null;
                ex = e;
            }
            if (tx == null)
            {
                throw new IllegalStateException("Unable to access resource value outside transaction", ex);
            }
            return tx;
        }

        public int getPersistentValue()
        {
            return persistentValue;
        }

        public int getValue()
        {
            Transaction tx = null;
            try
            {
                tx = getCurrentTransaction();
            }
            catch (Exception ex)
            {
                // return persistent value
            }
            Integer val = transientValue.get(tx);
            return val == null ? persistentValue : val;
        }

        @Override
        public void commit(Xid id, boolean onePhase) throws XAException
        {
            logger.debug("XA_COMMIT[" + id + "]");
            dumpStackTrace();
            Transaction tx = getCurrentTransaction();
            persistentValue = transientValue.get(tx);
        }

        @Override
        public void end(Xid xid, int flags) throws XAException
        {
            logger.debug("XA_END[" + xid + "] Flags=" + flags);
            dumpStackTrace();
        }

        @Override
        public void forget(Xid xid) throws XAException
        {
            logger.debug("XA_FORGET[" + xid + "]");
            dumpStackTrace();
        }

        @Override
        public int getTransactionTimeout() throws XAException
        {
            return (_timeout);
        }

        @Override
        public boolean isSameRM(XAResource xares) throws XAException
        {
            return (xares.equals(this));
        }

        @Override
        public int prepare(Xid xid) throws XAException
        {
            logger.debug("XA_PREPARE[" + xid + "]");
            dumpStackTrace();

            return (XA_OK);
        }

        @Override
        public Xid[] recover(int flag) throws XAException
        {
            logger.debug("RECOVER[" + flag + "]");
            dumpStackTrace();
            return (null);
        }

        @Override
        public void rollback(Xid xid) throws XAException
        {
            logger.debug("XA_ROLLBACK[" + xid + "]");
            dumpStackTrace();

            Transaction tx = getCurrentTransaction();
            transientValue.remove(tx);
        }

        @Override
        public boolean setTransactionTimeout(int seconds) throws XAException
        {
            _timeout = seconds;
            return (true);
        }

        @Override
        public void start(Xid xid, int flags) throws XAException
        {
            logger.debug("XA_START[" + xid + "] Flags=" + flags);
            dumpStackTrace();
        }

        protected int _timeout = 0;

        private void dumpStackTrace()
        {
            if (logger.isDebugEnabled() && false)
            {
                final StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                new Exception().printStackTrace(pw);
                pw.flush();
                logger.debug(sw.toString());
            }
        }
    }
}
