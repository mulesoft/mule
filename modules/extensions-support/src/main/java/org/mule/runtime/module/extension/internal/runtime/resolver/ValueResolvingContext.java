/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.Optional;

/**
 * Context used to provide all the parameters required for a {@link ValueResolver} to produce
 * a result.
 *
 * @since 4.0
 */
public interface ValueResolvingContext {

  static ValueResolvingContext with(Event event) {
    return new DefaultValueResolvingContext(event, null);
  }

  static ValueResolvingContext with(Event event, Optional<ConfigurationInstance> config) {
    return new DefaultValueResolvingContext(event, config.orElse(null));
  }

  /**
   * @return the {@link Event} of the current resolution context
   */
  Event getEvent();

  /**
   * @return the {@link ConfigurationInstance} of the current resolution context
   * if one is bound to the element to be resolved, or {@link Optional#empty()} if none is found.
   */
  Optional<ConfigurationInstance> getConfig();

}
