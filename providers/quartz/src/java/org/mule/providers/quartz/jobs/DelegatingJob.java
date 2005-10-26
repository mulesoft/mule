/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.quartz.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobDataMap;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.config.i18n.Message;
import org.mule.MuleManager;
import org.mule.providers.quartz.QuartzConnector;

/**
 * Extracts the Job object to invoke from the context.  The Job itself can be scheduled by
 * dispatching an event over a quartz endpoint.  The job can either be set as a property on
 * the event (this property can be a container reference or the actual job object) or the payload
 * of the event can be the Job (in which case when the job is fired it will have a NullPayload)
 *
 * @see org.mule.providers.NullPayload
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DelegatingJob implements Job 
{
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Object tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_OBJECT);
        if (tempJob == null) {
            tempJob = jobDataMap.get(QuartzConnector.PROPERTY_JOB_REF);
            if (tempJob == null) {
                throw new JobExecutionException(new Message("quartz", 2).getMessage());
            } else {
                try {
                    tempJob = MuleManager.getInstance().getContainerContext().getComponent(tempJob);
                } catch (ObjectNotFoundException e) {
                    throw new JobExecutionException(e);
                }
                if (!(tempJob instanceof Job)) {
                    throw new JobExecutionException(new Message("quartz", 3).getMessage());
                }
            }
        } else if (!(tempJob instanceof Job)) {
            throw new JobExecutionException(new Message("quartz", 3).toString());
        }
        ((Job)tempJob).execute(jobExecutionContext);
    }
}
