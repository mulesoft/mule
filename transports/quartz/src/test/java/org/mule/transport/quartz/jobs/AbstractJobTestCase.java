/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.MULE_CONTEXT_PROPERTY;
import static org.mule.transport.quartz.QuartzConnector.PROPERTY_JOB_DYNAMIC;

import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;

/**
 * MULE-9019
 * These tests use a {@code new Boolean(true)} in the data map instead of {@code Boolean.TRUE} or directly {@code true} to simulate the deserialization of that value object.
 */
@SmallTest
public class AbstractJobTestCase extends AbstractMuleTestCase
{
    private MuleContext muleContext = mock(MuleContext.class);

    private JobExecutionContext quartzJobContext = mock(JobExecutionContext.class);
    private JobDetail quartzJobDetail = mock(JobDetail.class);
    private Scheduler quartzScheduler = mock(Scheduler.class);
    private SchedulerContext quartzSchedulerContext = new SchedulerContext();
    private JobDataMap quartzJobDataMap = new JobDataMap();

    private AbstractJob job = mock(AbstractJob.class);

    @Before
    public void before() throws SchedulerException
    {
        when(quartzJobDetail.getJobDataMap()).thenReturn(quartzJobDataMap);
        when(quartzJobContext.getJobDetail()).thenReturn(quartzJobDetail);

        quartzSchedulerContext.put(MULE_CONTEXT_PROPERTY, muleContext);
        when(quartzScheduler.getContext()).thenReturn(quartzSchedulerContext);
        when(quartzJobContext.getScheduler()).thenReturn(quartzScheduler);

        doCallRealMethod().when(job).execute(any(JobExecutionContext.class));
        doCallRealMethod().when(job).getMuleContext(any(JobExecutionContext.class));
    }

    @Test
    public void pollingPrimaryJobDynamic() throws JobExecutionException
    {
        quartzJobDataMap.put(PROPERTY_JOB_DYNAMIC, new Boolean(true));
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);

        job.execute(quartzJobContext);

        verify(job).doExecute(quartzJobContext);
    }

    @Test
    public void pollingPrimaryJobNotDynamic() throws JobExecutionException
    {
        quartzJobDataMap.put(PROPERTY_JOB_DYNAMIC, new Boolean(false));
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);

        job.execute(quartzJobContext);

        verify(job).doExecute(quartzJobContext);
    }

    @Test
    public void pollingPrimaryJobDynamicNull() throws JobExecutionException
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);

        job.execute(quartzJobContext);

        verify(job).doExecute(quartzJobContext);
    }

    @Test
    public void pollingNotPrimaryJobDynamic() throws JobExecutionException
    {
        quartzJobDataMap.put(PROPERTY_JOB_DYNAMIC, new Boolean(true));
        when(muleContext.isPrimaryPollingInstance()).thenReturn(false);

        job.execute(quartzJobContext);

        verify(job).doExecute(quartzJobContext);
    }

    @Test
    public void pollingNotPrimaryJobNotDynamic() throws JobExecutionException
    {
        quartzJobDataMap.put(PROPERTY_JOB_DYNAMIC, new Boolean(false));
        when(muleContext.isPrimaryPollingInstance()).thenReturn(false);

        job.execute(quartzJobContext);

        verify(job, never()).doExecute(quartzJobContext);
    }

    @Test
    public void pollingNotPrimaryJobDynamicNull() throws JobExecutionException
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(false);

        job.execute(quartzJobContext);

        verify(job, never()).doExecute(quartzJobContext);
    }

}
