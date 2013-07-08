package org.mule.transport.polling;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.LifecycleAwareMessageProcessorWrapper;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.api.source.MessageSource;
import org.mule.construct.Flow;
import org.mule.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.session.NullSessionHandler;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.util.Predicate;
import org.mule.util.StringUtils;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Polling {@link MessageSource}. The Poll element in the XML configuration ends up being a {@link PollingMessageSource}.
 * </p>
 * <p>
 * The {@link PollingMessageSource} is responsible of registering a {@link Scheduler} in the {@link org.mule.api.registry.MuleRegistry}
 * at the initialization phase. This {@link Scheduler} can be stopped/started and executed by using the {@link org.mule.api.registry.MuleRegistry}
 * interface, this way users can manipulate poll from outside mule server.
 * </p>
 *
 * @since 3.5.0
 */
public class PollingMessageSource implements MessageSource,
        Initialisable, MuleContextAware, FlowConstructAware, Startable, Stoppable
{

    protected transient Log logger = LogFactory.getLog(getClass());


    /**
     * <p>
     * The Polling transport name identifier. Used to create the scheduler name
     * </p>
     */
    private static String POLLING_TRANSPORT = "polling";

    /**
     * <p>
     * Format string for all the Polling Schedulers name.
     * </p>
     */
    public static String POLLING_NAME_FORMAT = POLLING_TRANSPORT + "://%s/%s";

    /**
     * @param source The {@link PollingMessageSource} from where we want to get the {@link Scheduler} name.
     * @return The Name of a poll {@link Scheduler}
     */
    public static String schedulerNameOf(PollingMessageSource source)
    {
        return String.format(POLLING_NAME_FORMAT, source.flowConstruct.getName(), source.hashCode());
    }

    /**
     * @return Predicate used to request the  {@link org.mule.api.registry.MuleRegistry} all the polling {@link Scheduler}
     */
    public static Predicate<String> allPollSchedulers()
    {
        return new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.startsWith(POLLING_TRANSPORT + "://");
            }
        };
    }

    /**
     * @return Predicate used to request the  {@link org.mule.api.registry.MuleRegistry} all the polling {@link Scheduler}
     *         for a particular {@link Flow}
     */
    public static Predicate<String> flowPollingSchedulers(final String flowName)
    {
        return new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.startsWith(POLLING_TRANSPORT + "://" + flowName);
            }
        };
    }


    /**
     * <p>
     * {@link MessageSource} listener. In this case is {@link org.mule.construct.AbstractPipeline} that represents
     * the {@link org.mule.construct.Flow}.
     * </p>
     */
    private MessageProcessor listener;

    /**
     * <p>
     * The {@link MuleContext} used to register the {@link Scheduler} and handle exceptions
     * </p>
     */
    private MuleContext context;

    /**
     * <p>
     * {@link org.mule.construct.Flow} where the {@link MessageSource} is configured.
     * </p>
     */
    private FlowConstruct flowConstruct;

    protected MessageProcessorPollingOverride override;


    /**
     * <p>
     * The {@link Flow} is executed with a initial source {@link MuleEvent}. That {@link MuleEvent} is created by the
     * {@link PollingMessageSource#eventBuilder} in this class. The {@link PollingMessageSource#eventBuilder} is
     * the {@link MessageProcessor} or {@link org.mule.api.endpoint.OutboundEndpoint} configured as a child element of the
     * poll. In this class it is an instance of {@link LifecycleAwareMessageProcessorWrapper} as its life cycle is
     * managed by the {@link PollingMessageSource}
     * </p>
     */
    private LifecycleAwareMessageProcessorWrapper eventBuilder;


    /**
     * <p>
     * {@link SchedulerFactory} used to create the poll {@link Scheduler} at initialisation time.
     * </p>
     */
    private SchedulerFactory<PollingMessageSource> schedulerFactory;


    public PollingMessageSource(LifecycleAwareMessageProcessorWrapper eventBuilder,
                                SchedulerFactory<PollingMessageSource> schedulerFactory,
                                MessageProcessorPollingOverride override)
    {
        this.eventBuilder = eventBuilder;
        this.schedulerFactory = schedulerFactory;
        this.override = override;
    }


    /**
     * <p>
     * Initialises the {@link PollingMessageSource#eventBuilder} and registers the poll {@link Scheduler} into the
     * {@link org.mule.api.registry.MuleRegistry}
     * </p>
     *
     * @throws InitialisationException In case of {@link PollingMessageSource#eventBuilder} could not be initialised or the
     *                                 {@link Scheduler} could not be registered into the {@link org.mule.api.registry.MuleRegistry}
     */
    @Override
    public void initialise() throws InitialisationException
    {
        // TODO: Does it make sense to do outbound endpoint exchange pattern here?
        eventBuilder.initialise();
        schedulerFactory.create(schedulerNameOf(this), this);

       if (override == null) {
            override = new NoOverride();
        }
    }

    public void run()
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        try
        {
            executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    MessageProcessorPollingInterceptor interceptor = override.interceptor();


                    MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, (Map<String, Object>) null, context);
                    MuleEvent event = new DefaultMuleEvent(request, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct);
                    event = interceptor.prepareSourceEvent(event);

                    MuleEvent process = eventBuilder.process(event);
                    if (process != null)
                    {
                        messageRouter(interceptor).routeEvent(process, null) ;
                    }
                    else
                    {
                        // TODO DF: i18n
                        logger.info(String.format("Polling of '%s' returned null, the flow will not be invoked.",
                                                  this));
                    }
                    return null;
                }

                private PollMessageRouter messageRouter(MessageProcessorPollingInterceptor interceptor)
                {
                    return new PollMessageRouter(listener,
                                                 MessageExchangePattern.ONE_WAY,
                                                 new NullSessionHandler(), flowConstruct, interceptor);
                }
            });
        }
        catch (Exception e)
        {
            context.getExceptionListener().handleException(e);
        }
    }

    private ExecutionTemplate<MuleEvent> createExecutionTemplate()
    {
        return TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(context, new MuleTransactionConfig());

    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        eventBuilder.setFlowConstruct(flowConstruct);
    }


    @Override
    public void start() throws MuleException
    {
        eventBuilder.start();
    }

    @Override
    public void stop() throws MuleException
    {
        eventBuilder.stop();
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
        eventBuilder.setMuleContext(context);
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }


    /**
     * Override implementation that doesn't change anything. Used as a default when no override is defined
     */
    private static class NoOverride extends MessageProcessorPollingOverride
    {
        private MessageProcessorPollingInterceptor noOpInterceptor = new MessageProcessorPollingInterceptor() {};

        @Override
        public MessageProcessorPollingInterceptor interceptor()
        {
            return noOpInterceptor;
        }
    }





}
