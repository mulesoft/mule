/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.exception.MessagingExceptionHandler;

public class LaxAsyncInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
{
    private MessagingExceptionHandler exceptionHandler;

    public LaxAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource, MessagingExceptionHandler exceptionHandler)
    {
        super(workManagerSource);
        this.exceptionHandler = exceptionHandler;
    }

    public LaxAsyncInterceptingMessageProcessor(ThreadingProfile threadingProfile,
                                                 String name,
                                                 int shutdownTimeout)
    {
        super(threadingProfile, name, shutdownTimeout);
    }

    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {
        return doThreading && !event.isSynchronous() && !event.isTransacted();
    }

    @Override
    protected MessagingExceptionHandler getMessagingExceptionHandler(MuleEvent event)
    {
        return exceptionHandler != null ? exceptionHandler : super.getMessagingExceptionHandler(event);
    }
}
