/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionException;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transaction.TransactionCoordination;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * When exception strategies are used with transactions it should be possible to send
 * the exception message while rolling back the transaction. See MULE-4338
 */
public class ExceptionStrategyTransactionTestCase extends FunctionalTestCase
{

    private static String failure;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-transaction-test.xml";
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
        client.send("InputQueueClient2", "payload", null);

        Thread.sleep(500);
        
        if (failure != null)
        {
            fail(failure);
        }

    }

    static class AssertRollbackServiceExceptionStrategy extends DefaultServiceExceptionStrategy
    {
        private int visits = 0;

        public AssertRollbackServiceExceptionStrategy(MuleContext context)
        {
            super(context);
        }
        
        @Override
        protected void routeException(MuleEvent event, MessageProcessor target, Throwable t)
        {
            super.routeException(event, target, t);

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
