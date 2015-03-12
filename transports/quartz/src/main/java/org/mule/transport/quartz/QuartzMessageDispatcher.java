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
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.config.JobConfig;
import org.mule.transport.quartz.i18n.QuartzMessages;
import org.mule.transport.quartz.jobs.CustomJob;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.CustomJobFromMessageConfig;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;

/**
 * Can schedule a Job with the Quartz scheduler. The event must contain the Job to
 * invoke or have it set as a property. Time trigger properties can be set on the
 * event to control how and when the event is fired.
 */
public class QuartzMessageDispatcher extends AbstractMessageDispatcher
{

    public QuartzMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        JobConfig jobConfig = (JobConfig) endpoint.getProperty(QuartzConnector.PROPERTY_JOB_CONFIG);
        if (jobConfig == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
        }


        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(QuartzConnector.PROPERTY_JOB_DYNAMIC, Boolean.TRUE);
        MuleMessage msg = event.getMessage();
        // populate from invocation and outbound scopes only
        for (String key : msg.getInvocationPropertyNames())
        {
            jobDataMap.put(key, msg.getInvocationProperty(key));
        }
        for (String key : msg.getOutboundPropertyNames())
        {
            jobDataMap.put(key, msg.getOutboundProperty(key));
        }

        if (jobConfig instanceof ScheduledDispatchJobConfig) 
        {
            ScheduledDispatchJobConfig scheduledDispatchJobConfig = (ScheduledDispatchJobConfig) jobConfig;
            String endpointRef = event.getMuleContext()
                .getExpressionManager()
                .parse(scheduledDispatchJobConfig.getEndpointRef(), event);

            jobDataMap.put("endpointRef", endpointRef);
        }

        Job job = null;
        // work out what we're actually calling
        Object payload = event.getMessage().getPayload();

        if(jobConfig instanceof CustomJobConfig)
        {
            job = ((CustomJobConfig) jobConfig).getJob();
        }
        else if(jobConfig instanceof CustomJobFromMessageConfig)
        {
            job = ((CustomJobFromMessageConfig) jobConfig).getJob(msg);
            //rewrite the jobConfig to the real Jobconfig on the message
            jobConfig = ((CustomJobFromMessageConfig) jobConfig).getJobConfig(msg);
        }

        jobDataMap.put(QuartzConnector.PROPERTY_JOB_CONFIG, jobConfig);
        Class<? extends Job> jobClass = jobConfig.getJobClass();
        // If there has been a job created or found then we default to a customJob configuration
        if (job != null)
        {
            jobDataMap.put(QuartzConnector.PROPERTY_JOB_OBJECT, job);
            jobClass = CustomJob.class;
        }
       
        // The payload will be ignored by the CustomJob - don't know why we need it here
        //RM: The custom job may want the message and the Job type may not be delegating job
        jobDataMap.put(QuartzConnector.PROPERTY_PAYLOAD, payload);

        String cronExpression = jobDataMap.getString(QuartzConnector.PROPERTY_CRON_EXPRESSION);
        String repeatInterval = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_INTERVAL);
        String repeatCount = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_COUNT);
        String startDelay = jobDataMap.getString(QuartzConnector.PROPERTY_START_DELAY);
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

        JobDetail jobDetail = newJob(jobClass)
            // make the job name unique per endpoint (MULE-753)
            .withIdentity(endpoint.getEndpointURI().getAddress() + "-" + event.getId(), groupName)
            .usingJobData(jobDataMap)
                .build();

        TriggerBuilder triggerBuilder = newTrigger()
            .withIdentity(endpoint.getEndpointURI().toString() + "-" + event.getId(), groupName)
            .forJob(jobDetail.getKey().getName(), jobGroupName);

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

        Scheduler scheduler = ((QuartzConnector) this.getConnector()).getQuartzScheduler();

        // Minimize the the time window capturing the start time and scheduling the job.
        long start = System.currentTimeMillis();
        if (startDelay != null)
        {
            start += Long.parseLong(startDelay);
        }
        triggerBuilder.startAt(new Date(start));

        scheduler.scheduleJob(jobDetail, triggerBuilder.build());
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
