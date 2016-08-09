/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.infrastructure;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.time.Time;

import java.util.concurrent.TimeUnit;

/**
 * An {@link ObjectFactory} which produces instances of {@link DynamicConfigurationExpiration}
 */
public class DynamicConfigurationExpirationObjectFactory implements ObjectFactory<DynamicConfigurationExpiration> {

  private final long frequency;
  private final TimeUnit timeUnit;

  public DynamicConfigurationExpirationObjectFactory(long frequency, TimeUnit timeUnit) {
    this.frequency = frequency;
    this.timeUnit = timeUnit;
  }

  @Override
  public DynamicConfigurationExpiration getObject() throws Exception {
    return new DynamicConfigurationExpiration(new Time(frequency, timeUnit));
  }
}
