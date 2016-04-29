/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

public class JmsXAAlwaysBeginTestCase extends AbstractJmsFunctionalTestCase
{
    private static final List<Xid> committedTx = new CopyOnWriteArrayList<Xid>();
    private static final List<Xid> rolledbackTx = new CopyOnWriteArrayList<Xid>();
    protected static final Log logger = LogFactory.getLog(JmsXAAlwaysBeginTestCase.class);

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-xa-tx-ALWAYS_BEGIN.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        purge(getInboundQueueName());
        purge(getOutboundQueueName());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        purge(getInboundQueueName());
        purge(getOutboundQueueName());
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testAlwaysBeginTx() throws Exception
    {
        send(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNotReceive);
        assertEquals(committedTx.size(), 0);
        assertEquals(rolledbackTx.size(), 2);
    }

    @Ignore
    public static class TestRollbackComponent
    {

        public Object processObject(Object a) throws Exception
        {
            logger.debug("TestRollbackComponent " + a);
            TestResource res = new TestResource();
            Transaction currentTrans = muleContext.getTransactionManager().getTransaction();
            currentTrans.enlistResource(res);
            currentTrans.setRollbackOnly();
            return DEFAULT_OUTPUT_MESSAGE;
        }
    }

    @Ignore
    public static class TestResource implements XAResource
    {
        @Override
        public void commit(Xid id, boolean onePhase) throws XAException
        {
            committedTx.add(id);
            logger.debug("XA_COMMIT[" + id + "]");
        }

        @Override
        public void end(Xid xid, int flags) throws XAException
        {
            logger.debug("XA_END[" + xid + "] Flags=" + flags);
        }

        @Override
        public void forget(Xid xid) throws XAException
        {
            logger.debug("XA_FORGET[" + xid + "]");
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

            return (XA_OK);
        }

        @Override
        public Xid[] recover(int flag) throws XAException
        {
            logger.debug("RECOVER[" + flag + "]");
            return (null);
        }

        @Override
        public void rollback(Xid xid) throws XAException
        {
            rolledbackTx.add(xid);
            logger.debug("XA_ROLLBACK[" + xid + "]");
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
        }

        protected int _timeout = 0;
    }
}
