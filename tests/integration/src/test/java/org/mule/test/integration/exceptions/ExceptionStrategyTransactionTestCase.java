/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleEvent;
import org.mule.api.transaction.TransactionException;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transaction.TransactionCoordination;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * When exception strategies are used with transactions it should be possible to send
 * the exception message while rolling back the transaction. See MULE-4338
 */
public class ExceptionStrategyTransactionTestCase extends AbstractServiceAndFlowTestCase
{
    private static String failure;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exception-strategy-transaction-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exception-strategy-transaction-test-flow.xml"}
        });
    }

    public ExceptionStrategyTransactionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testRequestReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("InputQueueClient", "payload", null);

        // There should be a message on ExceptionQueue
        assertNotNull(client.request("ExceptionQueue", 10000));

        if (failure != null)
        {
            fail(failure);
        }
    }

    @Test
    public void testNoInfiniteLoop() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("InputQueueClient2", "payload", null);

        Thread.sleep(500);

        if (failure != null)
        {
            fail(failure);
        }

    }

    public static class AssertRollbackServiceExceptionStrategy extends DefaultMessagingExceptionStrategy
    {
        private int visits = 0;

        @Override
        protected void routeException(MuleEvent event, Throwable t)
        {
            super.routeException(event, t);

            if (visits++ > 1)
            {
                failure = "Exception strategy should only be called once";
                fail("Exception strategy should only be called once");
            }

            try
            {
                if (TransactionCoordination.getInstance().getTransaction() != null &&
                    !TransactionCoordination.getInstance().getTransaction().isRollbackOnly())
                {
                    failure = "transaction should have been set for rollback";
                }
            }
            catch (TransactionException e)
            {
                failure = e.getMessage();
                fail(e.getMessage());
            }
        }
    }

}
