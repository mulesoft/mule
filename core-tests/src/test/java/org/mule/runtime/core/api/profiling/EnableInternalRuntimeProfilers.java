/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.profiling;

import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_RUNTIME_PROFILING_CONSUMERS_ENABLEMENT_PROPERTY;
import static org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.setFeatureState;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;

import static java.util.Arrays.asList;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.List;

import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

/**
 * Enables the {@link ProfilingDataConsumer}'s as internal runtime profilers so that they will consume profiling data by default
 * without using the feature toggling mechanism.
 *
 * The {@link ProfilingDataConsumer}'s are disabled after the test.
 */
public class EnableInternalRuntimeProfilers extends SystemProperty {

  private List<ProfilingDataConsumer<?>> profilingDataConsumers = new ArrayList<>();

  public EnableInternalRuntimeProfilers(ProfilingDataConsumer<?>... profilingDataConsumers) {
    super(FORCE_RUNTIME_PROFILING_CONSUMERS_ENABLEMENT_PROPERTY, "true");
    this.profilingDataConsumers = asList(profilingDataConsumers);
  }

  @Override
  protected void after() {
    super.after();
    for (ProfilingDataConsumer<?> profilingDataConsumer : profilingDataConsumers)
      for (ProfilingEventType<?> eventType : profilingDataConsumer.getProfilingEventTypes()) {
        disableProfilingEventTypeForConsumer(eventType, profilingDataConsumer.getClass().getName());
      }
  }

  private void disableProfilingEventTypeForConsumer(ProfilingEventType<?> profilingEventType, String profilingFeatureSuffix) {
    withFeatureUser(new MuleTogglzArtifactFeatureUser(""), () -> {
      Feature feature =
          FEATURE_PROVIDER.getOrRegisterProfilingTogglzFeatureFrom(profilingEventType, profilingFeatureSuffix);
      setFeatureState(new FeatureState(feature, false));
    });
  }

}
