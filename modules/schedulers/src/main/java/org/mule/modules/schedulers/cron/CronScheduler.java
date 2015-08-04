/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.cron;

import static org.mule.modules.schedulers.i18n.SchedulerMessages.couldNotCreateScheduler;
import static org.mule.modules.schedulers.i18n.SchedulerMessages.couldNotPauseSchedulers;
import static org.mule.modules.schedulers.i18n.SchedulerMessages.couldNotScheduleJob;
import static org.mule.modules.schedulers.i18n.SchedulerMessages.couldNotShutdownScheduler;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.PollingReceiverWorker;
import org.mule.transport.polling.schedule.PollScheduler;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 * <p>
 * Cron {@link org.mule.api.schedule.Scheduler} implemented with Quartz.
 * </p>
 *
 * @since 3.5.0
 */
public class CronScheduler extends PollScheduler<PollingReceiverWorker> implements MuleContextAware
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public static final String THREAD_POLL_CLASS_PROPERTY = "org.quartz.threadPool.class";
    public static final String THREAD_POLL_CLASS = "org.quartz.simpl.SimpleThreadPool";
    public static final String THREAD_POOL_COUNT_PROPERTY = "org.quartz.threadPool.threadCount";
    public static final String POLL_CRON_SCHEDULER_JOB = "poll.scheduler.job";
    public static final String QUARTZ_INSTANCE_NAME_PROPERTY = "org.quartz.scheduler.instanceName";

    /**
     * <p>
     * The Quartz scheduler. The {@link CronScheduler} is a wrapper of this instance.
     * </p>
     */
    private org.quartz.Scheduler quartzScheduler;

    /**
     * <p>
     * {@link MuleContext} used to define the thread poll size of the quarz scheduler
     * </p>
     */
    private MuleContext context;

    /**
     * <p>
     * The quartz expression written in the scheduler configuration
     * </p>
     */
    private String cronExpression;

    /**
     * <p>
     * The poll job name created in the initialization phase. (This is used to tell quartz which is the job that
     * we are managing)
     * </p>
     */
    private String jobName;

    /**
     * <p>
     * The poll job name created in the initialization phase. (This is used to tell quartz which is the job that
     * we are managing)
     * </p>
     */
    private String groupName;

    public CronScheduler(String name, PollingReceiverWorker job, String cronExpression)
    {
        super(name, job);

        this.cronExpression = cronExpression;
    }

    @Override
    public void schedule() throws Exception
    {
        quartzScheduler.triggerJob(JobKey.jobKey(jobName, groupName));
    }

    @Override
    public void dispose()
    {
        try
        {
            quartzScheduler.shutdown();
        }
        catch (SchedulerException e)
        {
            logger.error(couldNotShutdownScheduler(), e);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            quartzScheduler = createScheduler();

            jobName = job.getReceiver().getReceiverKey();
            groupName = job.getReceiver().getEndpoint().getName();

            quartzScheduler.addJob(jobDetail(jobName, groupName, job), true);

            quartzScheduler.start();
        }
        catch (SchedulerException e)
        {
            throw new InitialisationException(couldNotCreateScheduler(), e, this);
        }

    }

    @Override
    public void start() throws MuleException
    {
        try
        {
            if (quartzScheduler.isStarted())
            {
                if (quartzScheduler.getTrigger(TriggerKey.triggerKey(getName(), groupName)) == null)
                {
                    CronTrigger cronTrigger = newTrigger()
                        .withIdentity(getName(), groupName)
                        .forJob(jobName, groupName)
                        .withSchedule(cronSchedule(cronExpression))
                        .build();
                    quartzScheduler.scheduleJob(cronTrigger);
                }
                else
                {
                    quartzScheduler.resumeAll();
                }
            }

        }
        catch (SchedulerException e)
        {
            throw new DefaultMuleException(couldNotScheduleJob(), e);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        try
        {
            quartzScheduler.pauseAll();
        }
        catch (SchedulerException e)
        {
            throw new DefaultMuleException(couldNotPauseSchedulers(), e);
        }
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    private JobDetail jobDetail(String jobName, String groupName, PollingReceiverWorker job)
    {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(POLL_CRON_SCHEDULER_JOB, job);
        return newJob(CronJob.class)
                .storeDurably()
                .withIdentity(jobName, groupName)
                .usingJobData(jobDataMap)
                .build();
    }


    private Scheduler createScheduler() throws SchedulerException
    {
        SchedulerFactory factory = new StdSchedulerFactory(withFactoryProperties());

        return factory.getScheduler();
    }

    private Properties withFactoryProperties()
    {
        Properties factoryProperties = new Properties();

        factoryProperties.setProperty(QUARTZ_INSTANCE_NAME_PROPERTY, context.getConfiguration().getId() + "-" + name);
        factoryProperties.setProperty(THREAD_POLL_CLASS_PROPERTY, THREAD_POLL_CLASS);
        factoryProperties.setProperty(THREAD_POOL_COUNT_PROPERTY, String.valueOf(context.getDefaultMessageReceiverThreadingProfile().getMaxThreadsActive()));
        return factoryProperties;
    }

}
