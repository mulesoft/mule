/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.i18n.QuartzMessages;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Extracts the Job object to invoke from the context. The Job itself can be
 * scheduled by dispatching an event over a quartz endpoint. The job can either be
 * set as a property on the event (this property can be a container reference or the
 * actual job object) or the payload of the event can be the Job (in which case when
 * the job is fired it will have a NullPayload)
 * 
 * @see org.mule.transport.NullPayload
 */
public class CustomJob extends AbstractJob
{
    protected void doExecute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Object tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_OBJECT);
        if (tempJob == null)
        {
            tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_REF);
            if (tempJob == null)
            {
                throw new JobExecutionException(QuartzMessages.invalidPayloadType().getMessage());
            }
            else
            {
                tempJob = muleContext.getRegistry().lookupObject((String) tempJob);
                if (tempJob == null)
                {
                    throw new JobExecutionException("Job not found: " + tempJob);
                }
                if (!(tempJob instanceof Job))
                {
                    throw new JobExecutionException(QuartzMessages.invalidJobObject().getMessage());
                }
            }
        }
        else if (!(tempJob instanceof Job))
        {
            throw new JobExecutionException(QuartzMessages.invalidJobObject().toString());
        }
        ((Job)tempJob).execute(jobExecutionContext);
    }
}
