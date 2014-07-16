/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactory;
import org.mule.transport.PollingReceiverWorker;


/**
 * <p>
 * Factory of the Cron Scheduler for poll
 * </p>
 *
 * @since 3.5.0
 */
public class CronSchedulerFactory extends SchedulerFactory<PollingReceiverWorker>
{

    private String expression;

    @Override
    protected Scheduler doCreate(String name, PollingReceiverWorker job)
    {
        CronScheduler cronScheduler = new CronScheduler(name, job, expression);
        cronScheduler.setMuleContext(context);
        return cronScheduler;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
