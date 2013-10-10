/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.i18n.QuartzMessages;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * Will dispatch the current message to a Mule endpoint at a later time.
 * This job can be used to fire time based events.
 */
public class ScheduledDispatchJob extends AbstractJob implements Serializable
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected void doExecute(JobExecutionContext jobExecutionContext) throws JobExecutionException
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

        if (this instanceof StatefulJob)
        {
            // Forces synchronous processing for the generated event
            jobDataMap.put(MuleProperties.MULE_FORCE_SYNC_PROPERTY, Boolean.TRUE);
        }

        try
        {
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
    }
}
