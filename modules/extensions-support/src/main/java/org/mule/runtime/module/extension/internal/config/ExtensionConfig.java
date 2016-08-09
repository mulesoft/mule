/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.core.time.Time;

import java.util.Optional;

/**
 * A config object which gathers all the configuration properties about how the platform should handle extensions
 *
 * @since 4.0
 */
public interface ExtensionConfig {

  /**
   * Returns the maximum amount of {@link Time} that a dynamic config instance can remain idle before it should be expired. This
   * does not mean that the instance will be expired exactly after that given amount of {@link Time}. The platform remains free to
   * perform the actual expiration at the frequency it sees fit
   *
   * @return a {@link Time}
   */
  Optional<Time> getDynamicConfigExpirationFrequency();
}
