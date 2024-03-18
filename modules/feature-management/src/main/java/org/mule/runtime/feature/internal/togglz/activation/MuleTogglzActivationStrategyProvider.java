/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.activation;

import org.mule.runtime.feature.internal.togglz.activation.strategies.MuleTogglzActivatedIfEnabledActivationStrategy;

import java.util.ArrayList;
import java.util.List;

import org.togglz.core.activation.ActivationStrategyProvider;
import org.togglz.core.activation.GradualActivationStrategy;
import org.togglz.core.activation.ReleaseDateActivationStrategy;
import org.togglz.core.activation.ScriptEngineActivationStrategy;
import org.togglz.core.activation.ServerIpActivationStrategy;
import org.togglz.core.activation.SystemPropertyActivationStrategy;
import org.togglz.core.activation.UserRoleActivationStrategy;
import org.togglz.core.activation.UsernameActivationStrategy;
import org.togglz.core.spi.ActivationStrategy;

/**
 * {@link ActivationStrategyProvider} for the Mule Runtime. It provides the @{@link ActivationStrategy}'s found through SPI in the
 * current TCCL as well the mule runtime activation strategies.
 *
 * @since 4.5.0
 */
public class MuleTogglzActivationStrategyProvider implements ActivationStrategyProvider {

  private static final MuleTogglzActivationStrategyProvider INSTANCE = new MuleTogglzActivationStrategyProvider();

  private final List<ActivationStrategy> strategies = new ArrayList<>();

  public static ActivationStrategyProvider getDefaultActivationStrategyProvider() {
    return INSTANCE;
  }

  @Override
  public List<ActivationStrategy> getActivationStrategies() {
    addTogglzActivationStrategies();
    addMuleActivationStrategies();

    return strategies;
  }

  private void addTogglzActivationStrategies() {
    strategies.add(new UsernameActivationStrategy());
    strategies.add(new GradualActivationStrategy());
    strategies.add(new ScriptEngineActivationStrategy());
    strategies.add(new ReleaseDateActivationStrategy());
    strategies.add(new ServerIpActivationStrategy());
    strategies.add(new UserRoleActivationStrategy());
    strategies.add(new SystemPropertyActivationStrategy());
  }

  private void addMuleActivationStrategies() {
    strategies.add(MuleTogglzActivatedIfEnabledActivationStrategy.getInstance());
  }
}
