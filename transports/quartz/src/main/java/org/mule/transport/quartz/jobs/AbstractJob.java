/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.transport.quartz.QuartzConnector;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;

/**
 * A superclass for Quartz jobs.
 */
public abstract class AbstractJob implements Job
{
    protected MuleContext muleContext;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        muleContext = getMuleContext(jobExecutionContext);
        if (muleContext.isPrimaryPollingInstance() ||
            jobExecutionContext.getJobDetail().getJobDataMap().get(QuartzConnector.PROPERTY_JOB_DYNAMIC) == Boolean.TRUE)
        {
            doExecute(jobExecutionContext);
        }
    }

    /**
     * Execute the job.
     */
    protected abstract void doExecute(JobExecutionContext jobExecutionContext) throws JobExecutionException;

    protected MuleContext getMuleContext(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        try
        {
            SchedulerContext schedulerContext = jobExecutionContext.getScheduler().getContext();
            return (MuleContext) schedulerContext.get(MuleProperties.MULE_CONTEXT_PROPERTY);
        }
        catch (SchedulerException e)
        {
            throw new JobExecutionException("Failed to retrieve MuleContext from the Scheduler Context: " + e.getMessage(), e);
        }
    }
}
