/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import static java.lang.Boolean.TRUE;
import static org.mule.api.config.MuleProperties.MULE_CONTEXT_PROPERTY;
import static org.mule.transport.quartz.QuartzConnector.PROPERTY_JOB_DYNAMIC;

import org.mule.api.MuleContext;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
            TRUE.equals(jobExecutionContext.getJobDetail().getJobDataMap().get(PROPERTY_JOB_DYNAMIC)))
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
            return (MuleContext) jobExecutionContext.getScheduler().getContext().get(MULE_CONTEXT_PROPERTY);
        }
        catch (SchedulerException e)
        {
            throw new JobExecutionException("Failed to retrieve MuleContext from the Scheduler Context: " + e.getMessage(), e);
        }
    }
}
