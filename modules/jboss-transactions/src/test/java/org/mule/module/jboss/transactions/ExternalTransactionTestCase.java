package org.mule.module.jboss.transactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.XaTransactionFactory;

import javax.transaction.Transaction;

/*
* $Id
* --------------------------------------------------------------------------------------
* Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
*
* The software in this package is published under the terms of the CPAL v1.0
* license, a copy of which has been included with this distribution in the
* LICENSE.txt file.
*/

/** Test transaction behavior when "joinExternal" is set to allow joining external transactions
 * There is one test per legal transactional behavior (e.g. ALWAYS_BEGIN).
 */
public class ExternalTransactionTestCase extends AbstractExternalTransactionTestCase
{
    protected static final Log logger = LogFactory.getLog(ExternalTransactionTestCase.class);

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/external-transaction-config.xml";
    }

    public void testBeginOrJoinTransaction() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_BEGIN_OR_JOIN);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertSame(tx, muleTx);
                resource1.setValue(14);
                return "OK";
            }
        });

        // Not committed yet, since Mule joined the external transaction
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(0, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());

        // now try with no active transaction
        result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                muleTx.enlistResource(resource1);
                resource1.setValue(15);
                return "OK";
            }
        });

        // Mule began and committed the transaction
        assertEquals(15, resource1.getPersistentValue());
    }

    public void testBeginTransaction() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_BEGIN);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNotSame(tx, muleTx);
                muleTx.enlistResource(resource1);
                resource1.setValue(14);
                return "OK";
            }
        });

        // Committed in Mule's transaction
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(14, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());

        result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNotSame(tx, muleTx);
                muleTx.enlistResource(resource1);
                resource1.setValue(15);
                return "OK";
            }
        });

        // Committed in Mule's transaction
        assertEquals("OK", result);
        assertEquals(15, resource1.getPersistentValue());
    }

    public void testNoTransactionProcessing() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_NONE);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        tx.enlistResource(resource1);
        resource1.setValue(14);
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNull(muleTx);
                return "OK";
            }
        });

        // transaction restored, no commit
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(0, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());

        result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNull(muleTx);
                return "OK";
            }
        });
    }

    public void testAlwaysJoinTransaction() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_JOIN);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertSame(tx, muleTx);
                resource1.setValue(14);
                return "OK";
            }
        });

        // Not committed yet, since Mule joined the external transaction
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(0, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());

        // try with no active transaction.. Should throw
        Exception ex = null;
        try
        {
            result = (String) tt.execute(new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    return "OK";
                }
            });
        }
        catch (Exception e)
        {
            ex = e;
            logger.debug("saw exception " + e.getMessage());
        }
        assertNotNull(ex);
    }

    public void testJoinTransactionIfPossible() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertSame(tx, muleTx);
                resource1.setValue(14);
                return "OK";
            }
        });

        // Not committed yet, since Mule joined the external transaction
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(0, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());

        // try with no active transaction.. Should run with none
        result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNull(muleTx);
                return "OK";
            }
        });
        assertEquals("OK", result);
    }

    public void testNoTransactionAllowed() throws Exception
    {
        init(true);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_NEVER);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        // This will throw since no transaction is allowed
        Exception ex = null;
        try
        {
            String result = (String) tt.execute(new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    return "OK";
                }
            });
        }
        catch (Exception e)
        {
            ex = e;
            logger.debug("saw exception " + e.getMessage());
        }
        assertNotNull(ex);
        tm.rollback();
    }
}