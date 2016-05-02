/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.outbound;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.endpoint.outbound.OutboundTxRollbackMessageProcessor;
import org.mule.runtime.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Test;

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
