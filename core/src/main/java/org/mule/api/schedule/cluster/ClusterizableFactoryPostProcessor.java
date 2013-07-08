/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.schedule.cluster;

import org.mule.api.MuleContext;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;

/**
 * <p>
 * Wraps a {@link ClusterizableScheduler} into a {@link ClusterizableSchedulerWrapper} in the post process stage of
 * the {@link org.mule.api.schedule.SchedulerFactory}
 * </p>
 *
 * @since 3.5.0
 */
public class ClusterizableFactoryPostProcessor implements SchedulerFactoryPostProcessor
{

    /**
     * <p>
     * The {@link MuleContext} must never be null
     * </p>
     */
    private MuleContext context;

    public ClusterizableFactoryPostProcessor(MuleContext context)
    {
        this.context = context;
    }

    /**
     * <p>
     * If the {@link Scheduler} is a {@link ClusterizableScheduler} then it is warped into a {@link ClusterizableSchedulerWrapper}
     * </p>
     *
     * @param scheduler The {@link Scheduler} that was created by the {@SchedulerFactory}. It will never be Null.
     * @return A modified instance of the {@link Scheduler}
     */
    @Override
    public Scheduler process(Scheduler scheduler)
    {
        if (scheduler instanceof ClusterizableScheduler)
        {
            return new ClusterizableSchedulerWrapper(context, (ClusterizableScheduler) scheduler);
        }

        return scheduler;
    }
}
