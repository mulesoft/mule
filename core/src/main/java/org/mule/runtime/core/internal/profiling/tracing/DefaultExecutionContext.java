/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.profiling.tracing.ComponentMetadata;
import org.mule.runtime.api.profiling.tracing.ExecutionContext;

import java.util.Optional;

/**
 * Immutable implementation of {@link ExecutionContext}
 */
public class DefaultExecutionContext implements ExecutionContext {

  private final ComponentMetadata componentMetadata;

  public DefaultExecutionContext(ComponentMetadata componentMetadata) {
    this.componentMetadata = componentMetadata;
  }

  @Override
  public Optional<ComponentMetadata> getCurrentComponentMetadata() {
    return ofNullable(componentMetadata);
  }
}
