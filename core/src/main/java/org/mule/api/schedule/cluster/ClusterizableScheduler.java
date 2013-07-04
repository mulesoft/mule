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
public interface ClusterizableScheduler extends Scheduler
{

}
