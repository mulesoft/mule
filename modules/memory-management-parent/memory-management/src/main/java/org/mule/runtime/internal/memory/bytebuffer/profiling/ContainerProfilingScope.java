/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.internal.memory.bytebuffer.profiling;

import static org.mule.runtime.api.profiling.ProfilingProducerScopeType.CONTAINER_SCOPE_TYPE;

import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.ProfilingProducerScopeType;

/**
 * A {@link ProfilingProducerScope} tht represents the whole container
 */
public class ContainerProfilingScope implements ProfilingProducerScope {

  @Override
  public String getProducerScopeIdentifier() {
    return "";
  }

  @Override
  public ProfilingProducerScopeType getProducerScopeTypeIdentifier() {
    return CONTAINER_SCOPE_TYPE;
  }
}
