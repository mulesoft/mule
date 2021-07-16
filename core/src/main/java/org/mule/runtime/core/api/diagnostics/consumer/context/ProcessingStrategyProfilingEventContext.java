/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.consumer.context;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.diagnostics.ProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Optional;

/**
 * General context for processing strategy profiling data.
 *
 * @since 4.4
 */
@Experimental
public interface ProcessingStrategyProfilingEventContext extends ProfilingEventContext {

  /**
   * @return the {@link CoreEvent} associated with the profiling event.
   */
  CoreEvent getEvent();

  /**
   * @return the thread name of the profiling event.
   */
  String getThreadName();

  /**
   * @return the artifact id of the profiling event.
   */
  String getArtifactId();

  /**
   * @return the artifact type of the profiling event.
   */
  String getArtifactType();

  /**
   * @return the optional {@link ComponentLocation} associated with the profiling event.
   */
  Optional<ComponentLocation> getLocation();

}
