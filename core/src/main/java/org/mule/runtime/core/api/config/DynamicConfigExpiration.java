/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static java.util.concurrent.TimeUnit.MINUTES;
import org.mule.runtime.api.time.Time;
import org.mule.runtime.core.internal.config.ImmutableDynamicConfigExpiration;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;

/**
 * Contains information about how much time should a dynamic config be idle before it can be considered elegible for expiration
 *
 * @since 4.0
 */
public interface DynamicConfigExpiration {

  static DynamicConfigExpiration getDefault() {
    return new ImmutableDynamicConfigExpiration(new Time(5, MINUTES), ImmutableExpirationPolicy.getDefault());
  }

  /**
   * Returns the maximum amount of {@link Time} that a dynamic config instance can remain idle before it should be expired. This
   * does not mean that the instance will be expired exactly after that given amount of {@link Time}. The platform remains free to
   * perform the actual expiration at the frequency it sees fit
   *
   * @return a {@link Time}
   */
  Time getFrequency();

  /**
   * @return The {@link ExpirationPolicy} that will be applied
   */
  ExpirationPolicy getExpirationPolicy();
}
