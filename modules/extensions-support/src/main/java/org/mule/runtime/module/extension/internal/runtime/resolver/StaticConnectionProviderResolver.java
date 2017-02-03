/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;

import java.util.Optional;

/**
 * A {@link ValueResolver} specialization for producing {@link ConnectionProvider} instances through a
 * {@link ConnectionProviderObjectBuilder}
 *
 * @since 4.0
 */
public class StaticConnectionProviderResolver<C> implements ConnectionProviderValueResolver<C> {

  private final StaticValueResolver<ConnectionProvider<C>> valueResolver;

  public StaticConnectionProviderResolver(StaticValueResolver<ConnectionProvider<C>> valueResolver) {
    this.valueResolver = valueResolver;
  }

  @Override
  public ConnectionProvider<C> resolve(Event event) throws MuleException {
    return valueResolver.resolve(event);
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public Optional<ResolverSet> getResolverSet() {
    return Optional.empty();
  }
}
