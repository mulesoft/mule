/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.provider;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.feature.internal.togglz.MuleTogglzProfilingFeature;
import org.mule.runtime.feature.internal.togglz.MuleTogglzRuntimeFeature;
import org.togglz.core.Feature;
import org.togglz.core.spi.FeatureProvider;

/**
 * Implementation of {@link FeatureProvider} for the Mule Runtime.
 *
 * @since 4.5.0
 */
public interface MuleTogglzFeatureProvider extends FeatureProvider {

  /**
   * Gets a {@link Feature} associated with a {@link org.mule.runtime.api.config.Feature}
   *
   * @param feature the runtime {@link org.mule.runtime.api.config.Feature}
   * @return the registered Togglz {@link Feature}
   */
  Feature getRuntimeTogglzFeature(org.mule.runtime.api.config.Feature feature);

  /**
   * Gets a Togglz feature associated with a {@link org.mule.runtime.api.config.Feature}
   *
   * @param feature the runtime {@link org.mule.runtime.api.config.Feature}
   * @return the registered {@link Feature}. If the feature is already registered it returns the corresponding togglz feature.
   */
  MuleTogglzRuntimeFeature getOrRegisterRuntimeTogglzFeatureFrom(org.mule.runtime.api.config.Feature feature);

  /**
   * Registers a Togglz {@link Feature} associated to a {@link ProfilingEventType} and a
   * {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
   *
   * @param profilingEventType the {@link ProfilingEventType}.
   * @param consumerName       the id associated to a {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
   * @return the registered {@link Feature}
   */
  MuleTogglzProfilingFeature getOrRegisterProfilingTogglzFeatureFrom(ProfilingEventType<?> profilingEventType,
                                                                     String consumerName);

  /**
   * returns a {@link Feature} by name
   *
   * @param featureName the name for the feature
   * @return the coresponding {@link Feature}
   */
  Feature getFeature(String featureName);

}
