/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

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
