/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.activation;

import org.mule.runtime.feature.internal.togglz.activation.strategies.MuleTogglzActivatedIfEnabledActivationStrategy;
import org.togglz.core.activation.ActivationStrategyProvider;
import org.togglz.core.spi.ActivationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

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
    addSpiActivationStrategies();
    addMuleActivationStrategies();

    return strategies;
  }

  private void addMuleActivationStrategies() {
    strategies.add(MuleTogglzActivatedIfEnabledActivationStrategy.getInstance());
  }

  private void addSpiActivationStrategies() {
    for (ActivationStrategy activationStrategy : ServiceLoader.load(ActivationStrategy.class)) {
      strategies.add(activationStrategy);
    }
  }
}
