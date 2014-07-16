/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;
import org.mule.transport.polling.MessageProcessorPollingOverride;
import org.mule.transport.polling.schedule.FixedFrequencySchedulerFactory;

public class PollingMessageSourceFactoryBean extends InboundEndpointFactoryBean
{
    protected SchedulerFactory<Runnable> schedulerFactory;
    protected MessageProcessor messageProcessor;
    protected MessageProcessorPollingOverride override;
    protected Long frequency;

    private FixedFrequencySchedulerFactory defaultSchedulerFactory()
    {
        FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory();
        factory.setFrequency(frequency);
        factory.setMuleContext(muleContext);
        return factory;
    }

    @Override
    public Object doGetObject() throws Exception
    {
        uriBuilder = new URIBuilder("polling://" + hashCode(), muleContext);

        properties.put(MessageProcessorPollingMessageReceiver.SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME, messageProcessor);
        properties.put(MessageProcessorPollingMessageReceiver.POLL_OVERRIDE_PROPERTY_NAME, override);
        properties.put(MessageProcessorPollingMessageReceiver.SCHEDULER_FACTORY_PROPERTY_NAME, schedulerFactory == null ? defaultSchedulerFactory() : schedulerFactory);
        properties.put(AbstractConnector.PROPERTY_POLLING_FREQUENCY, frequency);

        EndpointFactory ef = muleContext.getEndpointFactory();
        if (ef != null)
        {
            return ef.getInboundEndpoint(this);
        }
        else
        {
            throw new ConfigurationException(
                    MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
        }
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setOverride(MessageProcessorPollingOverride override)
    {
        this.override = override;
    }

    public void setFrequency(Long frequency)
    {
        this.frequency = frequency;
    }

    public void setSchedulerFactory(SchedulerFactory<Runnable> schedulerFactory)
    {
        this.schedulerFactory = schedulerFactory;
    }


}
