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

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.quartz.jobs.DelegatingJob;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.util.ClassUtils;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.util.Date;
import java.util.Iterator;

/**
 * Can schedule a Job with the Quartz scheduler. The event must contain the Job
 * to invoke or have it set as a property. Time triggger properties can be set
 * on the event to control how and when the event is fired.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
            String propertyKey = (String)iterator.next();
            jobDataMap.put(propertyKey, msg.getProperty(propertyKey));
        }
        jobDetail.setJobDataMap(jobDataMap);

        Job job;
        // work out what we're actually calling
        Object payload = event.getTransformedMessage();

        String jobClass = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_CLASS);
        if (payload instanceof Job) {
            job = (Job)payload;
            jobDataMap.put(QuartzConnector.PROPERTY_JOB_OBJECT, job);
            jobDetail.setJobClass(DelegatingJob.class);
        }
        else if (jobClass != null) {
            jobDetail.setJobClass(ClassUtils.loadClass(jobClass, getClass()));
        }
        else {
            Object tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_OBJECT);
            if (tempJob == null) {
                tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_REF);
                if (tempJob == null) {
                    throw new DispatchException(new Message("quartz", 2), event.getMessage(), event
                            .getEndpoint());
                }
                else {
                    tempJob = MuleManager.getInstance().getContainerContext().getComponent(tempJob);
                    if (!(tempJob instanceof Job)) {
                        throw new DispatchException(new Message("quartz", 3), event.getMessage(), event
                                .getEndpoint());
                    }
                }
            }
            else if (!(tempJob instanceof Job)) {
                throw new DispatchException(new Message("quartz", 3), event.getMessage(), event
                        .getEndpoint());
            }
            jobDetail.setJobClass(DelegatingJob.class);
        }

        jobDataMap.put(QuartzConnector.PROPERTY_PAYLOAD, payload);

        Trigger trigger = null;
        String cronExpression = jobDataMap.getString(QuartzConnector.PROPERTY_CRON_EXPRESSION);
        String repeatInterval = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_INTERVAL);
        String repeatCount = jobDataMap.getString(QuartzConnector.PROPERTY_REPEAT_COUNT);
        String startDelay = jobDataMap.getString(QuartzConnector.PROPERTY_START_DELAY);
        String groupName = jobDataMap.getString(QuartzConnector.PROPERTY_GROUP_NAME);
        String jobGroupName = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_GROUP_NAME);

        if (groupName == null) {
            groupName = QuartzConnector.DEFAULT_GROUP_NAME;
        }
        if (jobGroupName == null) {
            jobGroupName = groupName;
        }

        jobDetail.setGroup(groupName);

        if (cronExpression != null) {
            CronTrigger ctrigger = new CronTrigger();
            ctrigger.setCronExpression(cronExpression);
            trigger = ctrigger;
        }
        else if (repeatInterval != null) {
            SimpleTrigger strigger = new SimpleTrigger();
            strigger.setRepeatInterval(Long.parseLong(repeatInterval));
            if (repeatCount != null) {
                strigger.setRepeatCount(Integer.parseInt(repeatCount));
            }
            else {
                strigger.setRepeatCount(-1);
            }
            trigger = strigger;
        }
        else {
            throw new IllegalArgumentException(new Message("quartz", 1).getMessage());
        }
        long start = System.currentTimeMillis();
        if (startDelay != null) {
            start += Long.parseLong(startDelay);
        }
        trigger.setStartTime(new Date(start));
        trigger.setName(event.getEndpoint().getEndpointURI().toString());
        trigger.setGroup(groupName);
        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobGroupName);

        Scheduler scheduler = ((QuartzConnector)this.getConnector()).getScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
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
     * @param endpoint
     *            the endpoint to use when connecting to the resource
     * @param timeout
     *            the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available.
     *            If no data becomes available before the timeout elapses, null
     *            will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null
     *         will be returned if no data was avaialable
     * @throws Exception
     *             if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }
}
