/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilerDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;

import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Simple {@link ProfilerDataConsumerDiscoveryStrategy} that programmatically generates the data consumers.
 *
 * @since 4.4
 */
public class DefaultProfilerDataConsumerDiscoveryStrategy implements ProfilerDataConsumerDiscoveryStrategy {

  @Override
  public <S extends ProfilingDataConsumer<T>, T extends ProfilingEventContext> Set<S> discover() {
    // No data consumers.
    return emptySet();
  }

}
