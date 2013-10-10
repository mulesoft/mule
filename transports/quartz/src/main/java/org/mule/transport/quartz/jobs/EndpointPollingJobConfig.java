/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.transport.quartz.config.AbstractJobConfig;

import org.quartz.Job;
import org.quartz.StatefulJob;

/**
 * Configuration for the {@link EndpointPollingJob} job.
 */
public class EndpointPollingJobConfig extends AbstractJobConfig
{
    private String endpointRef;

    //TODO this should use th system default
    private int timeout = 5000;

    public String getEndpointRef()
    {
        return endpointRef;
    }

    public void setEndpointRef(String endpointRef)
    {
        this.endpointRef = endpointRef;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    @Override
    protected Class<? extends StatefulJob> getStatefulJobClass()
    {
        return StatefulEndpointPollingJob.class;
    }

    @Override
    protected Class<? extends Job> getStatelessJobClass()
    {
        return EndpointPollingJob.class;
    }
}
