/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.time.Time;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;

/**
 * Immutable implementation of {@link DynamicConfigExpiration}
 *
 * @since 1.0
 */
public class ImmutableDynamicConfigExpiration extends AbstractComponent implements DynamicConfigExpiration {

  private final Time frequency;
  private final ExpirationPolicy expirationPolicy;

  public ImmutableDynamicConfigExpiration(Time frequency, ExpirationPolicy expirationPolicy) {
    this.frequency = frequency;
    this.expirationPolicy = expirationPolicy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Time getFrequency() {
    return frequency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExpirationPolicy getExpirationPolicy() {
    return expirationPolicy;
  }
}
