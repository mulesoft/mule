/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional.transactions;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.module.client.MuleClient;
import org.mule.transaction.TransactionTemplate;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/** Test transaction behavior when "joinExternal" is set to allow joining external transactions
 * There is one test per legal transactional behavior (e.g. ALWAYS_BEGIN).
 */
public class ExternalTransactionTestCase extends AbstractExternalTransactionTestCase
{
    
    public static final long WAIT = 3000L;

    protected static final Log logger = LogFactory.getLog(ExternalTransactionTestCase.class);

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/external-transaction-config.xml";
    }

    @Test
    public void testBeginOrJoinTransaction() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_BEGIN_OR_JOIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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
        result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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

    @Test
    public void testBeginTransaction() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_BEGIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        String result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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

        result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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

    @Test
    public void testNoTransactionProcessing() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_NONE, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        tx.enlistResource(resource1);
        resource1.setValue(14);
        String result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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

        result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNull(muleTx);
                return "OK";
            }
        });
    }

    @Test
    public void testAlwaysJoinTransaction() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_ALWAYS_JOIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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
            result = tt.execute(new TransactionCallback<String>()
            {
                public String doInTransaction() throws Exception
                {
                    return "OK";
                }
            });
            fail("No exception seen");
        }
        catch (Exception e)
        {
            ex = e;
            logger.debug("saw exception " + e.getMessage());
        }
    }

    @Test
    public void testJoinTransactionIfPossible() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_JOIN_IF_POSSIBLE, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
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
        result = tt.execute(new TransactionCallback<String>()
        {
            public String doInTransaction() throws Exception
            {
                Transaction muleTx = tm.getTransaction();
                assertNull(muleTx);
                return "OK";
            }
        });
        assertEquals("OK", result);
    }

    @Test
    public void testNoTransactionAllowed() throws Exception
    {
        init();
        TransactionTemplate<String> tt = createTransactionTemplate(TransactionConfig.ACTION_NEVER, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        // This will throw since no transaction is allowed
        Exception ex = null;
        try
        {
            tt.execute(new TransactionCallback<String>()
            {
                public String doInTransaction() throws Exception
                {
                    return "OK";
                }
            });
            fail("No exception seen");
        }
        catch (Exception e)
        {
            ex = e;
            logger.debug("saw exception " + e.getMessage());
        }
        tm.rollback();
    }

    /** Check that the configuration specifies considers external transactions */
    @Test
    public void testConfiguration() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        tm = client.getMuleContext().getTransactionManager();
        tm.begin();
        client.send("vm://entry?connector=vm-normal", "OK", null);
        tm.commit();
        MuleMessage response = client.request("queue2", WAIT);
        assertNull("Response is not null", response);

        // This will fail, since there will be no transaction to join
        try
        {
            client.send("vm://entry?connector=vm-normal", "OK", null);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            // expected
        }
    }
}
