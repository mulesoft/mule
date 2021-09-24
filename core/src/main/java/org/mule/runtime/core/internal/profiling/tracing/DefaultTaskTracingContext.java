/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import org.mule.runtime.api.profiling.tracing.ComponentMetadata;
import org.mule.runtime.api.profiling.tracing.TaskTracingContext;

import java.util.Optional;

public class DefaultTaskTracingContext implements TaskTracingContext {

  private ComponentMetadata componentMetadata;

  public DefaultTaskTracingContext(ComponentMetadata componentMetadata) {
    this.componentMetadata = componentMetadata;
  }

  @Override
  public Optional<ComponentMetadata> getRunningComponentMetadata() {
    return Optional.ofNullable(componentMetadata);
  }
}
