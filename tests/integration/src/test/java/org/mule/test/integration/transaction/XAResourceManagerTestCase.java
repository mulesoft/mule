/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

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

    protected void doSetUp() throws Exception
    {
        tm = new JBossArjunaTransactionManagerFactory().create(muleContext.getConfiguration());
    }

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

        protected AbstractTransactionContext createTransactionContext(Object session)
        {
            return new AbstractTransactionContext() 
            {
                @Override
                public void doCommit()
                {
                }

                @Override
                public void doRollback()
                {
                }
            };
        }

        protected void doBegin(AbstractTransactionContext context)
        {
            // template method
        }

        protected int doPrepare(AbstractTransactionContext context)
        {
            // template method
            return 0;
        }

        protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }

        protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }
    }
}
