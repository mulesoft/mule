/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;

import java.util.Optional;

/**
 * Provides de capability to obtain (if there is any) the {@link ResolverSet} used to resolve the event.
 * 
 * @param <C> the generic type of the connections to be handled
 * @since 4.0
 */
public interface ConnectionProviderValueResolver<C> extends ValueResolver<Pair<ConnectionProvider<C>, ResolverSetResult>> {

  /**
   * @return the {@link ResolverSet} that will be used to resolve the values from a given event if there is any.
   */
  default Optional<ResolverSet> getResolverSet() {
    return empty();
  }

  default Optional<ConnectionProviderObjectBuilder<C>> getObjectBuilder() {
    return empty();
  }
}
