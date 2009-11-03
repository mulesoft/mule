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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.NullPayload;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.QuartzMessageReceiver;
import org.mule.transport.quartz.i18n.QuartzMessages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 * Will generate a new event based o the scheduled time. The payload of the event is
 * currently a static object or instance of {@link org.mule.transport.NullPayload} if no payload
 * has been set.
 *
 * We may want to extend this but allowing the payload to be generated using a factory.
 */
public class EventGeneratorJob implements Job
{

    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        MuleContext muleContext;
        try
        {
            muleContext = (MuleContext)jobExecutionContext.getScheduler().getContext().get(MuleProperties.MULE_CONTEXT_PROPERTY);
        }
        catch (SchedulerException e)
        {
            throw new JobExecutionException("Failed to retrieve Mulecontext from the Scheduler Context: " + e.getMessage(), e);
        }

        JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();

        String receiverKey = (String)map.get(QuartzMessageReceiver.QUARTZ_RECEIVER_PROPERTY);
        if (receiverKey == null)
            throw new JobExecutionException(QuartzMessages.receiverNotInJobDataMap().getMessage());

        String connectorName = (String)map.get(QuartzMessageReceiver.QUARTZ_CONNECTOR_PROPERTY);
        if (connectorName == null)
            throw new JobExecutionException(QuartzMessages.connectorNotInJobDataMap().getMessage());

        AbstractConnector connector = (AbstractConnector) muleContext.getRegistry().lookupConnector(connectorName);
        if (connector == null)
            throw new JobExecutionException(QuartzMessages.noConnectorFound(connectorName).getMessage());

        AbstractMessageReceiver receiver = (AbstractMessageReceiver)connector.lookupReceiver(receiverKey);
        if (receiver == null)
            throw new JobExecutionException(
                QuartzMessages.noReceiverInConnector(receiverKey, connectorName).getMessage());

        Object payload = jobExecutionContext.getJobDetail().getJobDataMap().get(
            QuartzConnector.PROPERTY_PAYLOAD);

        try
        {
            if (payload == null)
            {
                String ref = jobExecutionContext.getJobDetail().getJobDataMap().getString(
                    QuartzConnector.PROPERTY_PAYLOAD);

                if (ref == null)
                {
                    payload = NullPayload.getInstance();
                }
                else
                {
                    payload = muleContext.getRegistry().lookupObject(ref);
                }

                if (payload==null)
                {
                    logger.warn("There is no payload attached to this quartz job. Sending Null payload");
                    payload = NullPayload.getInstance();
                }
            }
            MuleMessage msg = new DefaultMuleMessage(receiver.getConnector().getMessageAdapter(payload), muleContext);
            //If the job is stateful users cna store state in this map and have it available for the next job trigger
            msg.setProperty(QuartzConnector.PROPERTY_JOB_DATA, jobExecutionContext.getJobDetail().getJobDataMap(), PropertyScope.INVOCATION);
            receiver.routeMessage(msg);
        }
        catch (Exception e)
        {
            receiver.handleException(e);
        }
    }
}
