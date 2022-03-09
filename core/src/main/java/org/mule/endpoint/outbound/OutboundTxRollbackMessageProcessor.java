/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transaction.TransactionCoordination;

/**
 * MessageProcessor implementation that stops outbound flow is the current
 * transaction has been rolled back.
 */
public class OutboundTxRollbackMessageProcessor extends AbstractInterceptingMessageProcessor
{
    private static final Log logger = LogFactory.getLog(OutboundTxRollbackMessageProcessor.class);

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // No point continuing if the service has rolledback the transaction
        if (isTransactionRollback())
        {
            return event;
        }
        else
        {
            return processNext(event);
        }
    }

    /**
     * Checks to see if the current transaction has been rolled back
     */
    protected boolean isTransactionRollback()
    {
        try
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (tx != null && tx.isRollbackOnly())
            {
                return true;
            }
        }
        catch (TransactionException e)
        {
            // TODO MULE-863: What should we really do?
            logger.warn(e.getMessage());
        }
        return false;
    }
}
