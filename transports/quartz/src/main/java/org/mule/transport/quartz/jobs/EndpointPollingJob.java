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
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.QuartzMessageReceiver;
import org.mule.transport.quartz.i18n.QuartzMessages;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.RegistryContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Will receive on an endpoint and dispatch it to the component set via the Receiver information.
 */
public class EndpointPollingJob implements Job
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();


        String receiverKey = (String) jobDataMap.get(QuartzMessageReceiver.QUARTZ_RECEIVER_PROPERTY);
        if (receiverKey == null)
        {
            throw new JobExecutionException(QuartzMessages.receiverNotInJobDataMap().getMessage());
        }

        String connectorName = (String) jobDataMap.get(QuartzMessageReceiver.QUARTZ_CONNECTOR_PROPERTY);
        if (connectorName == null)
        {
            throw new JobExecutionException(QuartzMessages.connectorNotInJobDataMap().getMessage());
        }

        QuartzConnector connector = (QuartzConnector) RegistryContext.getRegistry().lookupConnector(connectorName);
        if (connector == null)
        {
            throw new JobExecutionException(QuartzMessages.noConnectorFound(connectorName).getMessage());
        }

        AbstractMessageReceiver receiver = (AbstractMessageReceiver) connector.lookupReceiver(receiverKey);
        if (receiver == null)
        {
            throw new JobExecutionException(
                    QuartzMessages.noReceiverInConnector(receiverKey, connectorName).getMessage());
        }


        EndpointPollingJobConfig jobConfig = (EndpointPollingJobConfig) jobDataMap.get(QuartzConnector.PROPERTY_JOB_CONFIG);
        if (jobConfig == null)
        {
            throw new JobExecutionException(
                    QuartzMessages.missingJobDetail(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
        }


        try
        {
            MuleClient client = connector.getClient();
            logger.debug("Attempting to receive event on: " + jobConfig.getEndpointRef());
            MuleMessage result = client.request(jobConfig.getEndpointRef(), jobConfig.getTimeout());
            if (result != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Received event on: " + jobConfig.getEndpointRef());
                }
                //we need to do this because
                result = (MuleMessage)((ThreadSafeAccess)result).newThreadCopy();

                //Add the context properties to the message.
                result.addProperties(jobDataMap, PropertyScope.INVOCATION);

                receiver.routeMessage(result);
            }
        }
        catch (MuleException e)
        {
            throw new JobExecutionException(e);
        }
    }
}
