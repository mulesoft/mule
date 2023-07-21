/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
