/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEvent.Builder;

/**
 * Provides a way to define callbacks that work on instances of {@link InternalEvent} {@link Builder}s.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface EventBuilderConfigurer {

  /**
   * Applies changes on the given {@code builder}.
   * 
   * @param builder the {@link InternalEvent} builder to configure.
   */
  void configure(Builder builder);

}
