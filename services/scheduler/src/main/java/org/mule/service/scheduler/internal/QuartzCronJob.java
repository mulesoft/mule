/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
public class QuartzCronJob implements Job {

  public static final String JOB_TASK_KEY = QuartzCronJob.class.getName() + ".task";

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    Runnable task = (Runnable) context.getJobDetail().getJobDataMap().get(JOB_TASK_KEY);


    // ((CronTrigger)( context.getTrigger())).getTimeZone();

    task.run();
  }

}
