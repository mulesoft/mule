/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
