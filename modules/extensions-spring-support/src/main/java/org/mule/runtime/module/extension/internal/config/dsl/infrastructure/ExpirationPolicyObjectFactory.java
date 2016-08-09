/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.infrastructure;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.ImmutableExpirationPolicy;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * An {@link ObjectFactory} which produces instances of {@link ExpirationPolicy}
 *
 * @since 4.0
 */
public class ExpirationPolicyObjectFactory implements ObjectFactory<ExpirationPolicy> {

  @Inject
  private TimeSupplier timeSupplier;

  private Long maxIdleTime = null;
  private TimeUnit timeUnit = null;

  @Override
  public ExpirationPolicy getObject() throws Exception {
    if (maxIdleTime != null && timeUnit != null) {
      return new ImmutableExpirationPolicy(maxIdleTime, timeUnit, timeSupplier);
    }

    return ImmutableExpirationPolicy.getDefault(timeSupplier);
  }

  public void setMaxIdleTime(Long maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }
}
