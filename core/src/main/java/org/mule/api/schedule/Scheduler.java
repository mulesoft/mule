/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.schedule;

import org.mule.api.NameableObject;
import org.mule.api.lifecycle.Lifecycle;

/**
 * <p>
 * An scheduler is a class that arrange jobs in a define schedule. Once the Scheduler starts it launch a thread that
 * triggers an action, when stopped, that action is not executed anymore, unless it is scheduled on demand by
 * the method {@link org.mule.api.schedule.Scheduler#schedule()}
 * </p>
 * <p>
 * The initialization of a Scheduler is thought to be the stage where it reserves resources to schedule. While the
 * dispose phase is thought to be the stage where those resources are released.
 * </p>
 * <p>
 * This interface implements {@link NameableObject} the name of the Scheduler is used as its identifier in the
 * {@link org.mule.api.registry.MuleRegistry}
 * </p>
 * <p>
 * The recommended way to create a Scheduler is by a {@link SchedulerFactory}, this will allow other users to hook the
 * Scheduler creation. This can me omitted if this is not desired.
 * </p>
 * <p>
 * Each scheduler is thought to schedule a single job. Check the {@link SchedulerFactory#create(String, Object)}  to see the
 * creation of the scheduler for more details.
 * </p>
 *
 * @since 3.5.0
 */
public interface Scheduler extends Lifecycle, NameableObject
{

    /**
     * <p>
     * Launches the action asynchronously. The time can or can't be immediate depending on the Scheduler implementation.
     * </p>
     * <p>
     * By default,  schedulers users can use a scheduler not matter if it is started or stopped. By default the
     * Scheduler implementation should schedule a job regardless of it lifecycle state. If this is not accepted then
     * check Scheduler state and throw exception.
     * </p>
     *
     * @throws Exception If the job could not be scheduled.
     */
    void schedule() throws Exception;


}
