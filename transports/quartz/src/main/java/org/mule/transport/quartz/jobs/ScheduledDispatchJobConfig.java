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
 * The configuration for the {@link ScheduledDispatchJob} job.
 */
public class ScheduledDispatchJobConfig extends AbstractJobConfig
{
    /** 
     * The endpoint ref has to be a string and not a reference to the actual endpoint because
     * jobs can get stored in a database 
     */
    private String endpointRef;

    public String getEndpointRef()
    {
        return endpointRef;
    }

    public void setEndpointRef(String endpointRef)
    {
        this.endpointRef = endpointRef;
    }

    @Override
    protected Class<? extends StatefulJob> getStatefulJobClass()
    {
        return StatefulScheduledDispatchJob.class;
    }

    @Override
    protected Class<? extends Job> getStatelessJobClass()
    {
        return ScheduledDispatchJob.class;
    }
}
