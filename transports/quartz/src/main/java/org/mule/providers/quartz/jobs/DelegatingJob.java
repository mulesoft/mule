/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz.jobs;

import org.mule.RegistryContext;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.providers.quartz.i18n.QuartzMessages;

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
 * @see org.mule.providers.NullPayload
 */
public class DelegatingJob implements Job
{
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
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
                tempJob = RegistryContext.getRegistry().lookupObject((String) tempJob);
                if(tempJob==null)
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
