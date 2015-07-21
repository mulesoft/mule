/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import static org.mule.config.i18n.CoreMessages.pollSourceReturnedNull;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
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
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

import java.util.Map;

/**
 * <p>
 * Polling {@link org.mule.api.source.MessageSource}.
 * </p>
 * <p>
 * The {@link MessageProcessorPollingMessageReceiver} is responsible of creating a {@link org.mule.api.schedule.Scheduler}
 * at the initialization phase. This {@link org.mule.api.schedule.Scheduler} can be stopped/started and executed by using the {@link org.mule.api.registry.MuleRegistry}
 * interface, this way users can manipulate poll from outside mule server.
 * </p>
 */
public class MessageProcessorPollingMessageReceiver extends AbstractPollingMessageReceiver
{

    /**
     * <p>
     * The {@link InboundEndpoint} property for poll that contains the poll message source. Which is configured inside
     * the poll element in the XML configuration
     * </p>
     */
    public static final String SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "sourceMessageProcessor";

    /**
     * <p>
     * The {@link InboundEndpoint} property for poll that contains the {@link MessageProcessorPollingOverride}
     * </p>
     */
    public static final String POLL_OVERRIDE_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "pollOverride";

    /**
     * <p>
     * The {@link InboundEndpoint} property for poll that contains the {@link SchedulerFactory}
     * </p>
     */
    public static final String SCHEDULER_FACTORY_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "schedulerFactory";

    /**
     * <p>
     * The Polling transport name identifier. Used to create the scheduler name
     * </p>
     */
    public static final String POLLING_TRANSPORT = "polling";

    /**
     * <p>
     * Format string for all the Polling Schedulers name.
     * </p>
     */
    private static final String POLLING_SCHEDULER_NAME_FORMAT = POLLING_TRANSPORT + "://%s/%s";

    /**
     * The {@link org.mule.api.schedule.Scheduler} instance used to execute the scheduled jobs
     */
    private Scheduler scheduler;

    /**
     * <p>
     * Helper method to create {@link org.mule.api.schedule.Scheduler} names
     * </p>
     */
    private static String schedulerNameOf(MessageProcessorPollingMessageReceiver source)
    {
        return String.format(POLLING_SCHEDULER_NAME_FORMAT, source.flowConstruct.getName(), source.hashCode());
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

    public MessageProcessorPollingMessageReceiver(Connector connector,
                                                  FlowConstruct flowConstruct,
                                                  InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    public void poll() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, (Map<String, Object>) null,
                                                     getEndpoint().getMuleContext());
        pollWith(request);
    }

    public void pollWith(final MuleMessage request) throws Exception
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        try
        {
            final MessageProcessorPollingInterceptor interceptor = override.interceptor();
            MuleEvent muleEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {

                    ImmutableEndpoint ep = endpoint;
                    if (sourceMessageProcessor instanceof ImmutableEndpoint)
                    {
                        ep = (ImmutableEndpoint) sourceMessageProcessor;
                    }

                    MuleEvent event = new DefaultMuleEvent(request, ep.getExchangePattern(), flowConstruct);
                    event = interceptor.prepareSourceEvent(event);

                    OptimizedRequestContext.criticalSetEvent(event);

                    MuleEvent sourceEvent = sourceMessageProcessor.process(event);
                    if (isNewMessage(sourceEvent))
                    {
                        event = interceptor.prepareRouting(sourceEvent, createMuleEvent(sourceEvent.getMessage(), null));
                        routeEvent(event);
                        interceptor.postProcessRouting(event);
                    }
                    else
                    {
                        logger.info(pollSourceReturnedNull(flowConstruct.getName()));
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
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
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
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        sourceMessageProcessor = getSourceMessageProcessor();
        override = getPollOverride();

        if (override instanceof MuleContextAware)
        {
            ((MuleContextAware) override).setMuleContext(endpoint.getMuleContext());
        }

        if (override instanceof Initialisable)
        {
            ((Initialisable) override).initialise();
        }

        createScheduler();
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }

    @Override
    protected void doStart() throws MuleException
    {
        // The initialization phase if handled by the scheduler
        if (override instanceof Startable)
        {
            ((Startable) override).start();
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
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

    @Override
    protected void doDispose()
    {
        try
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
        finally
        {
            super.doDispose();

        }
    }

    private void createScheduler()
    {
        scheduler = getSchedulerFactory().create(schedulerNameOf(this), createWork());
    }

    private void disposeScheduler()
    {
        if (scheduler != null)
        {
            try
            {
                flowConstruct.getMuleContext().getRegistry().unregisterScheduler(scheduler);
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

    private MessageProcessorPollingOverride getPollOverride()
    {
        MessageProcessorPollingOverride override = (MessageProcessorPollingOverride) endpoint.getProperty(POLL_OVERRIDE_PROPERTY_NAME);
        if (override == null)
        {
            override = new NoOverride();
        }

        return override;
    }

    private MessageProcessor getSourceMessageProcessor() throws InitialisationException
    {
        MessageProcessor messageSource = (MessageProcessor) endpoint.getProperty(SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME);
        validate(messageSource);
        return messageSource;
    }

    private void validate(MessageProcessor messageSource) throws InitialisationException
    {
        if (messageSource instanceof OutboundEndpoint
            && !((OutboundEndpoint) messageSource).getExchangePattern().hasResponse())
        {
            throw new InitialisationException(CoreMessages.wrongMessageSource(messageSource.toString()), this);
        }
    }


    private SchedulerFactory<Runnable> getSchedulerFactory()
    {
        return (SchedulerFactory<Runnable>) endpoint.getProperty(SCHEDULER_FACTORY_PROPERTY_NAME);
    }

    /**
     * Override implementation that doesn't change anything. Used as a default when no override is defined
     */
    private static class NoOverride extends MessageProcessorPollingOverride
    {

        private MessageProcessorPollingInterceptor noOpInterceptor = new MessageProcessorPollingInterceptor()
        {
        };

        @Override
        public MessageProcessorPollingInterceptor interceptor()
        {
            return noOpInterceptor;
        }
    }


}
