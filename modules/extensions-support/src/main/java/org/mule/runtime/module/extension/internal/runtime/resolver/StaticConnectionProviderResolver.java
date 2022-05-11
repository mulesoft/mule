/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Pair;

import java.util.Optional;

/**
 * An implementation of {@link ConnectionProviderValueResolver} that wraps a {@link StaticValueResolver}
 *
 * @since 4.0
 */
public class StaticConnectionProviderResolver<C> implements ConnectionProviderValueResolver<C> {

  private final ConnectionProvider<C> connectionProvider;
  private final ResolverSetResult resolverSetResult;

  public StaticConnectionProviderResolver(ConnectionProvider<C> connectionProvider,
                                          ResolverSetResult resolverSetResult) {
    this.connectionProvider = connectionProvider;
    this.resolverSetResult = resolverSetResult;
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> resolve(ValueResolvingContext context) throws MuleException {
    return new Pair<>(connectionProvider, resolverSetResult);
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
