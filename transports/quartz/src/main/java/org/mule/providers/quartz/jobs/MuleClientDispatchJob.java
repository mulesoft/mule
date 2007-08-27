/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz.jobs;

import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.providers.quartz.i18n.QuartzMessages;
import org.mule.umo.UMOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Will dispatch to a Mule endpoint using the Mule client.
 */
public class MuleClientDispatchJob implements Job
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

        String dispatchEndpoint = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT);
        if (dispatchEndpoint == null)
        {
            throw new JobExecutionException(
                QuartzMessages.missingJobDetail(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT).getMessage());
        }

        try
        {
            MuleClient client = new MuleClient();
            logger.debug("Dispatching payload on: " + dispatchEndpoint);
            client.dispatch(dispatchEndpoint, payload, jobDataMap);
        }
        catch (UMOException e)
        {
            throw new JobExecutionException(e);
        }
    }
}
