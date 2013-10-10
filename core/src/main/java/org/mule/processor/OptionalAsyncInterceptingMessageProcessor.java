/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManagerSource;

/**
 * Implementation of {@link AsyncInterceptingMessageProcessor} which continues
 * processing in the same thread if the inbound endpoint has an exchange pattern that
 * has a response or if a transaction is present. Execution of the next message
 * processor is only passed off to another thread if this is not the case.
 */
public class OptionalAsyncInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
{
    public OptionalAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource)
    {
        super(workManagerSource);
    }

    @Deprecated
    public OptionalAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource, boolean doThreading)
    {
        super(workManagerSource, doThreading);
    }

    public OptionalAsyncInterceptingMessageProcessor(ThreadingProfile threadingProfile,
                                                     String name,
                                                     int shutdownTimeout)
    {
        super(threadingProfile, name, shutdownTimeout);
    }

    @Override
    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {   
        Object messageProperty = event.getMessage().getInboundProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY);
        boolean forceSync = Boolean.TRUE.equals(messageProperty);
        
        boolean hasResponse = event.getEndpoint().getExchangePattern().hasResponse();
        boolean isTransacted = event.getEndpoint().getTransactionConfig().isTransacted();
        
        return !forceSync && doThreading && !hasResponse && !isTransacted;
    }

}
