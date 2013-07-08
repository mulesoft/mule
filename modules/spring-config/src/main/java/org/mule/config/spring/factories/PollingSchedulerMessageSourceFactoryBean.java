package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.LifecycleAwareMessageProcessorWrapper;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessors;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.transport.polling.MessageProcessorPollingOverride;
import org.mule.transport.polling.PollingMessageSource;
import org.mule.transport.polling.schedule.FixedFrequencySchedulerFactory;

import org.springframework.beans.factory.FactoryBean;

/**
 * <p>
 * {@link FactoryBean} of a Poll element.
 * </p>
 */
public class PollingSchedulerMessageSourceFactoryBean implements FactoryBean, MuleContextAware
{

    /**
     * <p>
     * The {@link MessageProcessor} configured inside the poll element
     * </p>
     */
    protected MessageProcessor messageProcessor;

    /**
     * <p>
     * The {@link SchedulerFactory} that contains all the scheduler configuration
     * </p>
     */
    protected SchedulerFactory<PollingMessageSource> schedulerFactory;

    protected MessageProcessorPollingOverride override;

    /**
     * <p>
     * Kept for backward compatibility only. To support poll with frequency configured. If the poll element has a
     * schedule configured then the frequency value is ignored.
     * </p>
     * TODO: remove this for 4.0.0
     */
    private long frequency;
    private MuleContext context;

    @Override
    public Object getObject() throws Exception
    {
        if (schedulerFactory == null)
        {
            return new PollingMessageSource(sourceBuilder(), defaultSchedulerFactory(), override);
        }

        return new PollingMessageSource(sourceBuilder(), schedulerFactory, override);
    }

    private LifecycleAwareMessageProcessorWrapper sourceBuilder()
    {
        return MessageProcessors.lifecycleAwareMessageProcessorWrapper(messageProcessor);
    }

    @Override
    public Class<?> getObjectType()
    {
        return PollingMessageSource.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setSchedulerFactory(SchedulerFactory schedulerFactory)
    {
        this.schedulerFactory = schedulerFactory;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }

    private FixedFrequencySchedulerFactory defaultSchedulerFactory()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();
        factory.setFrequency(frequency);
        factory.setMuleContext(context);
        return factory;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public void setOverride(MessageProcessorPollingOverride override)
    {
        this.override = override;
    }
}
