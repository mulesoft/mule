/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
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
        return doThreading && !event.isSynchronous() && !event.isTransacted();
    }
}
