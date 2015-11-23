/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.transport.NullPayload;
import org.mule.transport.TrackingWorkManager;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//MULE-9139 - TODO verify if only one connector was created for all the polling elements in the config. If that's the case then we need to create a centralized service for the scheduling service
/**
 * <p>
 * Polling {@link org.mule.api.source.MessageSource}.
 * </p>
 * <p>
 * The {@link PollingMessageSource} is responsible of creating a {@link org.mule.api.schedule.Scheduler}
 * at the initialization phase. This {@link org.mule.api.schedule.Scheduler} can be stopped/started and executed by using the {@link org.mule.api.registry.MuleRegistry}
 * interface, this way users can manipulate poll from outside mule server.
 * </p>
 */
public class PollingMessageSource implements MessageSource, FlowConstructAware, Startable, Stoppable, MuleContextAware, Initialisable, Disposable
{

    private static Logger logger = LoggerFactory.getLogger(PollingMessageSource.class);

    /**
     * <p>
     * The Polling name identifier. Used to create the scheduler name
     * </p>
     */
    public static final String POLLING_SCHEME = "polling";

    /**
     * <p>
     * Format string for all the Polling Schedulers name.
     * </p>
     */
    private static final String POLLING_SCHEDULER_NAME_FORMAT = POLLING_SCHEME + "://%s/%s";
    private final SchedulerFactory<Runnable> schedulerFactory;

    /**
     * The {@link org.mule.api.schedule.Scheduler} instance used to execute the scheduled jobs
     */
    private Scheduler scheduler;

    private MessageProcessor listener;

    public static final long DEFAULT_POLL_FREQUENCY = 1000;
    public static final TimeUnit DEFAULT_POLL_TIMEUNIT = TimeUnit.MILLISECONDS;

    private long frequency = DEFAULT_POLL_FREQUENCY;
    private TimeUnit timeUnit = DEFAULT_POLL_TIMEUNIT;

    private FlowConstruct flowConstruct;
    private MuleContext muleContext;
    private boolean started;

    public PollingMessageSource(MuleContext muleContext, MessageProcessor sourceMessageProcessor, MessageProcessorPollingOverride override, SchedulerFactory<Runnable> schedulerFactory, Long frequency)
    {
        this.muleContext = muleContext;
        this.sourceMessageProcessor = sourceMessageProcessor;
        this.override = override;
        this.schedulerFactory = schedulerFactory;
        setFrequency(frequency);
    }

    @Override
    public void start() throws MuleException
    {
        try
        {
            // The initialization phase if handled by the scheduler
            if (override instanceof Startable)
            {
                ((Startable) override).start();
            }
            if (sourceMessageProcessor instanceof Startable)
            {
                ((Startable) sourceMessageProcessor).start();
            }
            started = true;
        }
        catch (Exception ex)
        {
            this.stop();
            throw new CreateException(CoreMessages.failedToScheduleWork(), ex, this);
        }
    }

    public String getPollingUniqueName()
    {
        return flowConstruct.getName() + "-polling-" + this.hashCode();
    }

    @Override
    public void stop() throws MuleException
    {
        this.started = false;
        if (override instanceof Stoppable)
        {
            ((Stoppable) override).stop();
        }

        // Stop the scheduler to address the case when the flow is stop but not the application
        if (scheduler != null)
        {
            scheduler.stop();
        }
    }

    protected PollingWorker createWork()
    {
        return new PollingWorker(new PollingTask()
        {
            @Override
            public boolean isStarted()
            {
                return PollingMessageSource.this.started;
            }

            @Override
            public void run() throws Exception
            {
                PollingMessageSource.this.performPoll();
            }

            @Override
            public void stop() throws MuleException
            {
                PollingMessageSource.this.stop();
            }
        }, muleContext.getExceptionListener());
    }

    public long getFrequency()
    {
        return frequency;
    }

    public void setFrequency(long value)
    {
        if (value <= 0)
        {
            frequency = DEFAULT_POLL_FREQUENCY;
        }
        else
        {
            frequency = value;
        }
    }

    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    /**
     * Check whether polling should take place on this instance.
     */
    public final void performPoll() throws Exception
    {
        if (!pollOnPrimaryInstanceOnly() || flowConstruct.getMuleContext().isPrimaryPollingInstance())
        {
            poll();
        }
    }

    private boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }

    /**
     * <p>
     * Helper method to create {@link org.mule.api.schedule.Scheduler} names
     * </p>
     */
    private String schedulerNameOf(FlowConstruct flowConstruct)
    {
        return String.format(POLLING_SCHEDULER_NAME_FORMAT, flowConstruct.getName(), this.hashCode());
    }

    /**
     * <p>
     * The poll message source, configured inside the poll element in the xml configuration. i.e.:
     * <pre>
     * {@code
     * <poll>
     *       <sfdc:query query=""/>
     * </poll>
     * </pre>
     * </p>
     */
    protected MessageProcessor sourceMessageProcessor;

    /**
     * <p>
     * The {@link MessageProcessorPollingOverride} that affects the routing of the {@link MuleEvent}
     * </p>
     */
    protected MessageProcessorPollingOverride override;

    public void poll() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, (Map<String, Object>) null, muleContext);
        pollWith(request);
    }

    public void pollWith(final MuleMessage request) throws Exception
    {
        ExecutionTemplate<MuleEvent> executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(muleContext, flowConstruct.getExceptionListener());
        try
        {
            final MessageProcessorPollingInterceptor interceptor = override.interceptor();
            MuleEvent muleEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    //TODO review what to put as exchange pattern. It used to be the one from the inbound endpoint if the message processor was an IE
                    MuleEvent event = new DefaultMuleEvent(request, MessageExchangePattern.ONE_WAY, flowConstruct);
                    event = interceptor.prepareSourceEvent(event);

                    OptimizedRequestContext.criticalSetEvent(event);

                    MuleEvent sourceEvent = sourceMessageProcessor.process(event);
                    if (isNewMessage(sourceEvent))
                    {
                        muleContext.getNotificationManager().fireNotification(new ConnectorMessageNotification(sourceEvent.getMessage(), getPollingUniqueName(), flowConstruct, ConnectorMessageNotification.MESSAGE_RECEIVED));
                        event = interceptor.prepareRouting(sourceEvent, new DefaultMuleEvent(sourceEvent.getMessage(), sourceEvent));
                        listener.process(event);
                        interceptor.postProcessRouting(event);
                    }
                    else
                    {
                        logger.info(CoreMessages.pollSourceReturnedNull(flowConstruct.getName()).getMessage());
                    }
                    return null;
                }
            });
            if (muleEvent != null)
            {
                interceptor.postProcessRouting(muleEvent);
            }
        }
        catch (MessagingException e)
        {
            //Already handled by TransactionTemplate
        }
        catch (Exception e)
        {
            muleContext.getExceptionListener().handleException(e);
        }
    }

    /**
     * <p>
     * On the Initialization phase it.
     * <ul>
     * <li>Calls the {@link SchedulerFactory} to create the scheduler</li>
     * <li>Gets the Poll the message source</li>
     * <li>Gets the Poll override</li>
     * </ul>
     * </p>
     */
    @Override
    public void initialise() throws InitialisationException
    {
        if (sourceMessageProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) sourceMessageProcessor).setMuleContext(muleContext);
        }
        if (sourceMessageProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) sourceMessageProcessor).setFlowConstruct(flowConstruct);
        }
        if (sourceMessageProcessor instanceof Initialisable)
        {
            ((Initialisable) sourceMessageProcessor).initialise();
        }
        if (override instanceof MuleContextAware)
        {
            ((MuleContextAware) override).setMuleContext(muleContext);
        }
        if (override instanceof FlowConstructAware)
        {
            ((FlowConstructAware) override).setFlowConstruct(flowConstruct);
        }
        if (override instanceof Initialisable)
        {
            ((Initialisable) override).initialise();
        }
        createScheduler();
    }

    @Override
    public void dispose()
    {
        if (override instanceof Disposable)
        {
            try
            {
                ((Disposable) override).dispose();
            }
            catch (Exception e)
            {
                logger.warn(String.format("Could not dispose polling override of class %s. Message receiver will continue to dispose", override.getClass().getCanonicalName()), e);
            }
        }

        disposeScheduler();
    }

    private void createScheduler()
    {
        scheduler = schedulerFactory.create(schedulerNameOf(flowConstruct), createWork());
    }

    private void disposeScheduler()
    {
        if (scheduler != null)
        {
            try
            {
                muleContext.getRegistry().unregisterScheduler(scheduler);
            }
            catch (MuleException e)
            {
                logger.warn(String.format("Could not unregister scheduler %s from registry.", scheduler.getName()), e);
            }
            scheduler = null;
        }
    }

    // Only consider response for source message processor a new message if it is not
    // null and payload is not NullPayload
    protected boolean isNewMessage(MuleEvent sourceEvent)
    {
        if (sourceEvent != null && !VoidMuleEvent.getInstance().equals(sourceEvent) && sourceEvent.getMessage() != null)
        {
            MuleMessage message = sourceEvent.getMessage();
            if (message.getPayload().equals(NullPayload.getInstance()))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }


    @Override
    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

}
