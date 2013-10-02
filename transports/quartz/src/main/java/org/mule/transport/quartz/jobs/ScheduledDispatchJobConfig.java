/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
