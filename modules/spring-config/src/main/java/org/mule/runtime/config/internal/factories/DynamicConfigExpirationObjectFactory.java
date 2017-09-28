/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.api.time.Time;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.core.internal.config.ImmutableDynamicConfigExpiration;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * An {@link ObjectFactory} which produces instances of {@link DynamicConfigExpiration}
 */
public class DynamicConfigExpirationObjectFactory extends AbstractComponentFactory<DynamicConfigExpiration> {

  @Inject
  private TimeSupplier timeSupplier;

  private final long frequency;
  private final TimeUnit timeUnit;
  private ExpirationPolicy expirationPolicy;

  public DynamicConfigExpirationObjectFactory(long frequency, TimeUnit timeUnit) {
    this.frequency = frequency;
    this.timeUnit = timeUnit;
  }

  @Override
  public DynamicConfigExpiration doGetObject() throws Exception {
    return new ImmutableDynamicConfigExpiration(new Time(frequency, timeUnit), getExpirationPolicy());
  }

  public void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
    this.expirationPolicy = expirationPolicy;
  }

  private ExpirationPolicy getExpirationPolicy() {
    if (expirationPolicy != null) {
      return expirationPolicy;
    }

    return ImmutableExpirationPolicy.getDefault(timeSupplier);
  }
}
