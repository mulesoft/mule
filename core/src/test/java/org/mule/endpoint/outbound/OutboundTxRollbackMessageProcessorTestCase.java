/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

public class OutboundTxRollbackMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    public void testNoRollback() throws InitialisationException, EndpointException, Exception
    {
        InterceptingMessageProcessor mp = new OutboundTxRollbackMessageProcessor();
        TestListener listener = new TestListener();
        mp.setListener(listener);

        MuleEvent event = createTestOutboundEvent(createTestOutboundEndpoint(null, null));
        mp.process(event);

        assertSame(event, listener.sensedEvent);
    }

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

            MuleEvent event = createTestOutboundEvent(createTestOutboundEndpoint(null, null));
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
