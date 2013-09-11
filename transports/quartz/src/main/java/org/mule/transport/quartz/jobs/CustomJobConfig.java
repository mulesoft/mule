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
 * This configuration simply holds a reference to a user defined job to execute.
 */
public class CustomJobConfig extends AbstractJobConfig
{
    private Job job;

    public Job getJob()
    {
        return job;
    }

    public void setJob(Job job)
    {
        this.job = job;
        setStateful(job instanceof StatefulJob);
    }

    @Override
    protected Class<? extends StatefulJob> getStatefulJobClass()
    {
        return StatefulCustomJob.class;
    }

    @Override
    protected Class<? extends Job> getStatelessJobClass()
    {
        return CustomJob.class;
    }
}
