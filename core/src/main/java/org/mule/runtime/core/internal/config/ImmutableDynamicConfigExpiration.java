/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
