/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleEvent;
import org.mule.api.client.MuleClient;
import org.mule.api.transaction.TransactionException;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transaction.TransactionCoordination;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

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
        MuleClient client = muleContext.getClient();
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
        MuleClient client = muleContext.getClient();
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
