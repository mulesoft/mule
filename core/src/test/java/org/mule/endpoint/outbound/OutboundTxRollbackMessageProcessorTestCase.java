/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.transaction.Transaction;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class OutboundTxRollbackMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    @Test
    public void testNoRollback() throws InitialisationException, EndpointException, Exception
    {
        InterceptingMessageProcessor mp = new OutboundTxRollbackMessageProcessor();
        TestListener listener = new TestListener();
        mp.setListener(listener);

        MuleEvent event = createTestOutboundEvent();
        mp.process(event);

        assertSame(event, listener.sensedEvent);
    }

    @Test
    public void testRollback() throws InitialisationException, EndpointException, Exception
    {
        InterceptingMessageProcessor mp = new OutboundTxRollbackMessageProcessor();
        TestListener listener = new TestListener();
        mp.setListener(listener);

        Transaction tx = new TestTransaction(muleContext);
        try
        {
            TransactionCoordination.getInstance().bindTransaction(tx);
            tx.setRollbackOnly();

            MuleEvent event = createTestOutboundEvent();
            MuleEvent result = mp.process(event);

            assertNull(listener.sensedEvent);
            assertSame(result, event);
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(tx);
        }
    }
}
