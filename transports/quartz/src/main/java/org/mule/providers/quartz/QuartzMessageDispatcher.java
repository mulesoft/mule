/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.RegistryContext;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.quartz.i18n.QuartzMessages;
import org.mule.providers.quartz.jobs.DelegatingJob;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.util.ClassUtils;

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

    public QuartzMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        JobDetail jobDetail = new JobDetail();
        // make the job name unique per endpoint (MULE-753)
        jobDetail.setName(event.getEndpoint().getEndpointURI().toString() + "-" + event.getId());

        JobDataMap jobDataMap = new JobDataMap();
        UMOMessage msg = event.getMessage();
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            jobDataMap.put(propertyKey, msg.getProperty(propertyKey));
        }
        jobDetail.setJobDataMap(jobDataMap);

        Job job;
        // work out what we're actually calling
        Object payload = event.getTransformedMessage();

        String jobClass = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_CLASS);
        // If the payload is a Job instance, then we are going to save it in
        // the jobDataMap under the key "jobObject". The actual Job that will 
        // execute will be the DelegatingJob
        if (payload instanceof Job)
        {
            job = (Job)payload;
            jobDataMap.put(QuartzConnector.PROPERTY_JOB_OBJECT, job);
            jobDetail.setJobClass(DelegatingJob.class);
        }
        // If the payload is not a Job instance, but the jobClass has been set
        // on the Message under the property "jobClass", then set the execution 
        // Job to be that class.
        else if (jobClass != null)
        {
            jobDetail.setJobClass(ClassUtils.loadClass(jobClass, getClass()));
        }
        // Otherwise, we have to find the job some other way
        else
        {
            // See if the Message has the job stored under "jobObject"
            Object tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_OBJECT);
            if (tempJob == null)
            {
                // See if the Message has the job stored under "jobRef"
                tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_REF);
                if (tempJob == null)
                {
                    // Now we'll give up
                    throw new DispatchException(QuartzMessages.invalidPayloadType(), 
                        event.getMessage(), event.getEndpoint());
                }
                else
                {
                    tempJob = RegistryContext.getRegistry().lookupObject(tempJob);
                    if (!(tempJob instanceof Job))
                    {
                        throw new DispatchException(QuartzMessages.invalidJobObject(), 
                            event.getMessage(), event.getEndpoint());
                    }
                }
            }
            else if (!(tempJob instanceof Job))
            {
                throw new DispatchException(QuartzMessages.invalidJobObject(), 
                    event.getMessage(), event.getEndpoint());
            }
            // If we have a job at this point, then the execution Job
            // will be the DelegatingJob
            jobDetail.setJobClass(DelegatingJob.class);
        }

        // The payload will be ignored by the DelegatingJob - don't know why
        // we need it here
        jobDataMap.put(QuartzConnector.PROPERTY_PAYLOAD, payload);

        Trigger trigger = null;
        String cronExpression = jobDataMap.getString(QuartzConnector.PROPERTY_CRON_EXPRESSION);
        String repeatInterval = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_INTERVAL);
        String repeatCount = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_COUNT);
        String startDelay = jobDataMap.getString(QuartzConnector.PROPERTY_START_DELAY);
        String groupName = jobDataMap.getString(QuartzConnector.PROPERTY_GROUP_NAME);
        String jobGroupName = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_GROUP_NAME);

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
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
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

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

}
