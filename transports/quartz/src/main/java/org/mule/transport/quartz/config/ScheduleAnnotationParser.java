/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.config;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.annotations.Schedule;
import org.mule.api.annotations.meta.Channel;
import org.mule.api.annotations.meta.ChannelType;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.endpoint.AbstractEndpointAnnotationParser;
import org.mule.config.endpoint.AnnotatedEndpointData;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.jobs.EventGeneratorJobConfig;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Creates a Quartz inbound endpoint for a service
 */
public class ScheduleAnnotationParser extends AbstractEndpointAnnotationParser
{

    @Override
    public InboundEndpoint parseInboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        Schedule schedule = (Schedule) annotation;
        ScheduleConfigBuilder builder = lookupConfig(schedule.config(), ScheduleConfigBuilder.class);
        if (builder != null)
        {
            return builder.buildScheduler();
        }
        else
        {
            return super.parseInboundEndpoint(annotation, Collections.emptyMap());
        }
    }

    protected AnnotatedEndpointData createEndpointData(Annotation annotation) throws MuleException
    {
        //This will only get called if there is no config builder configured
        Schedule schedule = (Schedule) annotation;

        String uri = "quartz://schedule" + UUID.getUUID();
        AnnotatedEndpointData epData = new AnnotatedEndpointData(MessageExchangePattern.ONE_WAY, ChannelType.Inbound, annotation);

        epData.setProperties(convertProperties(getProperties(schedule)));
        //By default the scheduler should only use a single thread
        //TODO configure threads
        String threads = (String) epData.getProperties().get("threads");
        if (threads == null)
        {
            threads = "1";
            epData.getProperties().put("threads", threads);
        }
        epData.setAddress(uri);
        epData.setConnector(getConnector());
        //Create event generator job
        EventGeneratorJobConfig config = new EventGeneratorJobConfig();
        config.setStateful(threads.equals("1"));
        epData.getProperties().put(QuartzConnector.PROPERTY_JOB_CONFIG, config);
        return epData;
    }


    protected String[] getProperties(Schedule schedule) throws MuleException
    {
        List<String> props = new ArrayList<String>(2);
        if (StringUtils.isNotBlank(schedule.cron()))
        {
            props.add(QuartzConnector.PROPERTY_CRON_EXPRESSION + "=" + schedule.cron());
        }
        else if (schedule.interval() > -1)
        {
            props.add(QuartzConnector.PROPERTY_REPEAT_INTERVAL + "=" + schedule.interval());

            if (schedule.startDelay() > -1)
            {
                props.add(QuartzConnector.PROPERTY_START_DELAY +"=" + schedule.startDelay());
            }
        }
        else
        {
            throw new IllegalArgumentException("cron or repeatInterval must be set");
        }
        return CollectionUtils.toArrayOfComponentType(props, String.class);

    }

    protected String getIdentifier()
    {
        return Schedule.class.getAnnotation(Channel.class).identifer();
    }

    protected QuartzConnector getConnector() throws MuleException
    {
        QuartzConnector connector = new QuartzConnector(muleContext);
        connector.setName("scheduler." + connector.hashCode());
        muleContext.getRegistry().registerConnector(connector);
        return connector;
    }

    /**
     * Validates that this parser can parse the supplied annotation.  Only returns true if the clazz is not an interface
     * and the annotation is an instance of {@link org.mule.api.annotations.Schedule}
     *
     * @param annotation the annotation being processed
     * @param clazz      the class on which the annotation was found
     * @param member     the member on which the annotation was found inside the class.  this is only set when the annotation
     *                   was either set on a {@link java.lang.reflect.Method}, {@link java.lang.reflect.Field} or {@link java.lang.reflect.Constructor}
     *                   class members, otherwise this value is null.
     * @return true if this parser supports the current annotation and the clazz is not an interface
     * @throws IllegalArgumentException if the class parameter is an interface
     */
    @Override
    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return !clazz.isInterface() && super.supports(annotation, clazz, member);
    }
}
