/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
