/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;
import org.mule.util.xa.ResourceManagerException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class XAResourceManagerTestCase extends AbstractMuleContextTestCase
{
    private TransactionManager tm;

    @Override
    protected void doSetUp() throws Exception
    {
        tm = new JBossArjunaTransactionManagerFactory().create(muleContext.getConfiguration());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        tm = null;
    }

    @Test
    public void testTxBehaviour() throws Exception
    {
        TestXAResourceManager rm = new TestXAResourceManager();
        rm.start();
        DefaultXASession s = rm.createSession();

        tm.begin();
        Transaction tx = tm.getTransaction();
        tx.enlistResource(s);

        tx.delistResource(s, XAResource.TMSUCCESS);
        tx.commit();
    }

    protected static class TestXAResourceManager extends AbstractXAResourceManager
    {
        private static Log logger = LogFactory.getLog(TestXAResourceManager.class);

        public DefaultXASession createSession()
        {
            return new DefaultXASession(this);
        }

        protected Log getLogger()
        {
            return logger;
        }

        @Override
        protected AbstractTransactionContext createTransactionContext(Object session)
        {
            return new AbstractTransactionContext()
            {
                @Override
                public void doCommit()
                {
                    // does nothing
                }

                @Override
                public void doRollback()
                {
                     // does nothing
                }
            };
        }

        @Override
        protected void doBegin(AbstractTransactionContext context)
        {
            // template method
        }

        @Override
        protected int doPrepare(AbstractTransactionContext context)
        {
            // template method
            return 0;
        }

        @Override
        protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }

        @Override
        protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }
    }
}
