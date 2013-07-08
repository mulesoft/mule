/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;
import org.mule.transport.polling.MessageProcessorPollingOverride;

import sun.net.ProgressMeteringPolicy;

import org.mule.transport.polling.PollingMessageSource;

@Deprecated
public class PollingMessageSourceFactoryBean extends InboundEndpointFactoryBean
{

    public static final String DISABLE_POLL_SCHEDULER = "disable.poll.scheduler";

    protected SchedulerFactory<PollingMessageSource> schedulerFactory;

    protected MessageProcessor messageProcessor;
    protected MessageProcessorPollingOverride override;
    protected Long frequency;

    /**
     * <p>
     * Kept for backward compatibility. If the compatibility check for poll is enabled then it creates the poll based
     * on the old poll implementation. If not then it creates poll with the new implementations (delegating the creation
     * to {@link PollingSchedulerMessageSourceFactoryBean}
     * </p>
     * TODO: Remove this for 4.0.0 and use {@link PollingSchedulerMessageSourceFactoryBean} instead.
     */
    @Override
    public Object getObject() throws Exception
    {
        if (isSchedulerFeatureDisabled())
        {
            return super.getObject();
        }
        else
        {
            PollingSchedulerMessageSourceFactoryBean factoryBean = new PollingSchedulerMessageSourceFactoryBean();
            factoryBean.setFrequency(frequency);
            factoryBean.setMessageProcessor(messageProcessor);
            factoryBean.setSchedulerFactory(schedulerFactory);
            factoryBean.setMuleContext(muleContext);
            factoryBean.setOverride(override);

            return factoryBean.getObject();

        }
    }

    @Override
    public Class<?> getObjectType()
    {
        if (isSchedulerFeatureDisabled())
        {
            return super.getObjectType();
        }
        else
        {
            return PollingMessageSource.class;

        }
    }

    private Boolean isSchedulerFeatureDisabled()
    {
        return Boolean.valueOf(System.getProperty(DISABLE_POLL_SCHEDULER, "false"));
    }

    @Override
    public Object doGetObject() throws Exception
    {
        uriBuilder = new URIBuilder("polling://" + hashCode(), muleContext);

        properties.put(MessageProcessorPollingMessageReceiver.SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME, messageProcessor);
        properties.put(MessageProcessorPollingMessageReceiver.POLL_OVERRIDE_PROPERTY_NAME, override);
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

    public void setSchedulerFactory(SchedulerFactory<PollingMessageSource> schedulerFactory)
    {
        this.schedulerFactory = schedulerFactory;
    }


}
