/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.seda;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.lifecycle.processor.ProcessIfStartedWaitIfSyncPausedMessageProcessor;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.SedaStageInterceptingMessageProcessor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.service.AbstractService;
import org.mule.service.processor.ServiceInternalMessageProcessor;
import org.mule.service.processor.ServiceLoggingMessageObserver;
import org.mule.service.processor.ServiceOutboundMessageProcessor;
import org.mule.service.processor.ServiceOutboundStatisticsObserver;
import org.mule.service.processor.ServiceSetEventRequestContextMessageObserver;
import org.mule.service.processor.ServiceStatisticsMessageObserver;

/**
 * A Seda service runs inside a Seda Model and is responsible for managing a Seda
 * Queue and thread pool for a Mule sevice service. In Seda terms this is equivilent
 * to a stage.
 */
public class SedaService extends AbstractService
{
    /**
     * Serial version/
     */
    private static final long serialVersionUID = 7711976708670893015L;

    /**
     * The time out used for taking from the Seda Queue.
     */
    protected Integer queueTimeout;

    /**
     * The threading profile to use for this service. If this is not set a default
     * will be provided by the server
     */
    protected ThreadingProfile threadingProfile;

    /**
     * The queue profile to use for this service. If this is not set a default will
     * be provided by the server
     */
    protected QueueProfile queueProfile;

    protected WorkManager workManager;

    public SedaService(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected MessageProcessor getServiceStartedAssertingMessageProcessor()
    {
        return new ProcessIfStartedWaitIfSyncPausedMessageProcessor(this, lifecycleManager.getState());
    }

    protected void addMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        builder.chain(new ServiceLoggingMessageObserver(this));
        builder.chain(new ServiceStatisticsMessageObserver(this));
        builder.chain(new ServiceSetEventRequestContextMessageObserver());
        builder.chain(new SedaStageInterceptingMessageProcessor(getName(), queueProfile, queueTimeout,
            new WorkManagerSource()
            {
                public WorkManager getWorkManager() throws MuleException
                {
                    return workManager;
                }
            }, getThreadingProfile().isDoThreading(), lifecycleManager.getState(), stats,
            muleContext));
        builder.chain(new ServiceInternalMessageProcessor(this));
        if (asyncReplyMessageSource.getEndpoints().size() > 0)
        {
            builder.chain(createAsyncReplyProcessor());
        }
        builder.chain(new ServiceOutboundMessageProcessor(this));
        builder.chain(new ServiceOutboundStatisticsObserver(this));
        builder.chain(outboundRouter);
    }

    /**
     * Initialise the service. The service will first create a Mule component from
     * the Service and then initialise a pool based on the attributes in the Service.
     * 
     * @throws org.mule.api.lifecycle.InitialisationException if the service fails to
     *             initialise
     */
    @Override
    protected synchronized void doInitialise() throws InitialisationException
    {
        if (threadingProfile == null)
        {
            threadingProfile = muleContext.getDefaultServiceThreadingProfile();
        }
        // Create thread pool
        // (Add one to maximum number of active threads to account for the service
        // work item that is running continuously and polling the SEDA queue.)
        ChainedThreadingProfile threadingProfile = new ChainedThreadingProfile(this.threadingProfile);
        threadingProfile.setMuleContext(muleContext);
        threadingProfile.setMaxThreadsActive(threadingProfile.getMaxThreadsActive() + 1);
        // TODO it would be nicer if the shutdown value was encapsulated in the
        // Threading profile, but it is more difficult than it seems

        final MuleConfiguration config = muleContext.getConfiguration();
        final boolean containerMode = config.isContainerMode();
        final String threadPrefix = containerMode
                ? String.format("[%s].%s", config.getId(), getName())
                : getName();
        workManager = threadingProfile.createWorkManager(threadPrefix, config.getShutdownTimeout());

        if (queueProfile == null)
        {
            queueProfile = ((SedaModel) model).getQueueProfile();
        }
        if (queueTimeout == null)
        {
            setQueueTimeout(((SedaModel) model).getQueueTimeout());
        }

        try
        {
            if (name == null)
            {
                throw new InitialisationException(
                    MessageFactory.createStaticMessage("Service has no name to identify it"), this);
            }
        }
        catch (Throwable e)
        {
            throw new InitialisationException(CoreMessages.objectFailedToInitialise("Service Queue"), e, this);
        }
        super.doInitialise();
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            workManager.start();
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStart("Service: " + getName()), e, this);
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();
        workManager.dispose();
    }

    @Override
    protected void doForceStop() throws MuleException
    {
        doStop();
    }

    @Override
    protected void doDispose()
    {
        super.doDispose();
        if (workManager != null)
        {
            workManager.dispose();
        }
    }

    @Override
    protected ServiceStatistics createStatistics()
    {
        return new ServiceStatistics(getName(), threadingProfile.getMaxThreadsActive());
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    public Integer getQueueTimeout()
    {
        return queueTimeout;
    }

    public void setQueueTimeout(Integer queueTimeout)
    {
        this.queueTimeout = queueTimeout;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

}
