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
