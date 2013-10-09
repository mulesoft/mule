/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractPollingMessageReceiver;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PollEndpointJob extends AbstractJob implements Lifecycle, MuleContextAware
{
    private String inboundPollingEndpointName;
    private AbstractPollingMessageReceiver receiver;
    
    public PollEndpointJob(String inboundPollingEndpointName)
    {
        this.inboundPollingEndpointName = inboundPollingEndpointName;
    }

    protected void doExecute(JobExecutionContext context) throws JobExecutionException
    {
        try
        {
            receiver.performPoll();
        }
        catch (Exception e)
        {
            throw new JobExecutionException(e);
        }
        
    }

    public void initialise() throws InitialisationException
    {
        //DO NOTHING
    }

    public void start() throws MuleException
    {
        InboundEndpoint endpoint = (InboundEndpoint) muleContext.getRegistry().lookupObject(this.inboundPollingEndpointName);
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        receiver = (AbstractPollingMessageReceiver) connector.getReceiver(null, endpoint);
        receiver.disableNativeScheduling();
    }

    public void stop() throws MuleException
    {
        //DO NOTHING
    }

    public void dispose()
    {
        //DO NOTHING
    }

    @Override
    protected MuleContext getMuleContext(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        return muleContext;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
