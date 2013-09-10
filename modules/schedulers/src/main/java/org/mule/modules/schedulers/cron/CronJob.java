/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import org.mule.transport.PollingReceiverWorker;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * <p>
 * {@link StatefulJob} for polling. This is always stateful as the synchronous processing strategy is defined by
 * the flow of the poll.
 * </p>
 *
 * @since 3.5.0
 */
public class CronJob implements StatefulJob
{

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {

        PollingReceiverWorker work = (PollingReceiverWorker) getSchedulerWork(context);
        if (work != null)
        {
            work.run();
        }

    }

    private Object getSchedulerWork(JobExecutionContext context)
    {
        return context.getJobDetail().getJobDataMap().get(CronScheduler.POLL_CRON_SCHEDULER_JOB);
    }
}
