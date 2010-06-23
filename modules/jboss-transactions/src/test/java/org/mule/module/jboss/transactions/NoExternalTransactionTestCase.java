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

/** Test tarnsaction behavior when "joinExternal" is set to disallow joining external transactions
 * There is one test per legal transactional behavior (e.g. ALWAYS_BEGIN).
 */
public class NoExternalTransactionTestCase extends AbstractExternalTransactionTestCase
{
    protected static final Log logger = LogFactory.getLog(NoExternalTransactionTestCase.class);

    @Override
    protected void doTearDown() throws Exception
    {
        try
        {
            if (tm != null && tm.getTransaction() != null)
                tm.rollback();
        }
        catch (Exception ex)
        {
            logger.debug(ex);
        }
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/no-external-transaction-config.xml";
    }

    public void testBeginOrJoinTransaction() throws Exception
    {
        init(false);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_BEGIN_OR_JOIN);

        logger.debug("TM is a " + tm.getClass().toString());
        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        String result;
        Exception ex = null;

        // This will throw, becasue nested transactions are not supported
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
        tm.rollback();

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
        init(false);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_BEGIN);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        String result;
        Exception ex = null;

        // This will throw, because nested transactions are not supported
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
        tm.rollback();

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

    public void testNoTransactionProcessing() throws Exception
    {
        init(false);
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
                assertNotNull(muleTx);
                return "OK";
            }
        });

        // transaction ignored, no commit
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
        init(false);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_JOIN);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        Exception ex = null;
        String result;
        try
        {
            // Thjis will throw, because Mule sees no transaction to join
            result = (String) tt.execute(new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    Transaction muleTx = tm.getTransaction();
                    assertSame(tx, muleTx);
                    resource1.setValue(14);
                    return "OK";
                }
            });
        }
        catch (Exception e)
        {
            ex = e;
            logger.debug("saw exception " + e.getMessage());
        }

        // Not committed yet, since Mule joined the external transaction
        assertNotNull(ex);
        tm.rollback();

        // try with no active transaction.. Should still throw
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
        init(false);
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

        // Not committed yet, since Mule saw no transaction to join
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
        init(false);
        TransactionTemplate tt = createTransactionTemplate(TransactionConfig.ACTION_NEVER);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        // This will not throw since Mule sees no transaction
        String result = (String) tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                resource1.setValue(14);
                return "OK";
            }
        });

        // Not committed yet, since Mule saw no transaction to join
        assertEquals("OK", result);
        assertEquals(14, resource1.getValue());
        assertEquals(0, resource1.getPersistentValue());
        tm.commit();

        // Now it's committed
        assertEquals(14, resource1.getPersistentValue());
    }

}