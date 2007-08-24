/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;
import org.mule.util.xa.ResourceManagerException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.jotm.Jotm;

public class XAResourceManagerTestCase extends AbstractMuleTestCase
{

    private Jotm jotm;
    private TransactionManager tm;

    protected void doSetUp() throws Exception
    {
        jotm = new Jotm(true, false);
        tm = jotm.getTransactionManager();
    }

    protected void doTearDown() throws Exception
    {
        jotm.stop();
        jotm = null;
        tm = null;
    }

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

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
         */
        protected Log getLogger()
        {
            return logger;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext(java.lang.Object)
         */
        protected AbstractTransactionContext createTransactionContext(Object session)
        {
            return new AbstractTransactionContext();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
         */
        protected void doBegin(AbstractTransactionContext context)
        {
            // template method
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
         */
        protected int doPrepare(AbstractTransactionContext context)
        {
            // template method
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
         */
        protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
         */
        protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
        {
            // template method
        }

    }
}
