/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.quartz.config.JobConfig;
import org.mule.transport.quartz.i18n.QuartzMessages;
import org.mule.transport.quartz.jobs.CustomJob;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.CustomJobFromMessageConfig;

import java.util.Date;
import java.util.Iterator;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * Can schedule a Job with the Quartz scheduler. The event must contain the Job to
 * invoke or have it set as a property. Time triggger properties can be set on the
 * event to control how and when the event is fired.
 */
public class QuartzMessageDispatcher extends AbstractMessageDispatcher
{

    public QuartzMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        JobConfig jobConfig = (JobConfig) endpoint.getProperty(QuartzConnector.PROPERTY_JOB_CONFIG);
        if (jobConfig == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
        }

        JobDetail jobDetail = new JobDetail();
        // make the job name unique per endpoint (MULE-753)
        jobDetail.setName(event.getEndpoint().getEndpointURI().getAddress() + "-" + event.getId());

        JobDataMap jobDataMap = new JobDataMap();
        MuleMessage msg = event.getMessage();
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String) iterator.next();
            jobDataMap.put(propertyKey, msg.getProperty(propertyKey));
        }
        jobDetail.setJobDataMap(jobDataMap);

        Job job = null;
        // work out what we're actually calling
        Object payload = event.transformMessage();

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
        jobDetail.setJobClass(jobConfig.getJobClass());
        // If there has been a job created or found then we default to a customJob configuration
        if (job != null)
        {
            jobDataMap.put(QuartzConnector.PROPERTY_JOB_OBJECT, job);
            jobDetail.setJobClass(CustomJob.class);
        }
       
        // The payload will be ignored by the CustomJob - don't know why we need it here
        //RM: The custom job may want the message and the Job type may not be delegating job
        jobDataMap.put(QuartzConnector.PROPERTY_PAYLOAD, payload);

        Trigger trigger;
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

        jobDetail.setGroup(groupName);

        if (cronExpression != null)
        {
            CronTrigger ctrigger = new CronTrigger();
            ctrigger.setCronExpression(cronExpression);
            trigger = ctrigger;
        }
        else if (repeatInterval != null)
        {
            SimpleTrigger strigger = new SimpleTrigger();
            strigger.setRepeatInterval(Long.parseLong(repeatInterval));
            if (repeatCount != null)
            {
                strigger.setRepeatCount(Integer.parseInt(repeatCount));
            }
            else
            {
                strigger.setRepeatCount(-1);
            }
            trigger = strigger;
        }
        else
        {
            throw new IllegalArgumentException(
                QuartzMessages.cronExpressionOrIntervalMustBeSet().getMessage());
        }
        long start = System.currentTimeMillis();
        if (startDelay != null)
        {
            start += Long.parseLong(startDelay);
        }
        trigger.setStartTime(new Date(start));
        trigger.setName(event.getEndpoint().getEndpointURI().toString() + "-" + event.getId());
        trigger.setGroup(groupName);
        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobGroupName);

        Scheduler scheduler = ((QuartzConnector)this.getConnector()).getQuartzScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
       // scheduler.start();
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
