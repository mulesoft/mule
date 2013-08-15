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

import org.mule.api.schedule.Scheduler;

/**
 * <p>
 * Marker interface to define a {@link Scheduler} that runs synchronized in a HA environment.
 * </p>
 * <p>
 * All the {@link Scheduler} that are marked as {@link ClusterizableScheduler} are post processed after created by
 * the {@link org.mule.api.schedule.SchedulerFactory} and are wrapped into a {@link ClusterizableSchedulerWrapper}
 * automatically by the {@link ClusterizableFactoryPostProcessor}
 * </p>
 *
 * @since 3.5.0
 */
public interface ClusterizableScheduler<T> extends Scheduler<T>
{

}
