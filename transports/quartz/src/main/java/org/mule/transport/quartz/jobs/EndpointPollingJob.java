/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.PropertyScope;
import org.mule.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.module.client.MuleClient;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.QuartzMessageReceiver;
import org.mule.transport.quartz.i18n.QuartzMessages;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Will receive on an endpoint and dispatch it to the component set via the Receiver information.
 */
public class EndpointPollingJob extends AbstractJob
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected void doExecute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

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

        final QuartzConnector connector = (QuartzConnector) muleContext.getRegistry().lookupConnector(connectorName);
        if (connector == null)
        {
            throw new JobExecutionException(QuartzMessages.noConnectorFound(connectorName).getMessage());
        }

        final AbstractMessageReceiver receiver = (AbstractMessageReceiver) connector.lookupReceiver(receiverKey);
        if (receiver == null)
        {
            throw new JobExecutionException(
                    QuartzMessages.noReceiverInConnector(receiverKey, connectorName).getMessage());
        }


        final EndpointPollingJobConfig jobConfig = (EndpointPollingJobConfig) jobDataMap.get(QuartzConnector.PROPERTY_JOB_CONFIG);
        if (jobConfig == null)
        {
            throw new JobExecutionException(
                    QuartzMessages.missingJobDetail(QuartzConnector.PROPERTY_JOB_CONFIG).getMessage());
        }


        try
        {
            logger.debug("Attempting to receive event on: " + jobConfig.getEndpointRef());
            ExecutionTemplate<MuleEvent> executionTemplate;
            final AtomicBoolean pollGlobalEndpoint = new AtomicBoolean(false);

            //TODO MULE-5050 work around because the builder is no longer idempotent, we now cache the endpoint instance
            InboundEndpoint endpoint = muleContext.getRegistry().lookupObject(jobConfig.getEndpointRef() + ".quartz-job");
            if(endpoint==null)
            {
                final EndpointBuilder epBuilder = muleContext.getRegistry().lookupEndpointBuilder(jobConfig.getEndpointRef());
                pollGlobalEndpoint.set(epBuilder != null);

                if (pollGlobalEndpoint.get())
                {
                    // referencing a global endpoint, fetch configuration from it
                    endpoint = epBuilder.buildInboundEndpoint();

                    //TODO MULE-5050 work around because the builder is no longer idempotent, we now cache the endpoint instance
                    muleContext.getRegistry().registerObject(jobConfig.getEndpointRef() + ".quartz-job", endpoint);
                    executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(muleContext, endpoint.getTransactionConfig(), receiver.getFlowConstruct().getExceptionListener());
                }
                else
                {
                    // a simple inline endpoint
                    executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(muleContext, new MuleTransactionConfig(), receiver.getFlowConstruct().getExceptionListener());
                }
            }
            else
            {
                executionTemplate = TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(muleContext, endpoint.getTransactionConfig(), receiver.getFlowConstruct().getExceptionListener());
            }


            final InboundEndpoint finalEndpoint = endpoint;
            ExecutionCallback<MuleEvent> cb = new ExecutionCallback<MuleEvent>()
            {
                public MuleEvent process() throws Exception
                {
                    Transaction tx = TransactionCoordination.getInstance().getTransaction();
                    if (tx != null)
                    {
                        tx.begin();
                    }

                    MuleMessage result = null;
                    if (pollGlobalEndpoint.get())
                    {
                        result = finalEndpoint.getConnector().request(finalEndpoint, jobConfig.getTimeout());
                    }
                    else
                    {
                        MuleClient client = new MuleClient(connector.getMuleContext());
                        result = client.request(jobConfig.getEndpointRef(), jobConfig.getTimeout());
                    }

                    if (result != null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Received event on: " + jobConfig.getEndpointRef());
                        }
                        if (pollGlobalEndpoint.get())
                        {
                            result.applyTransformers(null, finalEndpoint.getTransformers());
                        }

                        //we need to do this because
                        result = (MuleMessage) ((ThreadSafeAccess) result).newThreadCopy();

                        //Add the context properties to the message.
                        result.addProperties(jobDataMap, PropertyScope.INVOCATION);

                        receiver.routeMessage(result);
                    }
                    // nowhere to return
                    return null;
                }
            };

            executionTemplate.execute(cb);
        }
        catch (RuntimeException rex)
        {
            // rethrow
            throw rex;
        }
        catch (Exception e)
        {
            throw new JobExecutionException(e);
        }
    }
}
