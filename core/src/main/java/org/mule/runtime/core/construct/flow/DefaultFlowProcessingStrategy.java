/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct.flow;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.processor.StageNameSource;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;
import org.mule.runtime.core.processor.LaxAsyncInterceptingMessageProcessor;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;

/**
 * This processing strategy uses the 'asynchronous' strategy where possible, but if an event is
 * synchronous it processes it synchronously rather than failing.
 */
public class DefaultFlowProcessingStrategy extends AsynchronousProcessingStrategy
{

    @Override
    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(StageNameSource nameSource, MuleContext muleContext)
    {
        ThreadingProfile threadingProfile = createThreadingProfile(muleContext);
        String stageName = nameSource.getName();
        return new LaxAsyncInterceptingMessageProcessor(threadingProfile, getThreadPoolName(stageName, muleContext),
                                                        muleContext.getConfiguration().getShutdownTimeout());
    }

}
