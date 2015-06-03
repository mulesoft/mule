/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.execution.ExecutionCallback;
import org.mule.transaction.TransactionCoordination;

/**
 * Commits any pending transaction.
 * <p/>
 * This interceptor must be executed before the error handling interceptor so if
 * there is any failure doing commit, the error handler gets executed.
 */
class CommitTransactionInterceptor implements ExecutionInterceptor<MuleEvent>
{

    private final ExecutionInterceptor<MuleEvent> nextInterceptor;

    public CommitTransactionInterceptor(ExecutionInterceptor<MuleEvent> nextInterceptor)
    {
        this.nextInterceptor = nextInterceptor;
    }

    @Override
    public MuleEvent execute(ExecutionCallback<MuleEvent> callback, ExecutionContext executionContext) throws Exception
    {
        MuleEvent result = nextInterceptor.execute(callback, executionContext);
        if (executionContext.needsTransactionResolution())
        {
            try
            {
                TransactionCoordination.getInstance().resolveTransaction();
            }
            catch (Exception e)
            {
                //Null result only happens when there's a filter in the chain.
                //Unfortunately a filter causes the whole chain to return null
                //and there's no other way to retrieve the last event but using the RequestContext.
                //see https://www.mulesoft.org/jira/browse/MULE-8670
                if (result == null || VoidMuleEvent.getInstance().equals(result))
                {
                    result = RequestContext.getEvent();
                }
                throw new MessagingException(result, e);
            }
        }
        return result;
    }
}
