/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.scheduler;

import static java.util.Optional.empty;

import java.util.Optional;

/**
 * Singleton implementation whose {@link #getConfig()} method returns an empty value, to indicate that the container pools have to
 * be used.
 * 
 * @since 4.0
 */
public class SchedulerContainerPoolsConfig implements SchedulerPoolsConfigFactory {

  private static final SchedulerContainerPoolsConfig INSTANCE = new SchedulerContainerPoolsConfig();

  public static SchedulerContainerPoolsConfig getInstance() {
    return INSTANCE;
  }

  private SchedulerContainerPoolsConfig() {
    // Nothing to do
  }

  @Override
  public Optional<SchedulerPoolsConfig> getConfig() {
    return empty();
  }
}
