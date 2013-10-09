/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManagerSource;

public class LaxAsyncInterceptingMessageProcessor extends AsyncInterceptingMessageProcessor
{

    public LaxAsyncInterceptingMessageProcessor(WorkManagerSource workManagerSource)
    {
        super(workManagerSource);
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

}
