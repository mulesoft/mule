/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.connection.ConnectionProvider;

import java.util.Optional;

/**
 * Provides de capability to obtain (if there is any) the {@link ResolverSet} used to resolve the event.
 * 
 * @param <C> the generic type of the connections to be handled
 * @since 4.0
 */
public interface ConnectionProviderValueResolver<C> extends ValueResolver<ConnectionProvider<C>> {

  /**
   * @return the {@link ResolverSet} that will be used to resolve the values from a given event if there is any.
   */
  default Optional<ResolverSet> getResolverSet() {
    return Optional.empty();
  }
}
