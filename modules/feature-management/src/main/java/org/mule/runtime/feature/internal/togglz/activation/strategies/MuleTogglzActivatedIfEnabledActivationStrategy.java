/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.activation.strategies;

import org.togglz.core.activation.Parameter;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.spi.ActivationStrategy;
import org.togglz.core.user.FeatureUser;

/**
 * An {@link ActivationStrategy} that depends on enablement for activation. Basically it does nothing at the activation
 * verification.
 *
 * @see <a href="http://google.com">https://www.togglz.org/documentation/activation-strategies.html/a>
 *
 * @since 4.5.0
 */
public class MuleTogglzActivatedIfEnabledActivationStrategy implements ActivationStrategy {

  public static final String ID = "ACTIVATED-IF-ENABLED";
  private static final MuleTogglzActivatedIfEnabledActivationStrategy INSTANCE =
      new MuleTogglzActivatedIfEnabledActivationStrategy();

  public static MuleTogglzActivatedIfEnabledActivationStrategy getInstance() {
    return INSTANCE;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Activated if Enabled Activation Strategy";
  }

  @Override
  public boolean isActive(FeatureState featureState, FeatureUser featureUser) {
    return featureState.isEnabled();
  }

  @Override
  public Parameter[] getParameters() {
    return new Parameter[0];
  }

}
