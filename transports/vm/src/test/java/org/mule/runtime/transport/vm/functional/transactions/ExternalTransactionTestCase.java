/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.transaction.IllegalTransactionStateException;
import org.mule.runtime.core.util.ExceptionUtils;

import javax.transaction.Transaction;

import org.junit.Test;

/** Test transaction behavior when "joinExternal" is set to allow joining external transactions
 * There is one test per legal transactional behavior (e.g. ALWAYS_BEGIN).
 */
public class ExternalTransactionTestCase extends AbstractExternalTransactionTestCase
{

    public static final long WAIT = 3000L;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/external-transaction-config-flow.xml";
    }

    @Test
    public void testBeginOrJoinTransaction() throws Exception
    {
        init();
        ExecutionTemplate<String> executionTemplate = createExecutionTemplate(TransactionConfig.ACTION_BEGIN_OR_JOIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        ExecutionTemplate<String> tt = createExecutionTemplate(TransactionConfig.ACTION_ALWAYS_BEGIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        String result = tt.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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

        result = tt.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        ExecutionTemplate<String> executionTemplate = createExecutionTemplate(TransactionConfig.ACTION_NONE, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);

        assertNotNull(tx);
        tx.enlistResource(resource1);
        resource1.setValue(14);
        String result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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

        result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        ExecutionTemplate<String> executionTemplate = createExecutionTemplate(TransactionConfig.ACTION_ALWAYS_JOIN, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        try
        {
            result = executionTemplate.execute(new ExecutionCallback<String>()
            {
                @Override
                public String process() throws Exception
                {
                    return "OK";
                }
            });
            fail("No exception seen");
        }
        catch (Exception e)
        {
            logger.debug("saw exception " + e.getMessage());
        }
    }

    @Test
    public void testJoinTransactionIfPossible() throws Exception
    {
        init();
        ExecutionTemplate<String> executionTemplate = createExecutionTemplate(TransactionConfig.ACTION_JOIN_IF_POSSIBLE, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);
        String result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        result = executionTemplate.execute(new ExecutionCallback<String>()
        {
            @Override
            public String process() throws Exception
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
        ExecutionTemplate<String> executionTemplate = createExecutionTemplate(TransactionConfig.ACTION_NEVER, true);

        tm.begin();
        final Transaction tx = tm.getTransaction();
        final TestResource resource1 = new TestResource(tm);
        tx.enlistResource(resource1);
        assertNotNull(tx);

        // This will throw since no transaction is allowed
        try
        {
            executionTemplate.execute(new ExecutionCallback<String>()
            {
                @Override
                public String process() throws Exception
                {
                    return "OK";
                }
            });
            fail("No exception seen");
        }
        catch (Exception e)
        {
            logger.debug("saw exception " + e.getMessage());
        }
        tm.rollback();
    }

    /** Check that the configuration specifies considers external transactions */
    @Test
    public void testConfiguration() throws Exception
    {
        tm = muleContext.getTransactionManager();

        tm.begin();
        MuleClient client = muleContext.getClient();
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
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof IllegalTransactionStateException);
        }
    }
}
