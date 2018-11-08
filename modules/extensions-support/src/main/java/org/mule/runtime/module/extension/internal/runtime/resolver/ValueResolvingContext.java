/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Objects;
import java.util.Optional;

/**
 * Context used to provide all the parameters required for a {@link ValueResolver} to produce
 * a result.
 *
 * @since 4.0
 */
public class ValueResolvingContext {

  private CoreEvent event;
  private final ConfigurationInstance config;
  private final ExpressionManagerSession session;
  private final boolean resolveCursors;

  private ValueResolvingContext(CoreEvent event,
                                ExpressionManagerSession session,
                                ConfigurationInstance config,
                                boolean resolveCursors) {
    this.event = event;
    this.session = session;
    this.config = config;
    this.resolveCursors = resolveCursors;
  }

  /**
   * A builder to create {@link ValueResolvingContext} instances.
   *
   * @param event The event used to create this context
   *
   * @return a builder that can create instance of {@link ValueResolvingContext}
   */
  public static Builder builder(CoreEvent event) {
    return new Builder().withEvent(event);
  }

  /**
   * @return the {@link CoreEvent} of the current resolution context
   */
  public CoreEvent getEvent() {
    return event;
  }

  /**
   * @param event the {@link CoreEvent} of the current resolution context. Not null.
   */
  public void changeEvent(CoreEvent event) {
    requireNonNull(event);
    this.event = event;
  }

  /**
   * @return the {@link ConfigurationInstance} of the current resolution context
   * if one is bound to the element to be resolved, or {@link Optional#empty()} if none is found.
   */
  public Optional<ConfigurationInstance> getConfig() {
    return ofNullable(config);
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

  public boolean resolveCursors() {
    return resolveCursors;
  }

  public Optional<ExpressionManagerSession> getSession() {
    return ofNullable(session);
  }

  public static class Builder {

    private CoreEvent event;
    private Optional<ConfigurationInstance> config = empty();
    private ExpressionManager manager;
    private boolean resolveCursors = true;
    private boolean dynamic = true;

    public Builder withEvent(CoreEvent event) {
      this.event = event;
      return this;
    }

    public Builder withConfig(Optional<ConfigurationInstance> config) {
      this.config = config;
      return this;
    }

    public Builder withConfig(ConfigurationInstance config) {
      this.config = ofNullable(config);
      return this;
    }

    public Builder withExpressionManager(ExpressionManager manager) {
      this.manager = manager;
      return this;
    }

    public Builder resolveCursors(boolean resolveCursors) {
      this.resolveCursors = resolveCursors;
      return this;
    }

    public Builder dynamic(boolean dynamic) {
      this.dynamic = dynamic;
      return this;
    }

    public ValueResolvingContext build() {
      if (event == null) {
        return new ValueResolvingContext(null, null, null, true);
      }
      ExpressionManagerSession session = null;
      if (dynamic && manager != null) {
        session = manager.openSession(event.asBindingContext());
      }
      return new ValueResolvingContext(event, session, config.orElse(null), resolveCursors);
    }
  }
}
