/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import org.mule.runtime.api.component.location.Location;

import java.util.concurrent.Callable;

/**
 * Evaluates a {@link Callable} on a lazy configuration.
 *
 * @since 4.0
 */
public interface LazyComponentTaskExecutor {

  /**
   * Calling this method guarantees that the requested component with its dependencies from the configuration will be created, initialized and
   * after the task is completed the requested component and dependencies will be disposed.
   * <p/>
   * The requested component must exists in the configuration.
   *
   * @param location the location of the configuration component.
   * @throws E if there's a problem executing the task.
   */
  <T, E extends Exception> T withContext(Location location, Callable<T> callable) throws E;

}
