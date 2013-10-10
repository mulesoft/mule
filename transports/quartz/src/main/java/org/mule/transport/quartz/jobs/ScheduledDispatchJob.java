/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.i18n.QuartzMessages;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;

/**
 * Will dispatch the current message to a Mule endpoint at a later time.
 * This job can be used to fire time based events.
 */
public class ScheduledDispatchJob implements Job, Serializable
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Object payload = jobDataMap.get(QuartzConnector.PROPERTY_PAYLOAD);

        if (payload == null)
        {
            payload = NullPayload.getInstance();
        }

        ScheduledDispatchJobConfig config = (ScheduledDispatchJobConfig) jobDataMap.get(QuartzConnector.PROPERTY_JOB_CONFIG);
        if (config == null)
        {
            throw new JobExecutionException(
                QuartzMessages.missingJobDetail(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
        }

        try
        {
            SchedulerContext schedulerContext = jobExecutionContext.getScheduler().getContext();
            MuleContext muleContext = (MuleContext) schedulerContext.get(MuleProperties.MULE_CONTEXT_PROPERTY);

            String endpointRef = config.getEndpointRef();
            if (jobDataMap.containsKey("endpointRef"))
            {
                endpointRef = (String) jobDataMap.get("endpointRef");
            }

            logger.debug("Dispatching payload on: " + config.getEndpointRef());

            MuleClient client = new MuleClient(muleContext);
            client.dispatch(endpointRef, payload, jobDataMap);
        }
        catch (MuleException e)
        {
            throw new JobExecutionException(e);
        }
        catch (SchedulerException e)
        {
            throw new JobExecutionException(e);
        }
    }
}
