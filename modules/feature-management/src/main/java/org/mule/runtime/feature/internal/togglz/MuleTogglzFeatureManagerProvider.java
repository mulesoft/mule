/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz;

import org.mule.runtime.feature.internal.togglz.config.MuleHotSwitchProfilingFeatures;
import org.mule.runtime.feature.internal.togglz.provider.DefaultMuleTogglzFeatureProvider;
import org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureStateRepository;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.spi.FeatureManagerProvider;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import static org.mule.runtime.feature.internal.togglz.activation.MuleTogglzActivationStrategyProvider.getDefaultActivationStrategyProvider;

/**
 * a {@link FeatureManagerProvider} for the Mule Runtime. This is retrieved through SPI.
 *
 * @since 4.5.0
 */
public class MuleTogglzFeatureManagerProvider implements FeatureManagerProvider {

  public static final int PRIORITY = 30;
  public static DefaultMuleTogglzFeatureProvider FEATURE_PROVIDER =
      new DefaultMuleTogglzFeatureProvider(MuleHotSwitchProfilingFeatures.class);

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
    // Not used. By default.
    return PRIORITY;
  }
}
