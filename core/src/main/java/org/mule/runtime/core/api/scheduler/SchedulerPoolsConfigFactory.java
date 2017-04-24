/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides an instance of {@link SchedulerPoolsConfig} to use when building the infrastructure for the {@link Scheduler}s.
 * <p>
 * Implementations may return an empty config on {@link #get()}, which indicates that it is the responsability ig the caller to
 * provide the configuration.
 * 
 * @since 4.0
 */
public interface SchedulerPoolsConfigFactory extends Supplier<Optional<SchedulerPoolsConfig>> {


}
