/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz.jobs;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.i18n.QuartzMessages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Will dispatch the current message to a Mule endpoint at a leter time.
 * This job can be used to fire timebased events.
 */
public class ScheduledDispatchJob implements Job
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
            MuleClient client = new MuleClient();

            String endpointRef = config.getEndpointRef();
            if (jobDataMap.containsKey("endpointRef")) 
            {
                endpointRef = (String) jobDataMap.get("endpointRef");
            }

            logger.debug("Dispatching payload on: " + config.getEndpointRef());

            client.dispatch(endpointRef, payload, jobDataMap);
        }
        catch (MuleException e)
        {
            throw new JobExecutionException(e);
        }
    }
}
