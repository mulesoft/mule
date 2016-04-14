/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.NonBlockingSupported;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.QueueProfile;
import org.mule.management.stats.QueueStatistics;
import org.mule.service.Pausable;
import org.mule.service.Resumable;
import org.mule.work.MuleWorkManager;

import javax.resource.spi.work.Work;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous
 * processing of the next {@link MessageProcessor}.
 */
public class LaxSedaStageInterceptingMessageProcessor extends SedaStageInterceptingMessageProcessor
    implements Work, Lifecycle, Pausable, Resumable, NonBlockingSupported
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
        return doThreading && !event.isSynchronous() && !event.isTransacted() && !event.getExchangePattern().hasResponse();
    }
}
