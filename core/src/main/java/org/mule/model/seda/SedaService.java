/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.seda;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.lifecycle.processor.ProcessIfStartedWaitIfSyncPausedMessageProcessor;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.LaxSedaStageInterceptingMessageProcessor;
import org.mule.processor.SedaStageInterceptingMessageProcessor;
import org.mule.service.AbstractService;
import org.mule.service.processor.ServiceInternalMessageProcessor;
import org.mule.service.processor.ServiceLoggingMessageProcessor;
import org.mule.service.processor.ServiceOutboundMessageProcessor;
import org.mule.service.processor.ServiceOutboundStatisticsMessageProcessor;
import org.mule.service.processor.ServiceSetEventRequestContextMessageProcessor;
import org.mule.service.processor.ServiceStatisticsMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;

/**
 * A Seda service runs inside a Seda Model and is responsible for managing a Seda
 * Queue and thread pool for a Mule sevice service. In Seda terms this is equivilent
 * to a stage.
 */
@Deprecated
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

    protected SedaStageInterceptingMessageProcessor sedaStage;

    public SedaService(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected MessageProcessor getServiceStartedAssertingMessageProcessor()
    {
        return new ProcessIfStartedWaitIfSyncPausedMessageProcessor(this, lifecycleManager.getState());
    }

    protected void addMessageProcessors(MessageProcessorChainBuilder builder)
    {
        builder.chain(new ProcessingTimeInterceptor(null, this));
        builder.chain(new ServiceLoggingMessageProcessor(this));
        builder.chain(new ServiceStatisticsMessageProcessor(this));
        builder.chain(new ServiceSetEventRequestContextMessageProcessor());
        if (getThreadingProfile().isDoThreading())
        {
            sedaStage = new LaxSedaStageInterceptingMessageProcessor(ThreadNameHelper.sedaService(
                muleContext, getName()), getName(), queueProfile, queueTimeout, threadingProfile, stats,
                muleContext);
            builder.chain(sedaStage);
        }
        builder.chain(new ServiceInternalMessageProcessor(this));
        if (asyncReplyMessageSource.getEndpoints().size() > 0)
        {
            builder.chain(createAsyncReplyProcessor());
        }
        builder.chain(new ServiceOutboundMessageProcessor(this));
        builder.chain(new ServiceOutboundStatisticsMessageProcessor(this));
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

        if (queueProfile == null && model != null)
        {
            queueProfile = ((SedaModel) model).getQueueProfile();
        }
        if (queueTimeout == null && model != null)
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

    @Override
    protected void doPause() throws MuleException
    {
        super.doPause();
        sedaStage.pause();
    }

    @Override
    protected void doResume() throws MuleException
    {
        sedaStage.resume();
        super.doResume();
    }

}
