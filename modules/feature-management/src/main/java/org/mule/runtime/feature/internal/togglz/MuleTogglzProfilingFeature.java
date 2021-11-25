/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz;

import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFullyQualifiedProfilingEventTypeFeatureIdentifier;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.togglz.core.Feature;

import java.util.Objects;

/**
 * A Togglz {@link Feature} associated to profiling.
 * <p>
 * When {@link org.mule.runtime.api.profiling.ProfilingDataConsumer} is discovered a feature associated to each of the
 * {@link ProfilingEventType}'s it listens to is registered. In this way, we can enable, disable this features in runtime and the
 * profiling data is not generated in case no data consumers listens to it.
 *
 * @since 4.5.0
 */
public class MuleTogglzProfilingFeature implements Feature {

  private final ProfilingEventType<?> profilingEventType;
  private final String consumerName;

  public MuleTogglzProfilingFeature(ProfilingEventType<?> profilingEventType, String consumerIdentifier) {
    this.profilingEventType = profilingEventType;
    this.consumerName = consumerIdentifier;
  }

  @Override
  public String name() {
    return getFullyQualifiedProfilingEventTypeFeatureIdentifier(profilingEventType, consumerName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MuleTogglzProfilingFeature that = (MuleTogglzProfilingFeature) o;
    return Objects.equals(profilingEventType, that.profilingEventType) && Objects
        .equals(consumerName, that.consumerName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profilingEventType, consumerName);
  }
}
