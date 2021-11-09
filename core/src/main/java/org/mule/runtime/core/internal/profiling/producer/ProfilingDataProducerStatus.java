/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.core.internal.config.FeatureFlaggingUtils;
import org.mule.runtime.core.internal.config.togglz.MuleTogglzProfilingFeature;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The status of a {@link org.mule.runtime.api.profiling.ProfilingDataProducer} according to the requirements from the
 * {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
 */
public class ProfilingDataProducerStatus {

  private final Set<FeatureState> profilingFeaturesStates;

  public ProfilingDataProducerStatus(Collection<MuleTogglzProfilingFeature> profilingFeatures) {
    this.profilingFeaturesStates = new HashSet<>();
    for (Feature feature : profilingFeatures) {
      profilingFeaturesStates.add(FeatureFlaggingUtils.getFeatureState(feature));
    }
  }

  public boolean isEnabled() {
    return profilingFeaturesStates.stream().anyMatch(profilingFeatureState -> profilingFeatureState.isEnabled());
  }
}
