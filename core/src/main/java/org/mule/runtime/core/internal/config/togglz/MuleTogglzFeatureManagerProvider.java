/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.togglz;

import org.mule.runtime.core.internal.config.togglz.feature.provider.DefaultMuleTogglzFeatureProvider;
import org.mule.runtime.core.internal.config.togglz.state.MuleTogglzFeatureStateRepository;
import org.mule.runtime.core.internal.config.management.MuleTogglzProfilingFeatures;

import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import static org.mule.runtime.core.internal.config.togglz.activation.MuleTogglzActivationStrategyProvider.getDefaultActivationStrategyProvider;

/**
 * a {@link FeatureManagerProvider} for the Mule Runtime.
 */
public class MuleTogglzFeatureManagerProvider implements FeatureManagerProvider {

  public static DefaultMuleTogglzFeatureProvider FEATURE_PROVIDER =
      new DefaultMuleTogglzFeatureProvider(MuleTogglzProfilingFeatures.class);

  private static FeatureManager FEATURE_MANAGER = new FeatureManagerBuilder()
      .featureProvider(FEATURE_PROVIDER)
      .stateRepository(new MuleTogglzFeatureStateRepository(FEATURE_PROVIDER))
      .userProvider(new ThreadLocalUserProvider())
      .activationStrategyProvider(getDefaultActivationStrategyProvider())
      .build();


  @Override
  public FeatureManager getFeatureManager() {
    return FEATURE_MANAGER;
  }

  @Override
  public int priority() {
    return 30;
  }
}
