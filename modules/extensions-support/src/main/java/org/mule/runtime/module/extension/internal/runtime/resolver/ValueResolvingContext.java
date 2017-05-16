/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.Objects;
import java.util.Optional;

/**
 * Context used to provide all the parameters required for a {@link ValueResolver} to produce
 * a result.
 *
 * @since 4.0
 */
public class ValueResolvingContext {

  private final Event event;
  private final ConfigurationInstance config;

  private ValueResolvingContext(Event event, ConfigurationInstance config) {
    this.event = event;
    this.config = config;
  }

  public static ValueResolvingContext from(Event event) {
    return new ValueResolvingContext(event, null);
  }

  public static ValueResolvingContext from(Event event, Optional<ConfigurationInstance> config) {
    return new ValueResolvingContext(event, config.orElse(null));
  }

  /**
   * @return the {@link Event} of the current resolution context
   */
  public Event getEvent() {
    return event;
  }

  /**
   * @return the {@link ConfigurationInstance} of the current resolution context
   * if one is bound to the element to be resolved, or {@link Optional#empty()} if none is found.
   */
  public Optional<ConfigurationInstance> getConfig() {
    return Optional.ofNullable(config);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValueResolvingContext)) {
      return false;
    }

    ValueResolvingContext that = (ValueResolvingContext) o;
    return Objects.equals(event, that.event) && Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(event, config);
  }

}
