/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.config;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.NameableObject;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.quartz.QuartzConnector;

import org.quartz.Scheduler;

/**
 * TODO
 */
public class ScheduleConfigBuilder implements NameableObject
{
    private EndpointBuilder endpointBuilder;
    private QuartzConnector connector;
    private String scheduleId;

    public ScheduleConfigBuilder(String scheduleId, MuleContext muleContext) throws MuleException
    {
        super();
        this.scheduleId = scheduleId;

        endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder("quartz://" + scheduleId);
        endpointBuilder.setMuleContext(muleContext);
        endpointBuilder.setName(scheduleId);

        connector = new QuartzConnector(muleContext);
        connector.setName(scheduleId);

        endpointBuilder.setConnector(connector);
        endpointBuilder.setExchangePattern(MessageExchangePattern.ONE_WAY);
    }

    public ScheduleConfigBuilder addSchedulerFactoryProperty(String key, String value)
    {
        connector.getFactoryProperties().put(key, value);
        return this;
    }

    public ScheduleConfigBuilder setScheduler(Scheduler scheduler)
    {
        connector.setQuartzScheduler(scheduler);
        return this;
    }

    public ScheduleConfigBuilder setCron(String cron)
    {
        endpointBuilder.setProperty(QuartzConnector.PROPERTY_CRON_EXPRESSION, cron);
        return this;
    }

    public ScheduleConfigBuilder setInterval(long interval)
    {
        endpointBuilder.setProperty(QuartzConnector.PROPERTY_REPEAT_INTERVAL, interval);
        return this;
    }

    public ScheduleConfigBuilder setStartDelay(long delay)
    {
        endpointBuilder.setProperty(QuartzConnector.PROPERTY_START_DELAY, delay);
        return this;
    }

    public InboundEndpoint buildScheduler() throws MuleException
    {
        return endpointBuilder.buildInboundEndpoint();
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    public String getName()
    {
        return scheduleId + ".builder";
    }
}
