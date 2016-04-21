/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.QueueProfile;
import org.mule.runtime.core.management.stats.QueueStatistics;
import org.mule.runtime.core.service.Pausable;
import org.mule.runtime.core.service.Resumable;
import org.mule.runtime.core.work.MuleWorkManager;

import javax.resource.spi.work.Work;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous
 * processing of the next {@link MessageProcessor}.
 */
public class LaxSedaStageInterceptingMessageProcessor extends SedaStageInterceptingMessageProcessor
    implements Work, Lifecycle, Pausable, Resumable
{

    public LaxSedaStageInterceptingMessageProcessor(String name,
                                                    String queueName,
                                                    QueueProfile queueProfile,
                                                    int queueTimeout,
                                                    ThreadingProfile threadingProfile,
                                                    QueueStatistics queueStatistics,
                                                    MuleContext muleContext)
    {
        super(name, queueName, queueProfile, queueTimeout, threadingProfile, queueStatistics, muleContext);
    }

    @Override
    protected boolean isProcessAsync(MuleEvent event) throws MessagingException
    {
        return doThreading && canProcessAsync(event);
    }
}
