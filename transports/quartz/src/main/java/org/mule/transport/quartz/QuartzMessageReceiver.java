/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.quartz.config.JobConfig;
import org.mule.transport.quartz.i18n.QuartzMessages;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.EventGeneratorJobConfig;

import java.io.OutputStream;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;
import org.quartz.TriggerBuilder;

/**
 * Listens for Quartz sheduled events using the Receiver Job and fires events to the
 * service associated with this receiver.
 */
public class QuartzMessageReceiver extends AbstractMessageReceiver
{

    public static final String QUARTZ_RECEIVER_PROPERTY = "mule.quartz.receiver";
    public static final String QUARTZ_CONNECTOR_PROPERTY = "mule.quartz.connector";

    private final QuartzConnector connector;
    private boolean isStateful;

    public QuartzMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.connector = (QuartzConnector) connector;
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected MuleEvent createMuleEvent(MuleMessage message, OutputStream outputStream) throws MuleException
    {
        if (isStateful)
        {
            // Forces synchronous processing for the generated event
            message.setProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY, Boolean.TRUE, PropertyScope.INBOUND);
        }

        return super.createMuleEvent(message, outputStream);
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            Scheduler scheduler = connector.getQuartzScheduler();

            JobConfig jobConfig = (JobConfig) endpoint.getProperty(QuartzConnector.PROPERTY_JOB_CONFIG);
            if (jobConfig == null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
            }

            Class<? extends Job> jobClass = jobConfig.getJobClass();
            JobBuilder jobBuilder = newJob(jobClass);
            isStateful = StatefulJob.class.isAssignableFrom(jobClass);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(QUARTZ_RECEIVER_PROPERTY, this.getReceiverKey());
            jobDataMap.put(QUARTZ_CONNECTOR_PROPERTY, this.connector.getName());
            jobDataMap.putAll(endpoint.getProperties());

            if (jobConfig instanceof EventGeneratorJobConfig)
            {
                jobDataMap.put(QuartzConnector.PROPERTY_PAYLOAD, ((EventGeneratorJobConfig) jobConfig).getPayload());
            }
            jobDataMap.put(QuartzConnector.PROPERTY_JOB_CONFIG, jobConfig);

            Job job = null;
            if (jobConfig instanceof CustomJobConfig)
            {
                job = ((CustomJobConfig) jobConfig).getJob();
            }
            // If there has been a job created or found then we default to a custom Job configuration
            if (job != null)
            {
                jobDataMap.put(QuartzConnector.PROPERTY_JOB_OBJECT, job);
            }

            jobBuilder.usingJobData(jobDataMap);

            String cronExpression = (String)endpoint.getProperty(QuartzConnector.PROPERTY_CRON_EXPRESSION);
            String repeatInterval = (String)endpoint.getProperty(QuartzConnector.PROPERTY_REPEAT_INTERVAL);
            String repeatCount = (String)endpoint.getProperty(QuartzConnector.PROPERTY_REPEAT_COUNT);
            String startDelay = (String)endpoint.getProperty(QuartzConnector.PROPERTY_START_DELAY);
            String groupName = jobConfig.getGroupName();
            String jobGroupName = jobConfig.getJobGroupName();

            if (groupName == null)
            {
                groupName = QuartzConnector.DEFAULT_GROUP_NAME;
            }
            if (jobGroupName == null)
            {
                jobGroupName = groupName;
            }

            jobBuilder.withIdentity(endpoint.getEndpointURI().getAddress(), groupName);

            TriggerBuilder triggerBuilder = newTrigger()
                    .withIdentity(endpoint.getEndpointURI().getAddress(), groupName)
                    .forJob(endpoint.getEndpointURI().getAddress(), jobGroupName);

            if (cronExpression != null)
            {
                triggerBuilder.withSchedule(cronSchedule(cronExpression));
            }
            else if (repeatInterval != null)
            {
                triggerBuilder.withSchedule(simpleSchedule()
                    .withIntervalInMilliseconds(Long.parseLong(repeatInterval))
                    .withRepeatCount(repeatCount != null ? Integer.parseInt(repeatCount) : SimpleTrigger.REPEAT_INDEFINITELY));
            }
            else
            {
                throw new IllegalArgumentException(
                        QuartzMessages.cronExpressionOrIntervalMustBeSet().getMessage());
            }


            // Minimize the the time window capturing the start time and scheduling the job.
            long start = System.currentTimeMillis();
            if (startDelay != null)
            {
                start += Long.parseLong(startDelay);
            }
            triggerBuilder.startAt(new Date(start));

            // We need to handle cases when the job has already been persisted
            try
            {
                scheduler.scheduleJob(jobBuilder.build(), triggerBuilder.build());
            }
            catch (ObjectAlreadyExistsException oaee)
            {
                logger.warn("A quartz Job with name: " + endpoint.getEndpointURI().getAddress() +
                        " has already been registered. Cannot register again");
            }
        }
        catch (Exception e)
        {
            throw new EndpointException(CoreMessages.failedToStart("Quartz receiver"), e);
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        // nothing to do
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }

}
